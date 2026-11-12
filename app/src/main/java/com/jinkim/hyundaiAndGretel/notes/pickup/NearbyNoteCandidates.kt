package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.TrackedPosition
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.spatial.CandidateArea
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialUnavailableException
import com.jinkim.hyundaiAndGretel.notes.spatial.distanceMeters
import com.jinkim.hyundaiAndGretel.notes.validationError
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NearbyCandidate(val note: RoadNote, val distanceMeters: Double)
data class CandidateSnapshot(
    val position: TrackedPosition,
    val cells: Set<String>,
    val candidates: List<NearbyCandidate>,
    val evaluatedAtUtcMillis: Long,
)
enum class CandidateFailure { H3_UNAVAILABLE, INDEX_ERROR, STORAGE_ERROR, OUTSIDE_AREA }
sealed interface CandidateState {
    data object WaitingForLocation : CandidateState
    data object Loading : CandidateState
    /** 평가 당시 결과일 뿐이다. 실제 사용 시에는 currentCandidates()로 유효성을 다시 확인해야 한다. */
    data class Ready(val snapshot: CandidateSnapshot) : CandidateState
    data class Failed(val reason: CandidateFailure) : CandidateState
}

/** 명령, 결과 관찰과 후보 사용은 서비스의 메인 코루틴 실행 문맥에서만 처리한다. */
class NearbyNoteCandidates(
    private val scope: CoroutineScope,
    private val positions: StateFlow<TrackedPosition?>,
    private val spatial: SpatialIndex,
    private val store: NearbyNoteStore,
    private val elapsedMillis: () -> Long,
    private val utcMillis: () -> Long,
    private val onFailure: (String, Exception) -> Unit = { _, _ -> },
) {
    private data class Key(val session: Long, val cells: Set<String>)
    private val mutableState = MutableStateFlow<CandidateState>(CandidateState.WaitingForLocation)
    val state = mutableState.asStateFlow()
    private var key: Key? = null
    private var appliedPosition: TrackedPosition? = null
    private var rows: List<RoadNote>? = null
    private var observation: Job? = null
    private var staleDeadline: Job? = null

    init {
        scope.launch { positions.collect { update(it) } }
    }

    // 리뷰용: 위치가 바뀌면 최신성·지원 구역을 검사하고 주변 H3 셀을 계산한다. 셀이나 세션이 바뀔 때 DB 관찰을 교체한다.
    private fun update(position: TrackedPosition?) {
        staleDeadline?.cancel()
        if (position == null || !isFresh(position)) {
            clear()
            return
        }
        if (!CandidateArea.contains(position.fix.latitude, position.fix.longitude)) {
            clear()
            mutableState.value = CandidateState.Failed(CandidateFailure.OUTSIDE_AREA)
            return
        }
        val cells = try {
            spatial.cover(position.fix.latitude, position.fix.longitude).toSet().also {
                require(it.isNotEmpty() && it.size <= CandidateArea.MAX_QUERY_CELLS)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: SpatialUnavailableException) {
            fail(CandidateFailure.H3_UNAVAILABLE, error)
            return
        } catch (error: Exception) {
            fail(CandidateFailure.INDEX_ERROR, error)
            return
        }
        appliedPosition = position
        val nextKey = Key(position.session, cells)
        if (key != nextKey || observation?.isActive != true) {
            observation?.cancel()
            rows = null
            key = nextKey
            mutableState.value = CandidateState.Loading
            observation = scope.launch {
                // 리뷰용: 이 관찰 작업 자신의 취소 여부를 검사한다. 같은 셀로 돌아와도 이전 작업의 늦은 결과가 새 후보를 덮지 못하게 한다.
                val observationContext = coroutineContext
                try {
                    store.observeNearby(cells).collect { result ->
                        observationContext.ensureActive()
                        val current = positions.value
                        if (key == nextKey && current?.session == nextKey.session && isFresh(current)) {
                            rows = result.toList()
                            if (current == appliedPosition) publish(current, cells)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: SpatialUnavailableException) {
                    observationContext.ensureActive()
                    if (key == nextKey) fail(CandidateFailure.H3_UNAVAILABLE, error)
                } catch (error: IllegalArgumentException) {
                    observationContext.ensureActive()
                    if (key == nextKey) fail(CandidateFailure.INDEX_ERROR, error)
                } catch (error: Exception) {
                    observationContext.ensureActive()
                    if (key == nextKey) fail(CandidateFailure.STORAGE_ERROR, error)
                }
            }
        } else {
            publish(position, cells)
        }
        // 리뷰용: 새 위치가 더 오지 않아도 5초가 지나면 기존 후보를 무효화하는 타이머다.
        val remaining = FRESHNESS_MILLIS - (elapsedMillis() - position.fix.receivedElapsedMillis) + 1
        staleDeadline = scope.launch {
            delay(remaining.coerceAtLeast(1))
            if (positions.value == position && !isFresh(position)) clear()
        }
    }

    private fun publish(position: TrackedPosition, cells: Set<String>) {
        val originals = rows ?: return
        val now = utcMillis()
        mutableState.value = CandidateState.Ready(CandidateSnapshot(position, cells, evaluate(originals, position, now), now))
    }

    /** 실행을 양보하지 않고 현재 세션, 수신 단조시각, UTC와 위치를 기준으로 유효성을 확인한다. */
    // 리뷰용: 수신 처리자가 후보를 쓰기 직전에 호출한다. 화면에 남은 Ready 상태를 그대로 신뢰하지 않는다.
    fun currentCandidates(): List<NearbyCandidate>? {
        val ready = mutableState.value as? CandidateState.Ready ?: return null
        val position = positions.value ?: return null
        if (ready.snapshot.position != position || key?.session != position.session || ready.snapshot.cells != key?.cells || !isFresh(position)) return null
        if (!CandidateArea.contains(position.fix.latitude, position.fix.longitude)) return null
        return evaluate(rows ?: return null, position, utcMillis())
    }

    // 리뷰용: 저장 원본 검증 → 중복 제거 → 만료 제외 → 실제 거리 계산 → 6km 이내 정렬 순으로 후보를 좁힌다.
    private fun evaluate(originals: List<RoadNote>, position: TrackedPosition, nowUtc: Long): List<NearbyCandidate> =
        originals.onEach { require(it.validationError() == null) }
            .distinctBy { it.noteId }
            .filter { it.expiresAtUtcMillis > nowUtc }
            .map { note -> NearbyCandidate(note, distanceMeters(position.fix.latitude, position.fix.longitude, note.latitude, note.longitude)) }
            .filter { it.distanceMeters <= CandidateArea.RADIUS_METERS }
            .sortedWith(compareBy<NearbyCandidate> { it.distanceMeters }.thenBy { it.note.noteId })

    private fun isFresh(position: TrackedPosition): Boolean =
        elapsedMillis() - position.fix.receivedElapsedMillis in 0L..FRESHNESS_MILLIS

    private fun clear() {
        observation?.cancel()
        staleDeadline?.cancel()
        observation = null
        key = null
        appliedPosition = null
        rows = null
        mutableState.value = CandidateState.WaitingForLocation
    }

    private fun fail(reason: CandidateFailure, error: Exception) {
        clear()
        mutableState.value = CandidateState.Failed(reason)
        onFailure(reason.name, error)
    }

    private companion object { val FRESHNESS_MILLIS = TimeUnit.SECONDS.toMillis(5) }
}
