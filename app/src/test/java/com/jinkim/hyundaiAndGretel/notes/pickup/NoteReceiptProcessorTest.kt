package com.jinkim.hyundaiAndGretel.notes.pickup

import android.content.pm.ServiceInfo
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.foregroundServiceType
import com.jinkim.hyundaiAndGretel.service.shouldStopRoadNoteService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteReceiptProcessorTest {
    private val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
        37.5, 127.03, 0, 0, 600_000, SyntheticRoute.project(37.5, 127.03, SyntheticSession()))
    private class Store : ReceiptStore {
        val rows = MutableStateFlow<List<InboxItem>>(emptyList())
        var confirmCalls = 0
        val confirmations = mutableListOf<ReceiptKey>()
        var fail = false
        var gate = CompletableDeferred<Unit>().also { it.complete(Unit) }
        override fun observeInbox(recipientId: String) = rows.map { list -> list.filter { it.recipientId == recipientId } }
        override suspend fun receive(request: ReceiptRequest): ReceiveResult { gate.await(); return ReceiveResult.RECEIVED }
        override suspend fun confirm(noteId: String, recipientId: String) {
            confirmCalls++
            confirmations += ReceiptKey(noteId, recipientId)
            gate.await()
            if (fail) error("db")
            rows.value = rows.value.map { if (it.note.noteId == noteId && it.recipientId == recipientId) it.copy(displayedAtUtcMillis = 50) else it }
        }
    }

    @Test fun confirmationIsExplicitKeyedAndOnlyAdvancesAfterCommit() = runTest {
        val store = Store()
        val repo = NoteReceiptProcessor(backgroundScope, store, MutableStateFlow("B"), {})
        store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null))
        runCurrent()
        val key = ReceiptKey(note.noteId, "B")
        runCurrent() // 관찰·화면 구성·재진입만으로 확인 처리가 발생하지 않아야 한다.
        assertEquals(0, store.confirmCalls)
        repo.confirm(key, false)
        repo.confirm(key.copy(recipientId = "A"), true)
        repo.confirm(key.copy(noteId = "old"), true)
        runCurrent()
        assertEquals(0, store.confirmCalls)
        store.gate = CompletableDeferred()
        repo.confirm(key, true)
        repo.confirm(key, true)
        runCurrent()
        assertEquals(1, store.confirmCalls)
        assertNotNull(repo.inbox.value.pending)
        assertTrue(repo.hasPendingWrites)
        store.gate.complete(Unit)
        runCurrent()
        assertNull(repo.inbox.value.pending)
        assertFalse(repo.hasPendingWrites)
        assertEquals(50L, repo.inbox.value.items.single().displayedAtUtcMillis)
    }

    @Test fun ackFailureKeepsPendingAndOnlyAnotherExplicitClickRetries() = runTest {
        val store = Store().apply { fail = true }
        val actor = MutableStateFlow("B")
        val repo = NoteReceiptProcessor(backgroundScope, store, actor, {})
        store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null))
        runCurrent()
        val key = ReceiptKey(note.noteId, "B")
        repo.confirm(key, true)
        runCurrent()
        assertTrue(repo.inbox.value.error)
        assertNotNull(repo.inbox.value.pending)
        store.rows.value = store.rows.value.toList()
        runCurrent()
        assertEquals(1, store.confirmCalls)
        store.fail = false
        repo.confirm(key, true)
        runCurrent()
        assertEquals(2, store.confirmCalls)
        assertNull(repo.inbox.value.pending)
    }

    @Test fun acceptedWritesSurviveActorSwitchAndNeverAlterNewInbox() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        val actor = MutableStateFlow("B")
        var finished = 0
        val repo = NoteReceiptProcessor(backgroundScope, store, actor, { finished++ })
        runCurrent()
        val request = ReceiptRequest(note.noteId, SyntheticSession("B"), LocationFix(37.5, 127.03, 0))
        var result: ReceiveResult? = null
        repo.receive(request) { result = it }
        runCurrent()
        actor.value = "C"
        runCurrent()
        assertTrue(repo.hasPendingWrites)
        store.gate.complete(Unit)
        runCurrent()
        assertEquals(ReceiveResult.RECEIVED, result)
        assertTrue(repo.inbox.value.items.isEmpty())
        assertEquals(1, finished)
        assertFalse(repo.hasPendingWrites)
    }

    @Test fun acceptedAckIsFrozenAcrossActorSwitchAndItsFailureDoesNotChangeNewInbox() = runTest {
        val store = Store().apply { gate = CompletableDeferred(); fail = true }
        val actor = MutableStateFlow("B")
        val repo = NoteReceiptProcessor(backgroundScope, store, actor, {})
        store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null))
        runCurrent()
        repo.confirm(ReceiptKey(note.noteId, "B"), true)
        runCurrent()
        actor.value = "C"
        runCurrent()
        assertTrue(repo.hasPendingWrites)
        store.gate.complete(Unit)
        runCurrent()
        assertEquals(InboxState(), repo.inbox.value)
        assertFalse(repo.hasPendingWrites)
        assertEquals(1, store.confirmCalls)
    }

    @Test fun distanceAndReverseProgressUseMetersAroundThe500mBoundary() {
        val scale = 6_371_000.0 * kotlin.math.cos(Math.toRadians(37.5))
        fun at(meters: Double) = 127.03 - Math.toDegrees(meters / scale)
        val setup = SyntheticSession("B")
        assertNotNull(eligibleDistance(note, setup, LocationFix(37.5, at(499.99), 0), 1))
        assertNull(eligibleDistance(note, setup, LocationFix(37.5, at(500.01), 0), 1))
        val reverse = note.copy(route = note.route!!.copy(direction = -1))
        assertNotNull(eligibleDistance(reverse, setup.copy(direction = -1), LocationFix(37.5, at(-499.99), 0), 1))
        assertNull(eligibleDistance(reverse, setup.copy(direction = -1), LocationFix(37.5, at(1.0), 0), 1))
    }

    @Test fun reusedActorObservationRejectsOldAckUiResultsButKeepsLockWriteAndDatabaseUpdates() = runTest {
        for (oldFails in listOf(false, true)) {
            val store = Store().apply { gate = CompletableDeferred(); fail = oldFails }
            val actor = MutableStateFlow("B")
            var finished = 0
            val repo = NoteReceiptProcessor(backgroundScope, store, actor, { finished++ })
            store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null))
            runCurrent()
            val oldKey = ReceiptKey(note.noteId, "B")
            repo.confirm(oldKey, true)
            runCurrent()
            actor.value = "C"; runCurrent()
            actor.value = "B"; runCurrent()
            assertFalse(repo.inbox.value.error)
            assertNull(repo.inbox.value.confirming)
            repo.confirm(oldKey, true); runCurrent()
            assertEquals(1, store.confirmCalls) // 관찰 대상을 바꿔도 이미 수락한 확인 저장의 잠금을 풀면 안 된다.
            assertTrue(repo.hasPendingWrites)
            store.gate.complete(Unit); runCurrent()
            assertFalse(repo.inbox.value.error)
            assertNull(repo.inbox.value.confirming)
            assertEquals(listOf(oldKey), store.confirmations)
            assertEquals(1, finished)
            assertFalse(repo.hasPendingWrites)
            // 이전 작업의 정상 DB 커밋은 새 관찰자의 일반 데이터 흐름을 통해서도 조회할 수 있다.
            assertEquals(if (oldFails) null else 50L, repo.inbox.value.items.single().displayedAtUtcMillis)
            val next = note.copy(noteId = "00000000-0000-0000-0000-000000000002")
            store.rows.value = listOf(InboxItem(next, "B", 20, 0.0, null)); runCurrent()
            store.fail = true
            repo.confirm(ReceiptKey(next.noteId, "B"), true); runCurrent()
            assertTrue(repo.inbox.value.error) // 현재 관찰자 자신의 저장 실패는 화면에 계속 표시돼야 한다.
            assertEquals(next, repo.inbox.value.pending!!.note)
            assertEquals(2, finished)
            assertFalse(repo.hasPendingWrites)
        }
    }

    @Test fun stoppedLocalAckUsesDataSyncWithoutLocationPermissionAndKeepsAcceptedWriteAlive() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        var finished = 0
        var promotedType: Int? = null
        val repo = NoteReceiptProcessor(backgroundScope, store, MutableStateFlow("B"), { finished++ }, onWriteAccepted = {
            val type = foregroundServiceType(collecting = false, sdkInt = 34)
            if (type and ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION != 0) throw SecurityException("No location permission")
            promotedType = type
        })
        store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null)); runCurrent()
        repo.confirm(ReceiptKey(note.noteId, "B"), true); runCurrent()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, promotedType)
        assertEquals(1, store.confirmCalls)
        assertTrue(repo.hasPendingWrites)
        assertFalse(shouldStopRoadNoteService(
            TrackingStatus.STOPPED, SubmissionState.Idle, repo.hasPendingWrites))
        store.gate.complete(Unit); runCurrent()
        assertFalse(repo.hasPendingWrites)
        assertEquals(1, finished)
        assertTrue(shouldStopRoadNoteService(
            TrackingStatus.STOPPED, SubmissionState.Idle, repo.hasPendingWrites))
    }

    @Test fun failedForegroundOrStartedPromotionDoesNotAcceptAckOrReachStorage() = runTest {
        for (failure in listOf(SecurityException("foreground"), IllegalStateException("started"))) {
            val store = Store()
            var finished = 0
            val repo = NoteReceiptProcessor(backgroundScope, store, MutableStateFlow("B"), { finished++ },
                onWriteAccepted = { throw failure })
            store.rows.value = listOf(InboxItem(note, "B", 10, 0.0, null)); runCurrent()
            repo.confirm(ReceiptKey(note.noteId, "B"), true); runCurrent()
            assertEquals(0, store.confirmCalls)
            assertFalse(repo.hasPendingWrites)
            assertNull(repo.inbox.value.confirming)
            assertTrue(repo.inbox.value.error)
            assertNotNull(repo.inbox.value.pending)
            assertEquals(0, finished)
        }
    }

    @Test fun eligibilityPreservesExpirySelfDirectionAndLegacyBoundaries() {
        val p = LocationFix(37.5, 127.03, 0)
        assertEquals(0.0, eligibleDistance(note, SyntheticSession("B"), p, 1)!!, 0.0)
        assertNull(eligibleDistance(note, SyntheticSession(), p, 1))
        assertNull(eligibleDistance(note, SyntheticSession("B"), p, 600_000))
        assertNull(eligibleDistance(note.copy(route = null), SyntheticSession("B"), p, 1))
        assertNull(eligibleDistance(note, SyntheticSession("B", direction = -1), p, 1))
        assertNull(eligibleDistance(note, SyntheticSession("B"), p.copy(longitude = 127.031), 1))
        assertNull(eligibleDistance(note, SyntheticSession("B"), p.copy(latitude = 37.501), 1))
    }
}
