package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PickupFailure { PROXIMITY, STORAGE }
sealed interface PickupState {
    data object Waiting : PickupState
    data class Watching(val selected: Int, val pending: Int, val registered: Int, val overflow: Int) : PickupState
    data class Failed(val reason: PickupFailure) : PickupState
}

/** Main-confined registration/acceptance. SDK callbacks are rechecked after dispatch to Main. */
class ProximityPickup(
    private val scope: CoroutineScope,
    private val positions: StateFlow<TrackedPosition?>,
    candidateSignals: StateFlow<CandidateState>,
    private val currentCandidates: () -> List<NearbyCandidate>?,
    private val receipts: NoteReceiptProcessor,
    private val ownsSource: (ProximitySource) -> Boolean,
    private val elapsedMillis: () -> Long,
    private val utcMillis: () -> Long,
    private val onError: (String, Exception) -> Unit = { _, _ -> },
    private val scenarioId: () -> String? = { null },
) {
    private class Run(val session: Long, val source: ProximitySource, val setup: SyntheticSession) {
        var failed = false
        var overflow = 0
        val requests = mutableListOf<Request>()
        val accepted = mutableSetOf<String>()
    }
    private class Request(val owner: Run, val noteId: String) {
        var valid = true
        var alertId: String? = null
    }
    private var run: Run? = null
    private val mutableState = MutableStateFlow<PickupState>(PickupState.Waiting)
    val state = mutableState.asStateFlow()

    init {
        scope.launch {
            combine(positions, candidateSignals, receipts.inbox) { _, _, _ -> Unit }.collect { refresh() }
        }
    }

    fun start(session: Long, source: ProximitySource, setup: SyntheticSession) {
        if (run?.let { it.session == session && it.source === source } == true) return
        stop()
        run = Run(session, source, setup)
        refresh()
    }

    fun stop() {
        val previous = run
        run = null
        previous?.requests?.toList()?.forEach { invalidate(it) }
        mutableState.value = PickupState.Waiting
    }

    private fun current(owner: Run): Boolean = run === owner && !owner.failed && ownsSource(owner.source)

    private fun refresh() {
        val owner = run ?: return
        if (!current(owner)) return
        val position = positions.value
        val candidates = currentCandidates()
        val route = position?.let { SyntheticRoute.project(it.fix.latitude, it.fix.longitude, owner.setup) }
        if (position?.session != owner.session || candidates == null || route == null) {
            owner.requests.toList().forEach { invalidate(it) }
            if (!owner.failed) mutableState.value = PickupState.Waiting
            return
        }
        val received = receipts.inbox.value.items.filter { it.recipientId == owner.setup.actorId }.map { it.note.noteId }.toSet()
        val eligible = candidates.filter { candidate ->
            val note = candidate.note
            val noteRoute = note.route
            noteRoute != null && SyntheticRoute.matches(noteRoute, note.latitude, note.longitude) &&
                note.authorId != owner.setup.actorId && note.noteId !in received && note.noteId !in owner.accepted &&
                (SyntheticRoute.ahead(noteRoute, route)?.let { it >= 0 } == true)
        }.sortedWith(compareBy<NearbyCandidate> { it.distanceMeters }.thenBy { it.note.noteId })
        val selected = eligible.take(MAX_REGISTRATIONS)
        owner.overflow = (eligible.size - selected.size).coerceAtLeast(0)
        val ids = selected.map { it.note.noteId }.toSet()
        owner.requests.toList().filter { it.valid && it.noteId !in ids }.forEach { invalidate(it) }
        for (candidate in selected) {
            if (!current(owner)) break
            if (owner.requests.any { it.valid && it.noteId == candidate.note.noteId }) continue
            if (owner.requests.size >= MAX_REGISTRATIONS) break // Tombstones still occupy slots.
            val request = Request(owner, candidate.note.noteId)
            owner.requests += request
            try {
                owner.source.addProximity(candidate.note.latitude, candidate.note.longitude,
                    onRegistered = { id -> scope.launch { registered(request, id) } },
                    onEnter = { id -> scope.launch { if (request.alertId == id) evaluate(request) } },
                    onError = { error -> scope.launch { registrationError(request, error) } })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                fail(owner, PickupFailure.PROXIMITY, error)
            }
        }
        owner.requests.toList().forEach { evaluate(it) }
        publish(owner)
    }

    private fun registered(request: Request, id: String) {
        val owner = request.owner
        if (request.alertId != null) return // Duplicate onRegistered must not revive a request.
        request.alertId = id
        if (!request.valid || !current(owner)) invalidate(request)
        else {
            evaluate(request) // Already inside does not depend on the SDK emitting ENTER.
            publish(owner)
        }
        if (current(owner)) refresh()
    }

    private fun registrationError(request: Request, error: Exception) {
        if (error is CancellationException) throw error
        val owner = request.owner
        if (!request.valid || !current(owner)) {
            if (request.alertId == null) owner.requests.remove(request)
            if (current(owner)) refresh()
            return
        }
        if (request.alertId == null) owner.requests.remove(request) // SDK removes pre-registration failures locally.
        fail(owner, PickupFailure.PROXIMITY, error)
    }

    private fun evaluate(request: Request) {
        val owner = request.owner
        if (!request.valid || request.alertId == null || !current(owner)) return
        val position = positions.value ?: return
        if (position.session != owner.session || elapsedMillis() - position.fix.receivedElapsedMillis !in 0L..RECEIPT_FRESHNESS) return
        val note = currentCandidates()?.firstOrNull { it.note.noteId == request.noteId }?.note ?: return
        if (receipts.inbox.value.items.any { it.recipientId == owner.setup.actorId && it.note.noteId == note.noteId }) return
        if (note.noteId in owner.accepted || eligibleDistance(note, owner.setup, position.fix, utcMillis()) == null) return
        owner.accepted += note.noteId
        invalidate(request)
        if (!current(owner)) return
        receipts.receive(ReceiptRequest(note.noteId, owner.setup, position.fix, scenarioId())) { result ->
            if (current(owner)) {
                if (result == ReceiveResult.STORAGE_FAILURE) fail(owner, PickupFailure.STORAGE, IllegalStateException("Receipt storage failed"))
                else refresh()
            }
        }
    }

    private fun invalidate(request: Request) {
        request.valid = false
        val id = request.alertId ?: return
        val owner = request.owner
        if (!ownsSource(owner.source)) {
            owner.requests.remove(request) // release owns cleanup after this source stops being current.
            return
        }
        try {
            owner.source.removeProximity(id)
            owner.requests.remove(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (run === owner && !owner.failed) fail(owner, PickupFailure.PROXIMITY, error)
            else onError("PROXIMITY_REMOVE", error)
        }
    }

    private fun fail(owner: Run, reason: PickupFailure, error: Exception) {
        if (run !== owner || owner.failed) return
        owner.failed = true
        mutableState.value = PickupState.Failed(reason)
        onError(reason.name, error)
        owner.requests.toList().forEach { invalidate(it) }
    }

    private fun publish(owner: Run) {
        if (!current(owner)) return
        val active = owner.requests.filter { it.valid }
        mutableState.value = PickupState.Watching(active.size, owner.requests.count { it.alertId == null },
            active.count { it.alertId != null }, owner.overflow)
    }

    companion object {
        const val MAX_REGISTRATIONS = 16
        private val RECEIPT_FRESHNESS = TimeUnit.SECONDS.toMillis(2)
    }
}
