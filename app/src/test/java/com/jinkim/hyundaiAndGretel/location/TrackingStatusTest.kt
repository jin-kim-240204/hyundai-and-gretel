package com.jinkim.hyundaiAndGretel.location

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingStatusTest {
    @Test fun onlyLiveSessionsAreCollecting() {
        val expected = mapOf(
            TrackingStatus.STOPPED to false, TrackingStatus.STARTING to true,
            TrackingStatus.WAITING to true, TrackingStatus.RECEIVING to true,
            TrackingStatus.UNAVAILABLE to true, TrackingStatus.UNSUPPORTED to false,
            TrackingStatus.FAILED to false,
        )
        assertEquals(TrackingStatus.entries.toSet(), expected.keys)
        expected.forEach { (status, collecting) -> assertEquals(status.name, collecting, status.isCollecting) }
    }
}
