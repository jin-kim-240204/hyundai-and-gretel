package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.drivingstate.CarUxRestrictions as F
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptAdmissionTest {
    private val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
        37.5, 127.03, 0, 0, 600_000)
    private val key = ReceiptKey(note.noteId, "B")
    private val contentId = "receipt:B/${note.noteId}"
    private inner class Store : ReceiptStore {
        val rows = MutableStateFlow(listOf(InboxItem(note, "B", 1, 1.0, null)))
        var calls = 0
        var gate = CompletableDeferred<Unit>().also { it.complete(Unit) }
        override fun observeInbox(recipientId: String) = rows
        override suspend fun receive(request: ReceiptRequest) = ReceiveResult.RECEIVED
        override suspend fun confirm(noteId: String, recipientId: String) {
            calls++; gate.await()
            rows.value = rows.value.map { it.copy(displayedAtUtcMillis = 10) }
        }
    }
    @Test fun rawDepthChangeRejectsQueuedAckBeforeDerivedUiStateCatchesUp() = runTest {
        val store = Store()
        val repo = NoteReceiptProcessor(backgroundScope, store, MutableStateFlow("B"), {})
        val raw = MutableStateFlow(UiRestrictions(connected = true))
        val displayed = raw.map { it }.stateIn(backgroundScope, SharingStarted.Eagerly, raw.value)
        runCurrent()
        raw.value = UiRestrictions(true, true, F.UX_RESTRICTIONS_LIMIT_CONTENT, 100, 10, 0)
        val visited = setOf(contentId)
        assertTrue(displayed.value.allowsContent(contentId, visited))
        // 바인더가 실제 사용하는 수락 함수로 검증한다. 화면 갱신을 기다리지 않고 최신 규제를 읽어야 한다.
        repo.confirmVisibleReceipt(key, true, raw.value, visited)
        assertFalse(repo.hasPendingWrites)
        runCurrent()
        assertEquals(0, store.calls)
        assertEquals(visited, setOf(contentId))
        assertNotNull(repo.inbox.value.pending)
    }
    @Test fun currentVisitedBudgetRejectsNewItemButAcceptedAckFinishesAfterRestrictionsChange() = runTest {
        val store = Store(); store.gate = CompletableDeferred()
        var finished = 0
        val repo = NoteReceiptProcessor(backgroundScope, store, MutableStateFlow("B"), { finished++ })
        val raw = MutableStateFlow(UiRestrictions(true, true, F.UX_RESTRICTIONS_LIMIT_CONTENT, 100, 1, 1))
        runCurrent()
        val visited = mutableSetOf("debug:settings")
        repo.confirmVisibleReceipt(key, true, raw.value, visited.toSet())
        runCurrent()
        assertEquals(0, store.calls)
        assertEquals(setOf("debug:settings"), visited)
        // 누적 항목 수 제한에 도달해도 이미 표시한 쪽지는 확인할 수 있어야 한다.
        repo.confirmVisibleReceipt(key, false, raw.value, setOf(contentId))
        assertFalse(repo.hasPendingWrites)
        repo.confirmVisibleReceipt(key, true, raw.value, setOf(contentId))
        assertTrue(repo.hasPendingWrites)
        raw.value = raw.value.copy(depthLimit = 0, itemLimit = 0)
        runCurrent()
        assertEquals(1, store.calls)
        assertNotNull(repo.inbox.value.pending)
        store.gate.complete(Unit); runCurrent()
        assertFalse(repo.hasPendingWrites)
        assertNull(repo.inbox.value.pending)
        assertEquals(10L, store.rows.value.single().displayedAtUtcMillis)
        assertEquals(1, finished)
    }
}
