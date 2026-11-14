package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.spatial.CandidateArea
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PickupFailure { PROXIMITY, STORAGE }
sealed interface PickupState {
    data object Waiting : PickupState
    data class Watching(val selected: Int, val pending: Int, val onRegistrationCompleted: Int, val overflow: Int) : PickupState
    data class Failed(val reason: PickupFailure) : PickupState
}

class ProximityPickup(
    private val scope: CoroutineScope,
    private val positions: StateFlow<TrackedPosition?>,
    private val candidateSignals: StateFlow<CandidateState>,
    private val currentCandidates: () -> List<NearbyCandidate>?,
    private val receipts: NoteReceiptProcessor,
    private val ownsSource: (ProximitySource) -> Boolean,
    private val elapsedMillis: () -> Long,
    private val utcMillis: () -> Long,
    private val onError: (String, Exception) -> Unit = { _, _ -> },
    private val scenarioId: () -> String? = { null },
    private val onReceived: (ReceiptRequest) -> Unit = {},
) {

    private class PickupSession(val session: Long, val source: ProximitySource, val setup: SyntheticSession) {
        var failed = false
        var overflow = 0
        val requests = mutableListOf<ProximityRegistration>()
        val accepted = mutableSetOf<String>()
    }

    private class ProximityRegistration(val owner: PickupSession, val noteId: String) {
        var valid = true
        var alertId: String? = null
    }
    private var activeSession: PickupSession? = null
    private val mutableState = MutableStateFlow<PickupState>(PickupState.Waiting)
    val state = mutableState.asStateFlow()

    private var inputObservation: Job? = null

    fun start(session: Long, source: ProximitySource, setup: SyntheticSession) {
        if (activeSession?.let { it.session == session && it.source === source } == true) return
        stop()
        val owner = PickupSession(session, source, setup)
        activeSession = owner
        refreshNearbyNotes()
        if (isCurrentSession(owner)) observePickupInputs()
    }

    fun stop() {
        inputObservation?.cancel()
        inputObservation = null
        val previous = activeSession
        activeSession = null
        previous?.requests?.toList()?.forEach { unregisterRequest(it) }
        mutableState.value = PickupState.Waiting
    }

    private fun observePickupInputs() {
        inputObservation = scope.launch {
            combine(positions, candidateSignals, receipts.inbox) { _, _, _ -> Unit }
                .collect { refreshNearbyNotes() }
        }
    }

    private fun isCurrentSession(owner: PickupSession): Boolean = activeSession === owner && !owner.failed && ownsSource(owner.source)

    private fun refreshNearbyNotes() {
        val owner = activeSession ?: return
        if (!isCurrentSession(owner)) return
        val position = positions.value
        val candidates = currentCandidates()
        if (position?.session != owner.session || candidates == null ||
            !CandidateArea.contains(position.fix.latitude, position.fix.longitude)) {
            owner.requests.toList().forEach { unregisterRequest(it) }
            if (!owner.failed) mutableState.value = PickupState.Waiting
            return
        }
        val eligible = unreceivedNearbyNotes(owner, candidates)
        val selected = eligible.take(MAX_REGISTRATIONS)
        owner.overflow = (eligible.size - selected.size).coerceAtLeast(0)
        synchronizeRegistrations(owner, selected)
        receiveNotesAlreadyInside(owner)
        publish(owner)
    }

    private fun unreceivedNearbyNotes(owner: PickupSession, candidates: List<NearbyCandidate>): List<NearbyCandidate> {
        val received = receipts.inbox.value.items.filter { it.recipientId == owner.setup.actorId }.map { it.note.noteId }.toSet()
        val now = utcMillis()
        val scenario = scenarioId()
        return candidates.filter { candidate ->
            val note = candidate.note
            note.scenarioId == scenario && note.isActiveAt(now) && note.authorId != owner.setup.actorId &&
                CandidateArea.contains(note.latitude, note.longitude) &&
                note.noteId !in received && note.noteId !in owner.accepted
        }.sortedWith(compareBy<NearbyCandidate> { it.distanceMeters }.thenBy { it.note.noteId })
    }

    private fun synchronizeRegistrations(owner: PickupSession, selected: List<NearbyCandidate>) {
        val ids = selected.map { it.note.noteId }.toSet()
        owner.requests.toList().filter { it.valid && it.noteId !in ids }.forEach { unregisterRequest(it) }
        for (candidate in selected) {
            if (!isCurrentSession(owner)) break
            if (owner.requests.any { it.valid && it.noteId == candidate.note.noteId }) continue
            if (owner.requests.size >= MAX_REGISTRATIONS) break
            val request = ProximityRegistration(owner, candidate.note.noteId)
            owner.requests += request
            try {
                owner.source.addProximity(candidate.note.latitude, candidate.note.longitude,
                    radiusMeters = PICKUP_RADIUS_METERS.toFloat(),
                    onRegistered = { id -> scope.launch { onRegistrationCompleted(request, id) } },
                    onEnter = { id -> scope.launch { onEnteredRadius(request, id) } },
                    onError = { error -> scope.launch { onRegistrationFailed(request, error) } })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                fail(owner, PickupFailure.PROXIMITY, error)
            }
        }
    }

    private fun receiveNotesAlreadyInside(owner: PickupSession) {
        owner.requests.toList().forEach { tryReceiveNote(it) }
    }

    private fun onEnteredRadius(request: ProximityRegistration, alertId: String) {
        if (request.alertId == alertId) tryReceiveNote(request)
    }

    private fun onRegistrationCompleted(request: ProximityRegistration, id: String) {
        val owner = request.owner
        if (request.alertId != null) return
        request.alertId = id
        if (!request.valid || !isCurrentSession(owner)) unregisterRequest(request)
        else {
            tryReceiveNote(request)
            publish(owner)
        }
        if (isCurrentSession(owner)) refreshNearbyNotes()
    }

    private fun onRegistrationFailed(request: ProximityRegistration, error: Exception) {
        if (error is CancellationException) throw error
        val owner = request.owner
        if (!request.valid || !isCurrentSession(owner)) {
            if (request.alertId == null) owner.requests.remove(request)
            if (isCurrentSession(owner)) refreshNearbyNotes()
            return
        }
        if (request.alertId == null) owner.requests.remove(request)
        fail(owner, PickupFailure.PROXIMITY, error)
    }

    private fun tryReceiveNote(request: ProximityRegistration) {
        val owner = request.owner
        if (!request.valid || request.alertId == null || !isCurrentSession(owner)) return
        val position = positions.value ?: return
        if (position.session != owner.session || elapsedMillis() - position.fix.receivedElapsedMillis !in 0L..RECEIPT_FRESHNESS) return
        val note = currentCandidates()?.firstOrNull { it.note.noteId == request.noteId }?.note ?: return
        if (receipts.inbox.value.items.any { it.recipientId == owner.setup.actorId && it.note.noteId == note.noteId }) return
        if (note.scenarioId != scenarioId() || note.noteId in owner.accepted || eligibleDistance(note, owner.setup, position.fix, utcMillis()) == null) return
        owner.accepted += note.noteId
        unregisterRequest(request)
        if (!isCurrentSession(owner)) return

        val input = ReceiptRequest(note.noteId, owner.setup, position.fix, note.scenarioId)
        receipts.receive(input) { result ->
            if (isCurrentSession(owner)) {
                if (result == ReceiveResult.RECEIVED) {
                    try { onReceived(input) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { onError("RECEIPT_ANNOUNCEMENT", error) }
                }
                if (result == ReceiveResult.STORAGE_FAILURE) fail(owner, PickupFailure.STORAGE, IllegalStateException("Receipt storage failed"))
                else refreshNearbyNotes()
            }
        }
    }

    private fun unregisterRequest(request: ProximityRegistration) {
        request.valid = false
        val id = request.alertId ?: return
        val owner = request.owner
        if (!ownsSource(owner.source)) {
            owner.requests.remove(request)
            return
        }
        try {
            owner.source.removeProximity(id)
            owner.requests.remove(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (activeSession === owner && !owner.failed) fail(owner, PickupFailure.PROXIMITY, error)
            else onError("PROXIMITY_REMOVE", error)
        }
    }

    private fun fail(owner: PickupSession, reason: PickupFailure, error: Exception) {
        if (activeSession !== owner || owner.failed) return
        owner.failed = true
        inputObservation?.cancel()
        inputObservation = null
        mutableState.value = PickupState.Failed(reason)
        onError(reason.name, error)
        owner.requests.toList().forEach { unregisterRequest(it) }
    }

    private fun publish(owner: PickupSession) {
        if (!isCurrentSession(owner)) return
        val active = owner.requests.filter { it.valid }
        mutableState.value = PickupState.Watching(active.size, owner.requests.count { it.alertId == null },
            active.count { it.alertId != null }, owner.overflow)
    }

    companion object {
        const val MAX_REGISTRATIONS = 16
        private val RECEIPT_FRESHNESS = TimeUnit.SECONDS.toMillis(2)
    }
}
