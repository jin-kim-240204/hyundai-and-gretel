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

    @Test fun detailedRoadReportsKeepTheirCompleteOriginalContent() {
        val reports = listOf(
            "방금 지나온 곳 오른쪽 차로에 멧돼지 사체가 있어. 제보해줘.",
            "갓길에 고라니가 서 있어. 도로로 뛰어들 수 있으니 조심하라고 알려줘.",
            "2차로에 화물차에서 떨어진 나무 팔레트가 있어. 제보해줘.",
            "커브 돌자마자 오른쪽 차로에 큰 나뭇가지가 쓰러져 있어.",
            "터널 안 왼쪽 차로에 비상등 켠 차가 멈춰 있어. 뒤차에 알려줘.",
            "합류 지점 지나서 차 두 대가 부딪혔어. 1차로가 막혀 있어.",
            "방금 건넌 다리에서 차가 살짝 미끄러졌어. 도로가 얼었을 수도 있다고 알려줘.",
            "지하차도 입구에 물이 많이 고여 있어. 제보해줘.",
            "교차로에서 우회전하는 쪽에 기름처럼 보이는 게 길게 번져 있어.",
            "방금 지나온 고갯길에 안개가 너무 짙어서 앞차가 잘 안 보여.",
            "방금 지난 사거리 신호등이 전부 꺼져 있어. 조심하라고 알려줘.",
            "터널 나오자마자 야간 공사 중이야. 오른쪽 차로가 막혀 있어.",
            "커브 끝나자마자 차들이 멈춰 있어. 들어오기 전에 속도 줄이라고 알려줘.",
            "산길 오른쪽 차로에 비탈에서 내려온 흙하고 돌이 쌓여 있어.",
            "시장 앞 도로가 오늘 행사 때문에 통제 중이야. 이 근처 오는 차에 알려줘.",
        )
        for (report in reports) {
            val result = validateGleoParameters(basic + ("content" to report))
            assertTrue(report, result is ParsedGleoAction.Valid)
            assertEquals(report, (result as ParsedGleoAction.Valid).input.content)
        }
    }

    @Test fun optionalGuidanceIsIndependentAndInvalidValuesFallBack() {
        val guidance = "공사 중은 아니며 도로에 물체가 있는 것 같습니다."
        val valid = validateGleoParameters(basic + ("receiverGuidance" to " $guidance ")) as ParsedGleoAction.Valid
        assertEquals(basic["content"], valid.input.content)
        assertEquals(guidance, valid.input.receiverGuidance)
        for (invalid in listOf(null, " ", "A road hazard has been reported.", 42, listOf(guidance), "🚀".repeat(501))) {
            val legacy = validateGleoParameters(basic + ("receiverGuidance" to invalid)) as ParsedGleoAction.Valid
            assertNull(legacy.input.receiverGuidance)
        }
    }

    @Test fun callbackSuccessWaitsForCommittedResultAndReturnsSavedId() = runTest {
        val completion = CompletableDeferred<SubmissionState>()
        var calls = 0
        val guidance = "도로에 구덩이가 있습니다."
        val processor = GleoActionProcessor { input, receivedAt ->
            calls++
            assertEquals(basic["content"], input.content)
            assertEquals(NoteCategory.WARNING, input.category)
            assertEquals(guidance, input.receiverGuidance)
            assertEquals(38_000L, receivedAt)
            completion.await()
        }
        val action = validateGleoParameters(basic + ("receiverGuidance" to guidance))
        val response = async { processor.process(action, 38_000) }
        runCurrent()
        assertFalse(response.isCompleted)
        completion.complete(SubmissionState.Saved(note)); runCurrent()
        assertEquals(GleoStatus.SUCCESS, response.await().status)
        assertEquals(note.noteId, response.await().noteId)
        assertNull(response.await().reason)
        assertEquals(1, calls)
    }

    @Test fun rejectedInputNeverCallsSubmission() = runTest {
        val processor = GleoActionProcessor { _, _ -> error("must not submit") }
        assertEquals(GleoStatus.FAIL, processor.process(validateGleoParameters(emptyMap()), 0).status)
    }

    @Test fun failedPendingAndThrownStorageResultsNeverClaimSuccess() = runTest {
        for (result in SubmissionFailure.entries.map { SubmissionState.Failed(it) } + SubmissionState.Submitting) {
            val processor = GleoActionProcessor { _, _ -> result }
            val response = processor.process(validateGleoParameters(basic), 0)
            assertEquals(GleoStatus.FAIL, response.status)
            assertNull(response.noteId)
        }
        val processor = GleoActionProcessor { _, _ -> throw IOException("private details") }
        val response = processor.process(validateGleoParameters(basic), 0)
        assertEquals("SAVE_FAILED", response.reason)
        assertFalse(response.message.contains("private"))
    }

    @Test fun cancellationPropagatesWithoutAFakeSuccess() = runTest {
        val processor = GleoActionProcessor { _, _ -> throw CancellationException("cancel") }
        try {
            processor.process(validateGleoParameters(basic), 0)
            fail("must propagate cancellation")
        } catch (_: CancellationException) { }
    }
}
