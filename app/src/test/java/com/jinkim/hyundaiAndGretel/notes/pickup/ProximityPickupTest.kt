package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.spatial.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProximityPickupTest {
    private class Source : ProximitySource {
        data class Request(val longitude: Double, val registered: (String) -> Unit, val enter: (String) -> Unit, val error: (Exception) -> Unit)
        val requests = mutableListOf<Request>()
        val removed = mutableListOf<String>()
        var throwAdd = false
        var throwRemove = false
        override fun addProximity(latitude: Double, longitude: Double, onRegistered: (String) -> Unit,
                                  onEnter: (String) -> Unit, onError: (Exception) -> Unit) {
            if (throwAdd) throw IOException("add")
            requests += Request(longitude, onRegistered, onEnter, onError)
        }
        override fun removeProximity(alertId: String) {
            removed += alertId
            if (throwRemove) throw IOException("remove")
        }
    }
    private class Store : ReceiptStore {
        val rows = MutableStateFlow<List<InboxItem>>(emptyList())
        val inputs = mutableListOf<ReceiptRequest>()
        var result = ReceiveResult.RECEIVED
        var gate: CompletableDeferred<Unit>? = null
        override fun observeInbox(recipientId: String) = rows
        override suspend fun receive(request: ReceiptRequest): ReceiveResult {
            inputs += request
            gate?.await()
            return result
        }
        override suspend fun confirm(noteId: String, recipientId: String) = Unit
    }
    private fun note(id: Int = 1, longitude: Double = 127.03) = RoadNote(
        "00000000-0000-0000-0000-${id.toString().padStart(12, '0')}", "A", "구덩이", NoteCategory.WARNING,
        37.5, longitude, 0, 0, 600_000, SyntheticRoute.project(37.5, longitude, SyntheticSession()),
    )
    private inner class Harness(scope: CoroutineScope) {
        var elapsed = 0L
        var utc = 1L
        var sourceActive = true
        val source = Source()
        val positions = MutableStateFlow<TrackedPosition?>(TrackedPosition(1, LocationFix(37.5, 127.03, 0)))
        val signals = MutableStateFlow<CandidateState>(CandidateState.Loading)
        var notes = listOf(note())
        val store = Store()
        val receipts = NoteReceiptProcessor(scope, store, MutableStateFlow("B"), {})
        val pickup = ProximityPickup(scope, positions, signals, {
            val p = positions.value
            if (p == null || elapsed - p.fix.receivedElapsedMillis !in 0L..5_000) null else notes
                .filter { it.isActiveAt(utc) }
                .map { NearbyCandidate(it, distanceMeters(p.fix.latitude, p.fix.longitude, it.latitude, it.longitude)) }
                .filter { it.distanceMeters <= 400 }
        }, receipts, { sourceActive && it === source }, { elapsed }, { utc })
        fun start(session: Long = 1) = pickup.start(session, source, SyntheticSession("B"))
        fun signal() { signals.value = if (signals.value == CandidateState.Loading) CandidateState.WaitingForLocation else CandidateState.Loading }
    }

    @Test fun observationsBelongToExplicitStartStopAndLateEnterCannotWriteAfterStop() = runTest {
        val h = Harness(backgroundScope)
        runCurrent()
        assertEquals(0, h.positions.subscriptionCount.value)
        assertEquals(0, h.signals.subscriptionCount.value)
        h.start(); runCurrent()
        assertEquals(1, h.positions.subscriptionCount.value)
        assertEquals(1, h.signals.subscriptionCount.value)
        val pending = h.source.requests.single()
        h.pickup.stop(); runCurrent()
        assertEquals(0, h.positions.subscriptionCount.value)
        assertEquals(0, h.signals.subscriptionCount.value)
        pending.registered("late"); pending.enter("late"); h.signal(); runCurrent()
        assertEquals(listOf("late"), h.source.removed)
        assertTrue(h.store.inputs.isEmpty())
        assertEquals(PickupState.Waiting, h.pickup.state.value)
        h.start(); runCurrent()
        assertEquals(1, h.positions.subscriptionCount.value)
        assertEquals(2, h.source.requests.size)
        h.source.requests.last().registered("new"); runCurrent()
        assertEquals(1, h.store.inputs.size)
    }

    @Test fun pendingLimitDistanceOrderingAndNoDuplicateRegistration() = runTest {
        val h = Harness(backgroundScope)
        h.positions.value = TrackedPosition(1, LocationFix(37.5, 127.0265, 0))
        h.notes = (1..17).reversed().map { note(it, 127.03 + it * 0.00001) }
        h.start(); runCurrent()
        assertEquals(16, h.source.requests.size)
        assertEquals(127.03001, h.source.requests.first().longitude, 1e-10)
        assertEquals(127.03016, h.source.requests.last().longitude, 1e-10)
        assertEquals(PickupState.Watching(16, 16, 0, 1), h.pickup.state.value)
        repeat(3) { h.signal(); runCurrent() }
        assertEquals(16, h.source.requests.size)
        h.source.requests.first().registered("closest")
        runCurrent()
        assertEquals(16, h.source.requests.size)
        assertTrue(h.store.inputs.isEmpty()) // 등록 전이며 150미터 밖인 상태에서는 수신하면 안 된다.
    }

    @Test fun lateIdsKeepSlotsAndOldEnterCannotReceiveNewSameKeyRequest() = runTest {
        val h = Harness(backgroundScope)
        h.notes = (1..16).map { note(it) }
        h.start(); runCurrent()
        val old = h.source.requests.first()
        h.notes = emptyList(); h.signal(); runCurrent()
        h.notes = listOf(note(20)); h.signal(); runCurrent()
        assertEquals(16, h.source.requests.size)
        old.registered("old")
        runCurrent()
        assertTrue("old" in h.source.removed)
        assertEquals(17, h.source.requests.size)
        old.enter("old"); runCurrent()
        assertTrue(h.store.inputs.isEmpty())
        h.source.requests.last().registered("new"); runCurrent()
        assertEquals(note(20).noteId, h.store.inputs.single().noteId)
    }

    @Test fun alreadyInsideAndStationaryNewNoteReceiveOnceWithoutEnter() = runTest {
        val h = Harness(backgroundScope)
        h.notes = emptyList(); h.start(); runCurrent()
        h.notes = listOf(note()); h.signal(); runCurrent()
        h.source.requests.single().registered("inside"); runCurrent()
        assertEquals(1, h.store.inputs.size)
        repeat(3) { h.source.requests.single().enter("inside"); h.signal(); runCurrent() }
        assertEquals(1, h.store.inputs.size)
        assertEquals(listOf("inside"), h.source.removed)
    }

    @Test fun registrationErrorsBeforeAndAfterSuccessLatchFailureUntilExplicitNewSession() = runTest {
        for (registeredFirst in listOf(false, true)) {
            val h = Harness(backgroundScope)
            h.positions.value = TrackedPosition(1, LocationFix(37.5, 127.0265, 0))
            h.start(); runCurrent()
            val old = h.source.requests.single()
            if (registeredFirst) { old.registered("known"); runCurrent() }
            old.error(IOException("registration")); runCurrent()
            assertEquals(PickupState.Failed(PickupFailure.PROXIMITY), h.pickup.state.value)
            assertEquals(0, h.positions.subscriptionCount.value)
            assertEquals(0, h.signals.subscriptionCount.value)
            repeat(3) { h.signal(); runCurrent() }
            h.start(); runCurrent() // 중복 시작 호출로 실패한 세션을 재시도하지 않는다.
            assertEquals(1, h.source.requests.size)
            if (registeredFirst) assertTrue("known" in h.source.removed)
            h.pickup.stop()
            h.positions.value = h.positions.value!!.copy(session = 2)
            h.start(2); runCurrent()
            old.error(IOException("late")); runCurrent()
            assertTrue(h.pickup.state.value is PickupState.Watching)
            assertEquals(2, h.source.requests.size)
        }
    }

    @Test fun storageFailureNeverRetriesOnSignalsButNewSessionCanTryAgain() = runTest {
        val h = Harness(backgroundScope)
        h.store.result = ReceiveResult.STORAGE_FAILURE
        h.start(); runCurrent()
        h.source.requests.single().registered("one"); runCurrent()
        assertEquals(PickupState.Failed(PickupFailure.STORAGE), h.pickup.state.value)
        assertEquals(0, h.positions.subscriptionCount.value)
        repeat(3) { h.signal(); runCurrent() }
        h.positions.value = null; runCurrent()
        h.positions.value = TrackedPosition(1, LocationFix(37.5, 127.03, 0)); runCurrent()
        assertEquals(1, h.store.inputs.size)
        assertEquals(1, h.source.requests.size)
        h.pickup.stop(); h.positions.value = h.positions.value!!.copy(session = 2)
        h.start(2); runCurrent()
        h.source.requests.last().registered("two"); runCurrent()
        assertEquals(2, h.store.inputs.size)
    }

    @Test fun acceptedOldStorageFailureCannotFailNewSessionAndReleasePreventsLateSdkCalls() = runTest {
        val h = Harness(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        h.store.gate = gate; h.store.result = ReceiveResult.STORAGE_FAILURE
        h.start(); runCurrent()
        h.source.requests.single().registered("accepted"); runCurrent()
        assertTrue(h.receipts.hasPendingWrites)
        h.pickup.stop()
        h.positions.value = h.positions.value!!.copy(session = 2)
        h.notes = listOf(note(2)); h.start(2); runCurrent()
        val pending = h.source.requests.last()
        gate.complete(Unit); runCurrent()
        assertTrue(h.pickup.state.value is PickupState.Watching)
        h.sourceActive = false; h.pickup.stop()
        val removed = h.source.removed.size
        pending.registered("after-release"); runCurrent()
        assertEquals(removed, h.source.removed.size)
        assertEquals(PickupState.Waiting, h.pickup.state.value)
    }

    @Test fun freshnessRechecksCurrentPositionAndFutureReceiptsAreRejected() = runTest {
        val h = Harness(backgroundScope)
        h.elapsed = 2_001
        h.start(); runCurrent()
        val request = h.source.requests.single()
        request.registered("freshness"); runCurrent()
        assertTrue(h.store.inputs.isEmpty())
        h.positions.value = h.positions.value!!.copy(fix = LocationFix(37.5, 127.03, 3_000)); runCurrent()
        request.enter("freshness"); runCurrent()
        assertTrue(h.store.inputs.isEmpty())
        h.elapsed = 3_000
        h.signal(); runCurrent()
        h.source.requests.last().registered("restored"); runCurrent()
        assertEquals(1, h.store.inputs.size)
    }

    @Test fun receiptClockBoundaryAndLegacySelfDirectionFiltersAreNotSdkMeasurements() = runTest {
        for ((elapsed, expected) in listOf(2_000L to 1, 2_001L to 0)) {
            val h = Harness(backgroundScope)
            h.elapsed = elapsed
            h.start(); runCurrent()
            h.source.requests.single().registered("boundary"); runCurrent()
            assertEquals(expected, h.store.inputs.size)
        }
        val h = Harness(backgroundScope)
        h.notes = listOf(note().copy(route = null), note(2).copy(authorId = "B"),
            note(3).copy(route = note(3).route!!.copy(direction = -1)))
        h.start(); runCurrent()
        assertTrue(h.source.requests.isEmpty())
    }

    @Test fun invalidationBeforeRegistrationRemovesLateIdWithoutReceiving() = runTest {
        val h = Harness(backgroundScope)
        h.start(); runCurrent()
        val old = h.source.requests.single()
        h.positions.value = null; runCurrent()
        old.registered("invalidated"); runCurrent()
        assertEquals(listOf("invalidated"), h.source.removed)
        assertTrue(h.store.inputs.isEmpty())
        h.positions.value = TrackedPosition(1, LocationFix(37.5, 127.03, 0)); runCurrent()
        assertEquals(2, h.source.requests.size)
        old.error(IOException("late")); runCurrent()
        assertTrue(h.pickup.state.value is PickupState.Watching)
    }

    @Test fun synchronousAddAndRemovalExceptionsStopReceivingButContinueCleanup() = runTest {
        val h = Harness(backgroundScope)
        h.source.throwAdd = true
        h.start(); runCurrent()
        assertEquals(PickupState.Failed(PickupFailure.PROXIMITY), h.pickup.state.value)
        assertEquals(0, h.positions.subscriptionCount.value)
        h.pickup.stop(); h.source.throwAdd = false
        h.positions.value = TrackedPosition(2, LocationFix(37.5, 127.0265, 0))
        h.notes = listOf(note(1), note(2)); h.start(2); runCurrent()
        h.source.requests[0].registered("a"); h.source.requests[1].registered("b"); runCurrent()
        h.source.throwRemove = true
        h.source.requests[0].error(IOException("after")); runCurrent()
        assertTrue(h.source.removed.containsAll(listOf("a", "b")))
        val count = h.source.requests.size
        h.signal(); runCurrent()
        assertEquals(count, h.source.requests.size)
    }
}
