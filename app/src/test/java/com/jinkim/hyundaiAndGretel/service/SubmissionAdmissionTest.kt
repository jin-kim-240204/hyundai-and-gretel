package com.jinkim.hyundaiAndGretel.service

import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubmissionAdmissionTest {
    private fun rejection(
        busy: Boolean = false,
        demoEnabled: Boolean = true,
        scenarioId: String? = "active",
        canSubmit: Boolean = true,
        blocksNewWrites: Boolean = false,
        tracking: Boolean = true,
    ) = submissionRejection(busy, demoEnabled, scenarioId, canSubmit, blocksNewWrites, tracking)

    @Test fun releaseRejectsEvenWithTrackingOrAnExistingScenario() {
        assertEquals(SubmissionFailure.DEMO_REQUIRED, rejection(demoEnabled = false, scenarioId = null, canSubmit = false))
        assertEquals(SubmissionFailure.DEMO_REQUIRED, rejection(demoEnabled = false))
    }

    @Test fun activeReadyAuthorIsRequiredAndBusyRetainsPriority() {
        assertEquals(SubmissionFailure.DEMO_REQUIRED, rejection(scenarioId = null))
        assertEquals(SubmissionFailure.DEMO_REQUIRED, rejection(canSubmit = false))
        assertEquals(SubmissionFailure.DEMO_REQUIRED, rejection(blocksNewWrites = true))
        assertEquals(SubmissionFailure.BUSY, rejection(busy = true, blocksNewWrites = true))
        assertEquals(SubmissionFailure.LOCATION_UNAVAILABLE, rejection(tracking = false))
        assertNull(rejection())
    }
}
