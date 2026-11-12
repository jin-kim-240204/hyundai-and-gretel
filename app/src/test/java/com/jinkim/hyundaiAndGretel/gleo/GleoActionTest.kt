package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GleoActionTest {
    private val basic = mapOf<String, Any?>(
        "content" to "도로에 구덩이가 있음", "category" to "WARNING", "locationMode" to "CURRENT_ROAD",
    )
    private val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
        37.5, 127.03, 0, 1_000, 601_000)

    @Test fun schemaAndValidatedInputUseOnlyCurrentReportFields() {
        assertEquals("com.jinkim.hyundaiAndGretel.$CREATE_ROAD_NOTE", GLEO_INTENT_ACTION)
        val input = (validateGleoParameters(basic + mapOf("observedAt" to "2023-01-01", "draftId" to "old"))
            as ParsedGleoAction.Valid).input
        assertEquals(GleoRoadNoteInput("도로에 구덩이가 있음", NoteCategory.WARNING), input)
    }

    @Test fun missingInvalidAndOversizedParametersAreRejected() {
        for (key in basic.keys) for (value in listOf(null, " ", 42))
            assertTrue(validateGleoParameters(basic + (key to value)) is ParsedGleoAction.Rejected)
        for (values in listOf(basic - "content", basic + ("content" to "가".repeat(501)),
            basic + ("category" to "OTHER"), basic + ("locationMode" to "NAMED_PLACE"),
            basic + ("locationMode" to "DEFERRED")))
            assertTrue(validateGleoParameters(values) is ParsedGleoAction.Rejected)
    }

    @Test fun callbackSuccessWaitsForCommittedResultAndReturnsSavedId() = runTest {
        val completion = CompletableDeferred<SubmissionState>()
        var calls = 0
        val processor = GleoActionProcessor { content, category, receivedAt ->
            calls++
            assertEquals(basic["content"], content)
            assertEquals(NoteCategory.WARNING, category)
            assertEquals(38_000L, receivedAt)
            completion.await()
        }
        val response = async { processor.process(validateGleoParameters(basic), 38_000) }
        runCurrent()
        assertFalse(response.isCompleted)
        completion.complete(SubmissionState.Saved(note)); runCurrent()
        assertEquals(GleoStatus.SUCCESS, response.await().status)
        assertEquals(note.noteId, response.await().noteId)
        assertNull(response.await().reason)
        assertEquals(1, calls)
    }

    @Test fun rejectedInputNeverCallsSubmission() = runTest {
        val processor = GleoActionProcessor { _, _, _ -> error("must not submit") }
        assertEquals(GleoStatus.FAIL, processor.process(validateGleoParameters(emptyMap()), 0).status)
    }

    @Test fun failedPendingAndThrownStorageResultsNeverClaimSuccess() = runTest {
        for (result in SubmissionFailure.entries.map { SubmissionState.Failed(it) } + SubmissionState.Submitting) {
            val processor = GleoActionProcessor { _, _, _ -> result }
            val response = processor.process(validateGleoParameters(basic), 0)
            assertEquals(GleoStatus.FAIL, response.status)
            assertNull(response.noteId)
        }
        val processor = GleoActionProcessor { _, _, _ -> throw IOException("private details") }
        val response = processor.process(validateGleoParameters(basic), 0)
        assertEquals("SAVE_FAILED", response.reason)
        assertFalse(response.message.contains("private"))
    }

    @Test fun cancellationPropagatesWithoutAFakeSuccess() = runTest {
        val processor = GleoActionProcessor { _, _, _ -> throw CancellationException("cancel") }
        try {
            processor.process(validateGleoParameters(basic), 0)
            fail("must propagate cancellation")
        } catch (_: CancellationException) { }
    }
}
