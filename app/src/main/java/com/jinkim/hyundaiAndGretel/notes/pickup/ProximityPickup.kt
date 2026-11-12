package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PickupFailure { PROXIMITY, STORAGE }
sealed interface PickupState {
    data object Waiting : PickupState
    data class Watching(val selected: Int, val pending: Int, val onRegistrationCompleted: Int, val overflow: Int) : PickupState
    data class Failed(val reason: PickupFailure) : PickupState
}

/** 등록·수신 수락은 메인 실행 문맥에서 처리한다. SDK 콜백도 메인으로 옮긴 뒤 현재 세션인지 다시 확인한다. */
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
) {
    // 리뷰용: 한 수신 세션이 소유하는 등록 요청과 수락한 쪽지 목록이다. 다음 세션과 공유하지 않는다.
    private class PickupSession(val session: Long, val source: ProximitySource, val setup: SyntheticSession) {
        var failed = false
        var overflow = 0
        val requests = mutableListOf<ProximityRegistration>()
        val accepted = mutableSetOf<String>()
    }
    // 리뷰용: SDK 등록 요청 하나의 소유자·유효 여부·나중에 도착할 등록 ID를 함께 관리한다.
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

    /** 수신 감시는 start부터 stop까지만 위치·후보·수신 이력 변경을 관찰한다. */
    private fun observePickupInputs() {
        inputObservation = scope.launch {
            combine(positions, candidateSignals, receipts.inbox) { _, _, _ -> Unit }
                .collect { refreshNearbyNotes() }
        }
    }

    private fun isCurrentSession(owner: PickupSession): Boolean = activeSession === owner && !owner.failed && ownsSource(owner.source)

    // 리뷰용: 현재 세션에서 남이 작성한 미수신·전방 쪽지만 고른다. 가까운 최대 16개만 SDK에 등록한다.
    private fun refreshNearbyNotes() {
        val owner = activeSession ?: return
        if (!isCurrentSession(owner)) return
        val position = positions.value
        val candidates = currentCandidates()
        val route = position?.let { SyntheticRoute.project(it.fix.latitude, it.fix.longitude, owner.setup) }
        if (position?.session != owner.session || candidates == null || route == null) {
            owner.requests.toList().forEach { unregisterRequest(it) }
            if (!owner.failed) mutableState.value = PickupState.Waiting
            return
        }
        val eligible = unreceivedNotesAhead(owner, candidates, route)
        val selected = eligible.take(MAX_REGISTRATIONS)
        owner.overflow = (eligible.size - selected.size).coerceAtLeast(0)
        synchronizeRegistrations(owner, selected)
        receiveNotesAlreadyInside(owner)
        publish(owner)
    }

    private fun unreceivedNotesAhead(owner: PickupSession, candidates: List<NearbyCandidate>, route: RouteContext): List<NearbyCandidate> {
        val received = receipts.inbox.value.items.filter { it.recipientId == owner.setup.actorId }.map { it.note.noteId }.toSet()
        return candidates.filter { candidate ->
            val note = candidate.note
            val noteRoute = note.route
            noteRoute != null && SyntheticRoute.matches(noteRoute, note.latitude, note.longitude) &&
                note.authorId != owner.setup.actorId && note.noteId !in received && note.noteId !in owner.accepted &&
                (SyntheticRoute.ahead(noteRoute, route)?.let { it >= 0 } == true)
        }.sortedWith(compareBy<NearbyCandidate> { it.distanceMeters }.thenBy { it.note.noteId })
    }

    private fun synchronizeRegistrations(owner: PickupSession, selected: List<NearbyCandidate>) {
        val ids = selected.map { it.note.noteId }.toSet()
        owner.requests.toList().filter { it.valid && it.noteId !in ids }.forEach { unregisterRequest(it) }
        for (candidate in selected) {
            if (!isCurrentSession(owner)) break
            if (owner.requests.any { it.valid && it.noteId == candidate.note.noteId }) continue
            if (owner.requests.size >= MAX_REGISTRATIONS) break // 무효화됐어도 등록 ID를 기다리는 요청은 등록 한 자리를 계속 차지한다.
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

    // ENTER가 누락되거나 등록 시 이미 반경 안인 경우도 같은 수신 판정으로 보완한다.
    private fun receiveNotesAlreadyInside(owner: PickupSession) {
        owner.requests.toList().forEach { tryReceiveNote(it) }
    }

    private fun onEnteredRadius(request: ProximityRegistration, alertId: String) {
        if (request.alertId == alertId) tryReceiveNote(request)
    }

    // 리뷰용: 등록 완료가 늦게 와도 현재 유효한 요청인지 재확인한다. 이미 중지한 요청의 ID는 즉시 정리한다.
    private fun onRegistrationCompleted(request: ProximityRegistration, id: String) {
        val owner = request.owner
        if (request.alertId != null) return // 중복 등록 콜백이 무효화된 요청을 되살리지 못하게 한다.
        request.alertId = id
        if (!request.valid || !isCurrentSession(owner)) unregisterRequest(request)
        else {
            tryReceiveNote(request) // 등록할 때 이미 반경 안이면 ENTER 이벤트 없이도 수신 조건을 확인한다.
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
        if (request.alertId == null) owner.requests.remove(request) // SDK가 등록 전 실패를 정리하므로 아직 ID가 없는 요청을 목록에서 제거한다.
        fail(owner, PickupFailure.PROXIMITY, error)
    }

    // 리뷰용: ENTER 이벤트와 이미 반경 안인 경우가 함께 사용하는 판정이다. 위치 2초 최신성·500미터·경로·방향·만료·중복을 다시 확인한다.
    private fun tryReceiveNote(request: ProximityRegistration) {
        val owner = request.owner
        if (!request.valid || request.alertId == null || !isCurrentSession(owner)) return
        val position = positions.value ?: return
        if (position.session != owner.session || elapsedMillis() - position.fix.receivedElapsedMillis !in 0L..RECEIPT_FRESHNESS) return
        val note = currentCandidates()?.firstOrNull { it.note.noteId == request.noteId }?.note ?: return
        if (receipts.inbox.value.items.any { it.recipientId == owner.setup.actorId && it.note.noteId == note.noteId }) return
        if (note.noteId in owner.accepted || eligibleDistance(note, owner.setup, position.fix, utcMillis()) == null) return
        owner.accepted += note.noteId
        unregisterRequest(request)
        if (!isCurrentSession(owner)) return
        // 리뷰용: 수신 자격을 통과한 입력을 고정해 수신 기록 저장을 요청한다. 성공·중복·실패 처리는 저장 결과로 결정한다.
        receipts.receive(ReceiptRequest(note.noteId, owner.setup, position.fix, scenarioId())) { result ->
            if (isCurrentSession(owner)) {
                if (result == ReceiveResult.STORAGE_FAILURE) fail(owner, PickupFailure.STORAGE, IllegalStateException("Receipt storage failed"))
                else refreshNearbyNotes()
            }
        }
    }

    // 리뷰용: 요청을 먼저 무효화한다. ID가 아직 없으면 기록을 남겨, 뒤늦게 ID가 도착했을 때 해제할 수 있게 한다.
    private fun unregisterRequest(request: ProximityRegistration) {
        request.valid = false
        val id = request.alertId ?: return
        val owner = request.owner
        if (!ownsSource(owner.source)) {
            owner.requests.remove(request) // 더 이상 현재 소스가 아니면 SDK 해제 경로가 자원 정리를 담당한다.
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

    // 리뷰용: 근접 감시 또는 수신 저장 실패는 이 수신 세션을 실패로 고정한다. 위치 수집과 기존에 수락한 쓰기를 중지하지는 않는다.
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
