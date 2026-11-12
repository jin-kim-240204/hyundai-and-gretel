package com.jinkim.hyundaiAndGretel.notes.pickup

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReceiptKey(val noteId: String, val recipientId: String)
data class InboxState(
    val items: List<InboxItem> = emptyList(),
    val confirming: ReceiptKey? = null,
    val error: Boolean = false,
) {
    val pending get() = confirming?.let { key -> items.firstOrNull { it.note.noteId == key.noteId && it.recipientId == key.recipientId } }
        ?: items.firstOrNull { it.displayedAtUtcMillis == null }
}

/** 메인 실행 문맥에서 관리한다. 수락한 쓰기는 위치 수집 중지나 화면 연결 해제 후에도 완료한다. */
class NoteReceiptProcessor(
    private val scope: CoroutineScope,
    private val store: ReceiptStore,
    private val recipient: StateFlow<String>,
    private val onFinished: () -> Unit,
    private val onWriteAccepted: () -> Unit = {},
) {
    private val mutableInbox = MutableStateFlow(InboxState())
    val inbox = mutableInbox.asStateFlow()
    private var writes = 0
    val hasPendingWrites get() = writes > 0
    private var confirming: ReceiptKey? = null
    private var observationOwner: Any? = null
    private val refresh = MutableStateFlow(0L)
    fun refreshObservation() { refresh.value++ }

    // 리뷰용: 현재 수신자의 전체 이력을 관찰한다. 역할이 바뀌면 관찰 소유자도 교체하여 이전 화면의 결과와 분리한다.
    init {
        scope.launch {
            combine(recipient, refresh) { actor, _ -> actor }.collectLatest { actor ->
                observationOwner = Any()
                mutableInbox.value = InboxState()
                val ownerContext = coroutineContext
                try {
                    store.observeInbox(actor).collect { items ->
                        ownerContext.ensureActive()
                        if (recipient.value == actor) mutableInbox.value = mutableInbox.value.copy(items = items)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    ownerContext.ensureActive()
                    if (recipient.value == actor) mutableInbox.value = mutableInbox.value.copy(error = true)
                }
            }
        }
    }

    // 리뷰용: 수신 쓰기 수락 → 서비스 유지 확보 → 저장 → 결과 전달 → 남은 쓰기 수 감소 순서다.
    fun receive(request: ReceiptRequest, onResult: (ReceiveResult) -> Unit) {
        try { onWriteAccepted() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { onResult(ReceiveResult.STORAGE_FAILURE); return }
        writes++
        scope.launch {
            try {
                val result = try { store.receive(request) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { ReceiveResult.STORAGE_FAILURE }
                onResult(result)
            } finally {
                writes--
                onFinished()
            }
        }
    }

    // 리뷰용: 현재 화면의 대기 카드와 수신자·쪽지 키가 일치할 때만 명시 확인을 수락한다. 단순 노출은 확인 처리하지 않는다.
    fun confirm(key: ReceiptKey, screenResumed: Boolean) {
        val pending = inbox.value.pending ?: return
        if (!screenResumed || confirming != null || recipient.value != key.recipientId ||
            pending.recipientId != key.recipientId || pending.note.noteId != key.noteId) return
        // 리뷰용: 확인을 수락한 관찰 소유자를 고정한다. B→C→B로 돌아와도 이전 B 작업이 새 B 화면 상태를 변경하면 안 된다.
        val owner = observationOwner ?: return
        try { onWriteAccepted() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (observationOwner === owner && recipient.value == key.recipientId)
                mutableInbox.value = mutableInbox.value.copy(error = true)
            return
        }
        confirming = key
        mutableInbox.value = mutableInbox.value.copy(confirming = key, error = false)
        writes++
        scope.launch {
            try {
                // 리뷰용: DB 쓰기는 최초 수락한 쪽지·수신자 키로 끝낸다. 화면 갱신의 소유권 검사와 DB 저장 수명은 별개다.
                store.confirm(key.noteId, key.recipientId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (observationOwner === owner && recipient.value == key.recipientId) mutableInbox.value = mutableInbox.value.copy(error = true)
            } finally {
                confirming = null
                if (observationOwner === owner && recipient.value == key.recipientId) mutableInbox.value = mutableInbox.value.copy(confirming = null)
                writes--
                onFinished()
            }
        }
    }
}
