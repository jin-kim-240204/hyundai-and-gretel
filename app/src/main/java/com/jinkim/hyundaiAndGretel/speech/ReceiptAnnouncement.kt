package com.jinkim.hyundaiAndGretel.speech

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptRequest
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AnnouncementOwner(val recipientId: String, val scenarioId: String?, val trackingSession: Long)

/** 서비스 Main에서만 호출한다. UI 관측/확인과 별개로 새 수신 커밋만 읽는다. */
class ReceiptAnnouncement(
    private val scope: CoroutineScope,
    private val store: ReceiptStore,
    private val speaker: NoteSpeaker,
    private val currentOwner: () -> AnnouncementOwner?,
    private val utcMillis: () -> Long,
    private val canReadBody: () -> Boolean,
    private val onFailure: (Exception) -> Unit = {},
    private val log: (String) -> Unit = {},
) {
    private data class Pending(val key: ReceiptKey, val owner: AnnouncementOwner)
    private val pending = ArrayDeque<Pending>()
    private val seen = mutableMapOf<ReceiptKey, Long>()
    private var worker: Job? = null
    private var generation = 0L

    fun enqueue(request: ReceiptRequest) {
        val owner = currentOwner()
        if (owner == null) {
            log("ENQUEUE_NO_OWNER")
            return
        }
        if (owner.recipientId != request.recipient.actorId || owner.scenarioId != request.scenarioId) {
            log("ENQUEUE_OWNER_MISMATCH")
            return
        }
        val now = utcMillis()
        seen.entries.removeAll { entry -> now - entry.value >= RoadNote.TTL_MILLIS }
        val key = ReceiptKey(request.noteId, owner.recipientId)
        if (key in seen || pending.size >= MAX_PENDING) {
            log(if (key in seen) "ENQUEUE_DUPLICATE" else "ENQUEUE_FULL")
            return
        }
        seen[key] = now
        pending.addLast(Pending(key, owner))
        log("ENQUEUE_ACCEPTED")
        if (worker?.isActive == true) return
        val token = generation
        val next = scope.launch(start = CoroutineStart.LAZY) { drain(token) }
        worker = next
        next.start()
    }

    private suspend fun drain(token: Long) {
        try {
            while (pending.isNotEmpty() && token == generation) {
                val next = pending.removeFirst()
                if (next.owner != currentOwner()) {
                    log("LOOKUP_SKIPPED_OWNER_CHANGED")
                    continue
                }
                log("LOOKUP_STARTED")
                val item = withTimeout(LOOKUP_TIMEOUT_MS) {
                    store.observeInbox(next.key.recipientId).first()
                }.firstOrNull { it.note.noteId == next.key.noteId && it.recipientId == next.key.recipientId }
                currentCoroutineContext().ensureActive()
                if (token != generation || next.owner != currentOwner()) {
                    log("LOOKUP_DISCARDED_OWNER_CHANGED")
                    continue
                }
                val note = item?.note
                if (note == null || note.scenarioId != next.owner.scenarioId || !note.isActiveAt(utcMillis())) {
                    log("LOOKUP_NO_ELIGIBLE_NOTE")
                    continue
                }
                log("LOOKUP_DONE")
                val readBody = canReadBody()
                speaker.speak(announcementText(note, readBody)) {
                    // SDK 초기화/선행 묵음 중 바뀐 세션·만료·본문 정책도 출력 전에 확인한다.
                    token == generation && next.owner == currentOwner() &&
                        note.isActiveAt(utcMillis()) && readBody == canReadBody()
                }
            }
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            if (token == generation) pending.clear()
            onFailure(SpeechFailure("LOOKUP", "TIMEOUT"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // 오디오 실패 후 오래된 쪽지를 몰아서 읽지 않으며 수신 DB에는 손대지 않는다.
            if (token == generation) pending.clear()
            onFailure(error)
        }
    }

    fun reset() {
        generation++
        worker?.cancel()
        worker = null
        pending.clear()
        seen.clear()
        speaker.stop()
    }

    private companion object {
        const val MAX_PENDING = 8
        const val LOOKUP_TIMEOUT_MS = 5_000L
    }
}

/** 본문을 새로 추론하지 않는다. 긴 본문은 의미가 바뀔 수 있어 중간을 잘라 읽지 않는다. */
internal fun announcementText(note: RoadNote, readBody: Boolean): String {
    val kind = if (note.category == NoteCategory.WARNING) "도로 경고" else "도로 안내"
    val content = note.receiverMessage.trim().replace(Regex("\\s+"), " ")
    if (!readBody || content.isBlank() || content.codePointCount(0, content.length) > MAX_SPOKEN_CODE_POINTS) {
        return "${kind} 쪽지가 도착했습니다. 자세한 내용은 안전한 곳에서 확인해 주세요."
    }
    return if (note.receiverGuidance != null) content else "${kind}입니다. $content"
}

private const val MAX_SPOKEN_CODE_POINTS = 120
