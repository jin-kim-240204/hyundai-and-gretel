package com.jinkim.hyundaiAndGretel.speech

import com.jinkim.hyundaiAndGretel.gleo.ParsedGleoAction
import com.jinkim.hyundaiAndGretel.gleo.validateGleoParameters
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptAnnouncementTest {
    private class Store : ReceiptStore {
        var rows = emptyList<InboxItem>()
        var readGate: CompletableDeferred<Unit>? = null
        var confirms = 0
        override fun observeInbox(recipientId: String) = flow {
            readGate?.await()
            emit(rows.filter { it.recipientId == recipientId })
        }
        override suspend fun receive(request: ReceiptRequest) = error("Announcement does not insert receipts")
        override suspend fun confirm(noteId: String, recipientId: String) { confirms++ }
    }

    private class Speaker : NoteSpeaker {
        val texts = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var prepare: CompletableDeferred<Unit>? = null
        var failure: Exception? = null
        var stops = 0
        var active = 0
        var maxActive = 0
        override suspend fun speak(text: String, canStart: () -> Boolean) {
            prepare?.await()
            if (!canStart()) return
            active++
            maxActive = maxOf(maxActive, active)
            try {
                texts += text
                gate?.await()
                failure?.let { throw it }
            } finally { active-- }
        }
        override fun stop() { stops++ }
    }

    private class Harness(scope: CoroutineScope) {
        val store = Store()
        val speaker = Speaker()
        var now = 10L
        var owner: AnnouncementOwner? = AnnouncementOwner("B", null, 1)
        var readBody = true
        val failures = mutableListOf<Exception>()
        val events = mutableListOf<String>()
        val announcements = ReceiptAnnouncement(
            scope, store, speaker, { owner }, { now }, { readBody }, failures::add, events::add,
        )

        fun add(id: Int, guidance: String? = null): ReceiptRequest {
            val note = RoadNote("note-$id", "A", "오른쪽에 물체가 있는 것 같습니다.", NoteCategory.WARNING,
                37.5, 127.0, 0, 0, RoadNote.TTL_MILLIS, receiverGuidance = guidance)
            store.rows = store.rows + InboxItem(note, "B", now, 10.0, null)
            return ReceiptRequest(note.noteId, SyntheticSession("B"), LocationFix(37.5, 127.0, 0))
        }
    }

    @Test fun onlyExplicitNewReceiptPlaysAndDuplicatesNeverAckTheScreen() = runTest {
        val h = Harness(backgroundScope)
        val guidance = "오른쪽에 물체가 있는 것 같습니다."
        val input = h.add(1, guidance)
        runCurrent()
        assertTrue(h.speaker.texts.isEmpty())
        h.announcements.enqueue(input)
        h.announcements.enqueue(input)
        runCurrent()
        assertEquals(listOf(guidance), h.speaker.texts)
        assertEquals(0, h.store.confirms)
        assertNull(h.store.rows.single().displayedAtUtcMillis)
    }

    @Test fun playbackWaitsForCompletionBeforeStartingNext() = runTest {
        val h = Harness(backgroundScope)
        h.speaker.gate = CompletableDeferred()
        h.announcements.enqueue(h.add(1)); h.announcements.enqueue(h.add(2))
        runCurrent()
        assertEquals(1, h.speaker.texts.size)
        h.speaker.gate!!.complete(Unit)
        runCurrent()
        assertEquals(2, h.speaker.texts.size)
        assertEquals(1, h.speaker.maxActive)
    }

    @Test fun ownerChangeDuringDatabaseReadDoesNotSpeakToTheNewOwner() = runTest {
        val h = Harness(backgroundScope)
        h.store.readGate = CompletableDeferred()
        h.announcements.enqueue(h.add(1)); runCurrent()
        h.owner = AnnouncementOwner("C", null, 2)
        h.store.readGate!!.complete(Unit); runCurrent()
        assertTrue(h.speaker.texts.isEmpty())
    }

    @Test fun resetCancelsPlaybackAndDropsQueuedItems() = runTest {
        val h = Harness(backgroundScope)
        h.speaker.gate = CompletableDeferred()
        h.announcements.enqueue(h.add(1)); h.announcements.enqueue(h.add(2)); runCurrent()
        h.announcements.reset(); runCurrent()
        h.speaker.gate!!.complete(Unit); runCurrent()
        assertEquals(1, h.speaker.texts.size)
        assertEquals(0, h.speaker.active)
        assertEquals(1, h.speaker.stops)
        assertTrue(h.failures.isEmpty())
    }

    @Test fun expiredDeletedAndMismatchedScenarioReceiptsAreSkipped() = runTest {
        for (reason in listOf("expired", "deleted", "scenario")) {
            val h = Harness(backgroundScope)
            val input = h.add(1)
            when (reason) {
                "expired" -> h.now = RoadNote.TTL_MILLIS
                "deleted" -> h.store.rows = emptyList()
                "scenario" -> h.store.rows = h.store.rows.map { it.copy(note = it.note.copy(scenarioId = "other")) }
            }
            h.announcements.enqueue(input); runCurrent()
            assertTrue(reason, h.speaker.texts.isEmpty())
        }
    }

    @Test fun engineFailureDropsRemainingAudioWithoutChangingTheInbox() = runTest {
        val h = Harness(backgroundScope)
        h.speaker.failure = SpeechFailure("TTS_PLAYBACK", "ON_DEVICE_ENGINE_ERROR")
        h.announcements.enqueue(h.add(1)); h.announcements.enqueue(h.add(2)); runCurrent()
        assertEquals(1, h.speaker.texts.size)
        assertEquals("TTS_PLAYBACK ON_DEVICE_ENGINE_ERROR", speechFailureCode(h.failures.single()))
        assertEquals(2, h.store.rows.size)
        assertEquals(0, h.store.confirms)
    }

    @Test fun queueIsBoundedAndUnknownOwnerDoesNotEnqueue() = runTest {
        val h = Harness(backgroundScope)
        h.owner = null
        h.announcements.enqueue(h.add(0)); runCurrent()
        assertTrue(h.speaker.texts.isEmpty())
        h.owner = AnnouncementOwner("B", null, 1)
        repeat(20) { h.announcements.enqueue(h.add(it + 1)) }
        runCurrent()
        assertEquals(8, h.speaker.texts.size)
    }

    @Test fun policyRecoveryOnlyAllowsNewReceipts() = runTest {
        val h = Harness(backgroundScope)
        h.owner = null
        h.announcements.enqueue(h.add(1))
        h.owner = AnnouncementOwner("B", null, 1)
        runCurrent()
        assertTrue(h.speaker.texts.isEmpty())
        assertEquals(listOf("ENQUEUE_NO_OWNER"), h.events)

        h.announcements.enqueue(h.add(2))
        runCurrent()
        assertEquals(1, h.speaker.texts.size)
        assertEquals(
            listOf("ENQUEUE_NO_OWNER", "ENQUEUE_ACCEPTED", "LOOKUP_STARTED", "LOOKUP_DONE"),
            h.events,
        )
        assertEquals(0, h.store.confirms)
    }

    @Test fun unexpectedFailureDiagnosticOmitsExceptionMessage() {
        assertEquals("IOException", speechFailureCode(IOException("private note text")))
    }

    @Test fun readTimeoutDoesNotReplayStaleQueuedNotesOnTheNextEvent() = runTest {
        val h = Harness(backgroundScope)
        h.store.readGate = CompletableDeferred()
        h.announcements.enqueue(h.add(1)); h.announcements.enqueue(h.add(2)); runCurrent()
        advanceTimeBy(5_001); runCurrent()
        assertEquals("LOOKUP TIMEOUT", speechFailureCode(h.failures.single()))
        h.store.readGate = null
        h.announcements.enqueue(h.add(3)); runCurrent()
        assertEquals(1, h.speaker.texts.size)
    }

    @Test fun stateChangesDuringEnginePreparationPreventSpeech() = runTest {
        for (change in 0..2) {
            val h = Harness(backgroundScope)
            val ready = CompletableDeferred<Unit>()
            h.speaker.prepare = ready
            h.announcements.enqueue(h.add(1)); runCurrent()
            when (change) {
                0 -> h.now = RoadNote.TTL_MILLIS
                1 -> h.owner = AnnouncementOwner("A", null, 2)
                2 -> h.readBody = false
            }
            ready.complete(Unit); runCurrent()
            assertTrue(h.speaker.texts.isEmpty())
        }
    }

    @Test fun permittedGuidancePreservesNegationAndUncertaintyWithoutAPrefix() {
        val h = Harness(CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
        h.add(1, "도로 경고입니다. 공사 중은 아닙니다.\n 물체가 있는 것 같습니다.")
        val note = h.store.rows.single().note
        assertEquals("도로 경고입니다. 공사 중은 아닙니다. 물체가 있는 것 같습니다.", announcementText(note, true))

        val limit = "🚧".repeat(120)
        assertEquals(limit, announcementText(note.copy(receiverGuidance = limit), true))
        val information = note.copy(category = NoteCategory.INFORMATION)
        assertEquals("도로 안내 쪽지가 도착했습니다. 자세한 내용은 안전한 곳에서 확인해 주세요.",
            announcementText(information, false))
    }

    @Test fun invalidGleoGuidanceAndLegacyRowsOnlyPlayTheArrivalNotice() = runTest {
        val h = Harness(backgroundScope)
        val report = mapOf("content" to "공사 중은 아님", "category" to "WARNING", "locationMode" to "CURRENT_ROAD")
        val actions = listOf(report) + listOf(null, " \n ", 42, "🚧".repeat(501), "🚧".repeat(121))
            .map { report + ("receiverGuidance" to it) }
        for ((index, action) in actions.withIndex()) {
            val input = (validateGleoParameters(action) as ParsedGleoAction.Valid).input
            val notice = h.add(index, input.receiverGuidance)
            h.announcements.enqueue(notice)
            runCurrent()
        }
        h.announcements.enqueue(h.add(actions.size, " \n "))
        runCurrent()
        val general = "도로 경고 쪽지가 도착했습니다. 자세한 내용은 안전한 곳에서 확인해 주세요."
        assertEquals(List(actions.size + 1) { general }, h.speaker.texts)
        assertEquals(0, h.store.confirms)
    }
}
