package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.location.TrackedPosition
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.spatial.CandidateArea
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialUnavailableException
import com.jinkim.hyundaiAndGretel.notes.spatial.distanceMeters
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyNoteCandidatesTest {
    private class Index : SpatialIndex {
        var error: Exception? = null
        var overrideCells: Set<String>? = null
        override fun cell(latitude: Double, longitude: Double) = if (longitude < 127.04) "west" else "east"
        override fun cover(latitude: Double, longitude: Double): Set<String> {
            error?.let { throw it }
            return overrideCells ?: setOf(cell(latitude, longitude))
        }
    }
    private class Store : NearbyNoteStore {
        data class Request(val cells: Set<String>, val rows: MutableStateFlow<List<RoadNote>?> = MutableStateFlow(null))
        val requests = mutableListOf<Request>()
        var active = 0
        override fun observeNearby(cells: Set<String>): Flow<List<RoadNote>> {
            val request = Request(cells).also { requests += it }
            return request.rows.filterNotNull().onStart { active++ }.onCompletion { active-- }
        }
    }
    private fun position(time: Long = 0, session: Long = 1, longitude: Double = 127.03) =
        TrackedPosition(session, LocationFix(37.5, longitude, time))
    private fun note(id: Int = 1, latitude: Double = 37.5, longitude: Double = 127.03, expires: Long = 1_600_000) =
        RoadNote("00000000-0000-0000-0000-${id.toString().padStart(12,'0')}", "A", "구덩이", NoteCategory.WARNING,
            latitude, longitude, 0, expires - RoadNote.TTL_MILLIS, expires)

    @Test fun cancelledOldFailuresCannotClearSameKeyReplacementButCurrentFailuresStillApply() = runTest {
        val failures = listOf(
            SpatialUnavailableException(UnsatisfiedLinkError("old")) to CandidateFailure.H3_UNAVAILABLE,
            IllegalArgumentException("old") to CandidateFailure.INDEX_ERROR,
            IOException("old") to CandidateFailure.STORAGE_ERROR,
        )
        for ((error, reason) in failures) {
            checkLateObservation(error, reason)
        }
    }

    @Test fun cancelledOldResultCannotReplaceSameKeyCandidatesOrStopSubsequentUpdates() = runTest {
        checkLateObservation(null, CandidateFailure.STORAGE_ERROR)
    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun TestScope.checkLateObservation(oldError: Exception?, currentFailure: CandidateFailure) {
        val releaseOld = CompletableDeferred<Unit>()
        val updates = Channel<Result<List<RoadNote>>>(Channel.UNLIMITED)
        var requests = 0
        var replacementActive = false
        val diagnostics = mutableListOf<String>()
        val store = object : NearbyNoteStore {
            override fun observeNearby(cells: Set<String>): Flow<List<RoadNote>> {
                if (++requests != 1) return updates.receiveAsFlow().map { it.getOrThrow() }
                    .onStart { replacementActive = true }.onCompletion { replacementActive = false }
                // 취소에 바로 반응하지 않는 정리 과정을 만들어, 취소된 관찰 작업 자체의 활성 여부 검사를 검증한다.
                return object : Flow<List<RoadNote>> {
                    override suspend fun collect(collector: FlowCollector<List<RoadNote>>) {
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                releaseOld.await()
                                oldError?.let { throw it }
                                collector.emit(listOf(note(1)))
                            }
                        }
                    }
                }
            }
        }
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { 0 }, { 1_000_000 },
            onFailure = { reason, _ -> diagnostics += reason })
        try {
            runCurrent()
            positions.value = null
            runCurrent()
            positions.value = position()
            runCurrent()
            updates.send(Result.success(listOf(note(2))))
            runCurrent()
            assertEquals(2, requests)
            assertEquals(note(2), nearby.currentCandidates()!!.single().note)
            releaseOld.complete(Unit)
            runCurrent()
            assertTrue(nearby.state.value is CandidateState.Ready)
            assertEquals(note(2), nearby.currentCandidates()!!.single().note)
            assertTrue(replacementActive)
            assertTrue(diagnostics.isEmpty())
            updates.send(Result.success(listOf(note(3))))
            runCurrent()
            assertEquals(note(3), nearby.currentCandidates()!!.single().note)
            updates.send(Result.failure(oldError ?: IOException("current")))
            runCurrent()
            assertEquals(CandidateState.Failed(currentFailure), nearby.state.value)
            assertNull(nearby.currentCandidates())
            assertFalse(replacementActive)
            assertEquals(listOf(currentFailure.name), diagnostics)
        } finally {
            releaseOld.complete(Unit)
            positions.value = null
            runCurrent()
            updates.close()
        }
    }

    @Test fun flowChangeAndSameCoverMovementReevaluateDistanceWithoutPollingOrDuplicateSubscriptions() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val store = Store()
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { testScheduler.currentTime }, { 1_000_000 })
        runCurrent()
        assertEquals(CandidateState.Loading, nearby.state.value)
        store.requests.single().rows.value = listOf(note(2, longitude = 127.033), note(1), note(1), note(3, longitude = 127.039))
        runCurrent()
        assertEquals(listOf(1,2), nearby.currentCandidates()!!.map { it.note.noteId.takeLast(1).toInt() })
        assertEquals(1, store.active)
        // 위치가 바뀌지 않아도 DB 변경을 관찰하여 새 주변 쪽지를 후보에 반영해야 한다.
        store.requests.single().rows.value = listOf(note(4))
        runCurrent()
        assertEquals(note(4), nearby.currentCandidates()!!.single().note)
        advanceTimeBy(1_000)
        positions.value = position(1_000, longitude = 127.039)
        assertNull(nearby.currentCandidates()) // 새 위치를 처리하기 전에 이전 위치의 평가 결과를 사용할 수 없어야 한다.
        runCurrent()
        assertEquals(1, store.requests.size)
        assertTrue(nearby.currentCandidates()!!.isEmpty())
        assertEquals(1, store.active)
    }

    @Test fun rawSnapshotMayAgeButConsumerChecksUtcFreshnessAndCurrentSessionWithoutSuspending() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val store = Store()
        var utc = 1_000_000L
        var elapsed = 0L
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { elapsed }, { utc })
        runCurrent()
        store.requests.single().rows.value = listOf(note(expires = 1_000_002))
        runCurrent()
        assertEquals(1, nearby.currentCandidates()!!.size)
        utc = 1_000_002
        assertTrue(nearby.currentCandidates()!!.isEmpty())
        assertEquals(1, (nearby.state.value as CandidateState.Ready).snapshot.candidates.size)
        elapsed = 5_001
        assertNull(nearby.currentCandidates())
        elapsed = 0
        positions.value = position(session = 2)
        assertNull(nearby.currentCandidates())
        positions.value = null
        assertNull(nearby.currentCandidates())
    }

    @Test fun delayedRowsRecheckUtcAndOldCoverOrSessionEmissionsCannotRestoreCandidates() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val store = Store()
        var utc = 1_000_000L
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { testScheduler.currentTime }, { utc })
        runCurrent()
        val old = store.requests.single()
        utc = 1_000_003
        old.rows.value = listOf(note(expires = 1_000_002))
        runCurrent()
        assertTrue(nearby.currentCandidates()!!.isEmpty())
        positions.value = position(session = 2, longitude = 127.045)
        assertNull(nearby.currentCandidates())
        runCurrent()
        old.rows.value = listOf(note())
        runCurrent()
        assertNull(nearby.currentCandidates())
        store.requests.last().rows.value = listOf(note(longitude = 127.045))
        runCurrent()
        assertEquals(1, nearby.currentCandidates()!!.size)
        assertEquals(1, store.active)
        positions.value = null
        runCurrent()
        assertEquals(0, store.active)
        assertEquals(CandidateState.WaitingForLocation, nearby.state.value)
    }

    @Test fun staleDeadlineStopsObservationAndFreshPositionCanResume() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val store = Store()
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { testScheduler.currentTime }, { 1_000_000 })
        runCurrent()
        store.requests.single().rows.value = emptyList()
        runCurrent()
        advanceTimeBy(5_000)
        assertNotNull(nearby.currentCandidates())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(CandidateState.WaitingForLocation, nearby.state.value)
        assertEquals(0, store.active)
        positions.value = position(5_001)
        runCurrent()
        assertEquals(CandidateState.Loading, nearby.state.value)
        assertEquals(1, store.active)
    }

    @Test fun radiusBoundaryAndInvalidRowsAreNotSilentlyAccepted() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position())
        val store = Store()
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { 0 }, { 1_000_000 })
        runCurrent()
        val near = note(1, latitude = 37.5 + Math.toDegrees(399.99 / 6_371_000.0))
        val far = note(2, latitude = 37.5 + Math.toDegrees(400.01 / 6_371_000.0))
        store.requests.single().rows.value = listOf(far, near)
        runCurrent()
        assertEquals(listOf(near), nearby.currentCandidates()!!.map { it.note })
        store.requests.single().rows.value = listOf(note().copy(latitude = Double.NaN))
        runCurrent()
        assertEquals(CandidateState.Failed(CandidateFailure.INDEX_ERROR), nearby.state.value)
        assertNull(nearby.currentCandidates())
    }

    @Test fun areaAndH3OrStorageFailuresAreNotSuccessfulEmptyLists() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position(longitude = 127.07))
        val index = Index()
        val store = Store()
        val nearby = NearbyNoteCandidates(backgroundScope, positions, index, store, { 0 }, { 1_000_000 })
        runCurrent()
        assertEquals(CandidateState.Failed(CandidateFailure.OUTSIDE_AREA), nearby.state.value)
        assertTrue(store.requests.isEmpty())
        index.error = SpatialUnavailableException(UnsatisfiedLinkError("test"))
        positions.value = position()
        runCurrent()
        assertEquals(CandidateState.Failed(CandidateFailure.H3_UNAVAILABLE), nearby.state.value)
        assertNull(nearby.currentCandidates())
        index.error = null
        index.overrideCells = (0..CandidateArea.MAX_QUERY_CELLS).map { "cell-$it" }.toSet()
        positions.value = position(session = 2)
        runCurrent()
        assertEquals(CandidateState.Failed(CandidateFailure.INDEX_ERROR), nearby.state.value)
        assertTrue(store.requests.isEmpty())
        val failedStore = object : NearbyNoteStore {
            override fun observeNearby(cells: Set<String>): Flow<List<RoadNote>> = flow { throw IOException("test") }
        }
        val failed = NearbyNoteCandidates(backgroundScope, MutableStateFlow(position()), Index(), failedStore, { 0 }, { 1_000_000 })
        runCurrent()
        assertEquals(CandidateState.Failed(CandidateFailure.STORAGE_ERROR), failed.state.value)
    }

    @Test fun distanceOrderWorksInBothDirectionsAndTiesUseNoteIdWhileOutsideCenterNotesRemainEligible() = runTest {
        val positions = MutableStateFlow<TrackedPosition?>(position(longitude = 127.0200))
        val store = Store()
        val nearby = NearbyNoteCandidates(backgroundScope, positions, Index(), store, { 0 }, { 1_000_000 })
        runCurrent()
        val outside = note(1, longitude = 127.0199)
        val farther = note(2, longitude = 127.022)
        store.requests.single().rows.value = listOf(farther, note(3, longitude = 127.0199), outside)
        runCurrent()
        assertEquals(listOf(outside, note(3, longitude = 127.0199), farther), nearby.currentCandidates()!!.map { it.note })
        positions.value = position(longitude = 127.0221)
        runCurrent()
        assertEquals(farther, nearby.currentCandidates()!!.first().note)
        positions.value = position(longitude = 127.0201)
        runCurrent()
        assertEquals(outside, nearby.currentCandidates()!!.first().note)
    }
}
