package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GleoActionTest {
    private val now = Instant.parse("2026-11-09T08:37:56Z")
    private val basic = mapOf<String, Any?>(
        "content" to "도로에 구덩이가 있음", "category" to "WARNING", "locationMode" to "CURRENT_ROAD",
    )

    @Test fun androidActionIncludesPackageButSchemaFunctionNameDoesNot() {
        assertEquals("com.jinkim.hyundaiAndGretel.$CREATE_ROAD_NOTE", GLEO_INTENT_ACTION)
    }

    @Test fun validImmediateReportUsesVehicleDirectionWithoutObservationTime() {
        val input = (validateGleoParameters(basic) as ParsedGleoAction.Valid).input
        assertEquals(LocationMode.CURRENT_ROAD, input.locationMode)
        assertEquals(DirectionMode.CURRENT_VEHICLE, input.directionMode)
        assertNull(input.observedAt)
        assertNull(input.draftId)
    }

    @Test fun missingInvalidOrOversizedInputsFailWithoutFollowUp() {
        for (key in basic.keys) {
            for (values in listOf(basic - key, basic + (key to " "), basic + (key to null), basic + (key to 42))) {
                assertEquals(GleoStatus.FAIL, previewGleoAction(validateGleoParameters(values)).status)
            }
        }
        for (values in listOf(
            basic + ("locationMode" to "NAMED_PLACE"),
            basic + ("locationMode" to "DEFERRED"),
            basic + ("locationMode" to "NEEDS_CLARIFICATION"),
            basic + ("category" to "OTHER"),
            basic + ("content" to "가".repeat(501)),
            basic + ("category" to "a".repeat(33)),
            basic + ("locationMode" to "a".repeat(33)),
        )) assertEquals(GleoStatus.FAIL, previewGleoAction(validateGleoParameters(values)).status)
        assertTrue(validateGleoParameters(basic + ("content" to "가".repeat(500))) is ParsedGleoAction.Valid)
    }

    @Test fun removedAndUnknownKeysNeverBecomeExecutionParameters() {
        val response = previewGleoAction(validateGleoParameters(basic + mapOf(
            "draftId" to "existing-draft", "observedAt" to "2023-10-03T15:23:50Z",
            "directionMode" to "USER_DESCRIPTION", "placeQuery" to "서울역",
            "unexpected" to mapOf("execute" to "code"),
        )))
        assertEquals(GleoStatus.SUCCESS, response.status)
        assertNull(response.report!!.draftId)
        assertNull(response.report.observedAt)
        assertNull(response.report.placeQuery)
        assertEquals(DirectionMode.CURRENT_VEHICLE, response.report.directionMode)
    }

    @Test fun categoriesAreReturnedAndEachCallCreatesANewReport() = runTest {
        val store = FakeStore()
        for (category in listOf("WARNING", "INFORMATION")) {
            val response = GleoActionProcessor(store).process(validateGleoParameters(basic + ("category" to category)), now)
            assertEquals(GleoStatus.SUCCESS, response.status)
            assertEquals(category, response.report!!.category)
            assertEquals(now, store.receivedAt)
            assertNull(store.saved!!.draftId)
        }
        assertEquals(2, store.saves)
    }

    @Test fun debugProbeReturnsOneTerminalResultWithoutSaving() {
        val response = previewGleoAction(validateGleoParameters(basic))
        assertEquals(GleoStatus.SUCCESS, response.status)
        assertEquals("PROBE_ONLY", response.reason)
        assertEquals("The road report was received successfully.", response.message)
        assertEquals(2, GleoStatus.entries.size)
    }

    @Test fun rejectedInputDoesNotReadOrWriteStore() = runTest {
        val store = FakeStore(failure = IOException("private details"))
        val response = GleoActionProcessor(store).process(validateGleoParameters(emptyMap()), now)
        assertEquals(GleoStatus.FAIL, response.status)
        assertEquals(0, store.consentReads)
        assertEquals(0, store.saves)
    }

    @Test fun noConsentDoesNotSave() = runTest {
        val store = FakeStore(consent = false)
        val response = GleoActionProcessor(store).process(validateGleoParameters(basic), now)
        assertEquals("CONSENT_REQUIRED", response.reason)
        assertNull(store.saved)
    }

    @Test fun saveFailureDoesNotClaimSavedOrLeakException() = runTest {
        for (failure in listOf(IOException("private details"), IllegalArgumentException("private details"))) {
            val store = FakeStore(failure = failure)
            val response = GleoActionProcessor(store).process(validateGleoParameters(basic), now)
            assertEquals(GleoStatus.FAIL, response.status)
            assertEquals("SAVE_FAILED", response.reason)
            assertNull(response.report)
            assertFalse(response.message.contains("private"))
        }
    }

    @Test fun cancellationIsPropagatedWithoutSuccess() = runTest {
        val store = FakeStore(failure = CancellationException("deadline"))
        try {
            GleoActionProcessor(store).process(validateGleoParameters(basic), now)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertNull(store.saved)
        }
    }

    private class FakeStore(var consent: Boolean = true, val failure: Exception? = null) : DraftStore {
        var saved: RoadNoteInput? = null
        var receivedAt: Instant? = null
        var consentReads = 0
        var saves = 0
        override suspend fun hasConsent(): Boolean { consentReads++; return consent }
        override suspend fun setConsent(granted: Boolean) { consent = granted }
        override suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft {
            failure?.let { throw it }
            saves++
            saved = input
            this.receivedAt = receivedAt
            return RoadNoteDraft("draft-$saves", input, receivedAt, receivedAt, DraftState.PENDING_LOCATION)
        }
        override suspend fun list() = emptyList<RoadNoteDraft>()
        override suspend fun delete(id: String) = false
        override suspend fun clear() = Unit
    }
}
