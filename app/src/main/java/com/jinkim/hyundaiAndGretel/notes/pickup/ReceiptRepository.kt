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

/** Main-owned; accepted writes outlive tracking stop and screen detachment. */
class ReceiptRepository(
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

    init {
        scope.launch {
            recipient.collectLatest { actor ->
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

    fun confirm(key: ReceiptKey, screenResumed: Boolean) {
        val pending = inbox.value.pending ?: return
        if (!screenResumed || confirming != null || recipient.value != key.recipientId ||
            pending.recipientId != key.recipientId || pending.note.noteId != key.noteId) return
        try { onWriteAccepted() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableInbox.value = mutableInbox.value.copy(error = true); return }
        confirming = key
        mutableInbox.value = mutableInbox.value.copy(confirming = key, error = false)
        writes++
        scope.launch {
            try {
                store.confirm(key.noteId, key.recipientId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (recipient.value == key.recipientId) mutableInbox.value = mutableInbox.value.copy(error = true)
            } finally {
                confirming = null
                if (recipient.value == key.recipientId) mutableInbox.value = mutableInbox.value.copy(confirming = null)
                writes--
                onFinished()
            }
        }
    }
}
