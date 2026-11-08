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
    private val now = Instant.parse("2026-11-08T03:00:20Z")
    private val basic = mapOf<String, Any?>("content" to "도로에 구덩이가 있음", "locationMode" to "CURRENT_ROAD")

    @Test fun androidActionIncludesPackageButSchemaFunctionNameDoesNot() {
        assertEquals("com.jinkim.hyundaiAndGretel.$CREATE_ROAD_NOTE_DRAFT", GLEO_INTENT_ACTION)
    }

    @Test fun currentRoadUsesVehicleDirectionWithoutRequiringSpokenTime() {
        val valid = validateGleoParameters(basic, now) as ParsedGleoAction.Valid
        assertEquals(DirectionMode.CURRENT_VEHICLE, valid.input.directionMode)
        assertNull(valid.input.utteranceRecordedAt)
        assertNull(valid.missingField)
    }

    @Test fun namedPlacePreservesExitQualifierAndDoesNotUseVehicleDirection() {
        val valid = validateGleoParameters(
            basic + mapOf("locationMode" to "NAMED_PLACE", "placeQuery" to "역삼역 4번출구 쪽"), now,
        ) as ParsedGleoAction.Valid
        assertEquals("역삼역 4번출구 쪽", valid.input.placeQuery)
        assertEquals(DirectionMode.UNKNOWN, valid.input.directionMode)
    }

    @Test fun missingPlaceKeepsContentForSavingBeforeQuestion() {
        val valid = validateGleoParameters(basic + ("locationMode" to "NAMED_PLACE"), now) as ParsedGleoAction.Valid
        assertEquals("placeQuery", valid.missingField)
        assertEquals(basic["content"], valid.input.content)
    }

    @Test fun explicitDirectionRequiresItsDescription() {
        val valid = validateGleoParameters(basic + ("directionMode" to "USER_DESCRIPTION"), now) as ParsedGleoAction.Valid
        assertEquals("directionDescription", valid.missingField)
    }

    @Test fun modeConflictsAreRejected() {
        for (values in listOf(
            basic + ("placeQuery" to "뱅뱅사거리"),
            basic + ("directionDescription" to "반대편"),
            basic + mapOf("locationMode" to "NAMED_PLACE", "directionMode" to "CURRENT_VEHICLE"),
        )) assertEquals(GleoStatus.FAIL, rejection(values).status)
    }

    @Test fun requiredMissingIsAskingAndNeverDefaultsToCurrentRoad() {
        for (values in listOf(emptyMap(), basic - "locationMode", basic + ("content" to "  "))) {
            assertEquals(GleoStatus.ASKING, rejection(values).status)
        }
    }

    @Test fun badTypesNullEmptyOptionalAndUnknownEnumsFail() {
        for (values in listOf(
            basic + ("content" to 42), basic + ("content" to null),
            basic + ("locationMode" to "nearby"), basic + ("directionMode" to "north"),
            basic + ("draftId" to " "), basic + ("placeQuery" to listOf("place")),
        )) assertEquals(GleoStatus.FAIL, rejection(values).status)
    }

    @Test fun lengthLimitsRejectExcessAndAcceptBoundary() {
        assertTrue(validateGleoParameters(basic + ("content" to "가".repeat(500)), now) is ParsedGleoAction.Valid)
        assertEquals(GleoStatus.FAIL, rejection(basic + ("content" to "가".repeat(501))).status)
        assertEquals(GleoStatus.FAIL, rejection(basic + ("draftId" to "a".repeat(129))).status)
    }

    @Test fun unknownKeysNeverBecomeExecutionParameters() {
        val valid = validateGleoParameters(basic + ("unexpected" to mapOf("execute" to "code")), now) as ParsedGleoAction.Valid
        assertEquals(2, basic.size)
        assertEquals("도로에 구덩이가 있음", valid.input.content)
    }

    @Test fun timestampsRequireZoneRejectFutureAndNormalizeOffset() {
        for (timestamp in listOf("2026-11-08T03:00:00", "not-a-date", "2026-11-08T03:00:21Z")) {
            assertEquals(GleoStatus.FAIL, rejection(basic + ("utteranceRecordedAt" to timestamp)).status)
        }
        val valid = validateGleoParameters(
            basic + ("utteranceRecordedAt" to "2026-11-08T12:00:00+09:00"), now,
        ) as ParsedGleoAction.Valid
        assertEquals(Instant.parse("2026-11-08T03:00:00Z"), valid.input.utteranceRecordedAt)
    }

    @Test fun noConsentDoesNotSave() = runTest {
        val store = FakeStore(consent = false)
        val response = GleoActionProcessor(store).process(validateGleoParameters(basic, now), now)
        assertEquals("CONSENT_REQUIRED", response.reason)
        assertNull(store.saved)
    }

    @Test fun rejectedInputDoesNotReadOrWriteStore() = runTest {
        val store = FakeStore(failure = IOException("private path"))
        val response = GleoActionProcessor(store).process(validateGleoParameters(emptyMap(), now), now)
        assertEquals(GleoStatus.ASKING, response.status)
        assertEquals(0, store.consentReads)
        assertNull(store.saved)
    }

    @Test fun ambiguousReportSavesBeforeAsking() = runTest {
        val store = FakeStore()
        val response = GleoActionProcessor(store).process(
            validateGleoParameters(basic + ("locationMode" to "NEEDS_CLARIFICATION"), now), now,
        )
        assertNotNull(store.saved)
        assertEquals(GleoStatus.ASKING, response.status)
        assertEquals("draft-001", response.draftId)
        assertFalse(response.message.contains("도로"))
    }

    @Test fun deferredReportSavesWithoutAnotherQuestion() = runTest {
        val store = FakeStore()
        val response = GleoActionProcessor(store).process(
            validateGleoParameters(basic + ("locationMode" to "DEFERRED"), now), now,
        )
        assertNotNull(store.saved)
        assertEquals(GleoStatus.SUCCESS, response.status)
        assertNull(response.missingField)
        assertTrue(response.message.contains("not been published"))
    }

    @Test fun unverifiedTimestampIsNotStoredAsMeasuredUtteranceTime() = runTest {
        val store = FakeStore()
        val response = GleoActionProcessor(store).process(
            validateGleoParameters(basic + ("utteranceRecordedAt" to "2026-11-08T03:00:00Z"), now), now,
        )
        assertEquals(GleoStatus.SUCCESS, response.status)
        assertNull(store.saved!!.utteranceRecordedAt)
        assertEquals(now, store.receivedAt)
    }

    @Test fun saveFailureDoesNotClaimSavedOrLeakException() = runTest {
        val store = FakeStore(failure = IOException("private path and report content"))
        val response = GleoActionProcessor(store).process(validateGleoParameters(basic, now), now)
        assertEquals(GleoStatus.FAIL, response.status)
        assertEquals("SAVE_FAILED", response.reason)
        assertNull(response.draftId)
        assertFalse(response.message.contains("private"))
    }

    @Test fun invalidExistingIdDoesNotCreateReplacement() = runTest {
        val store = FakeStore(failure = IllegalArgumentException("unknown draft"))
        val response = GleoActionProcessor(store).process(
            validateGleoParameters(basic + ("draftId" to "unknown"), now), now,
        )
        assertEquals("INVALID_DRAFT", response.reason)
        assertNull(response.draftId)
    }

    @Test fun cancellationIsPropagatedWithoutSuccess() = runTest {
        val store = FakeStore(failure = CancellationException("deadline"))
        try {
            GleoActionProcessor(store).process(validateGleoParameters(basic, now), now)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertNull(store.saved)
        }
    }

    @Test fun debugProbeValidatesWithoutRequiringConsentOrCreatingSavedDraft() {
        val response = previewGleoAction(validateGleoParameters(basic, now))
        assertEquals(GleoStatus.SUCCESS, response.status)
        assertEquals("PROBE_ONLY", response.reason)
        assertNull(response.draftId)
        assertTrue(response.message.contains("nothing was saved"))
    }

    @Test fun debugProbeAsksForUnclearLocationButDeferredEndsNormally() {
        val unclear = previewGleoAction(validateGleoParameters(basic + ("locationMode" to "NEEDS_CLARIFICATION"), now))
        val deferred = previewGleoAction(validateGleoParameters(basic + ("locationMode" to "DEFERRED"), now))
        assertEquals(GleoStatus.ASKING, unclear.status)
        assertEquals(GleoStatus.SUCCESS, deferred.status)
        assertNull(deferred.missingField)
    }

    @Test fun debugProbeKeepsInvalidParameterFailures() {
        val response = previewGleoAction(validateGleoParameters(basic + ("content" to 42), now))
        assertEquals(GleoStatus.FAIL, response.status)
        assertEquals("INVALID_INPUT", response.reason)
    }

    private fun rejection(values: Map<String, Any?>) =
        (validateGleoParameters(values, now) as ParsedGleoAction.Rejected).response

    private class FakeStore(var consent: Boolean = true, val failure: Exception? = null) : DraftStore {
        var saved: RoadNoteInput? = null
        var receivedAt: Instant? = null
        var consentReads = 0
        override suspend fun hasConsent(): Boolean { consentReads++; return consent }
        override suspend fun setConsent(granted: Boolean) { consent = granted }
        override suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft {
            failure?.let { throw it }
            saved = input
            this.receivedAt = receivedAt
            val state = if (input.locationMode in listOf(LocationMode.DEFERRED, LocationMode.NEEDS_CLARIFICATION)) {
                DraftState.AWAITING_LOCATION
            } else DraftState.PENDING_LOCATION
            return RoadNoteDraft("draft-001", input, receivedAt, receivedAt, state)
        }
        override suspend fun list() = emptyList<RoadNoteDraft>()
        override suspend fun delete(id: String) = false
        override suspend fun clear() = Unit
    }
}
