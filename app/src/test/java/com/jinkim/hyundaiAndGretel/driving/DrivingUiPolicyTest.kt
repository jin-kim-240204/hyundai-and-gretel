package com.jinkim.hyundaiAndGretel.driving

import com.jinkim.hyundaiAndGretel.gleo.GleoProbeEvent
import com.jinkim.hyundaiAndGretel.gleo.GleoStatus
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class DrivingUiPolicyTest {
    @Test fun unknownStateAndPartialRestrictionsNeverShowFullLogs() {
        assertTrue(useCompactDrivingUi(null, null))
        assertTrue(useCompactDrivingUi(false, null))
        assertTrue(useCompactDrivingUi(true, 0))
        assertTrue(useCompactDrivingUi(false, 32))
        assertFalse(useCompactDrivingUi(false, 0))
    }

    @Test fun drivingStatusShowsOnlyShortResultAndNeverParameters() {
        val event = GleoProbeEvent(1, Instant.EPOCH, "secret function", "private JSON", "private extras")
        assertEquals("Gleo에게 목격한 내용을 말해 주세요.", drivingStatus(null))
        assertEquals("제보 내용을 확인하고 있습니다.", drivingStatus(event))
        assertEquals("제보 내용을 확인했습니다.", drivingStatus(event.copy(elapsedMs = 5, status = GleoStatus.SUCCESS)))
        assertEquals("Gleo의 질문에 답해 주세요.", drivingStatus(event.copy(elapsedMs = 5, status = GleoStatus.ASKING)))
        assertEquals("제보를 처리하지 못했습니다.", drivingStatus(event.copy(elapsedMs = 5000, status = GleoStatus.FAIL)))
        assertFalse(drivingStatus(event).contains("private"))
    }
}
