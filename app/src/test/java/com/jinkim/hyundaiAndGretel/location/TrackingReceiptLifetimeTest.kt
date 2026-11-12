package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.SubmissionState
import org.junit.Assert.*
import org.junit.Test

class TrackingReceiptLifetimeTest {
    @Test fun storageOnlyForegroundDoesNotRequireLocationButCollectionStillDoes() {
        for (api in listOf(29, 33, 34, 35)) {
            assertEquals(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                foregroundServiceType(collecting = false, sdkInt = api))
            assertEquals(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                foregroundServiceType(collecting = true, sdkInt = api))
        }
        assertEquals(0, foregroundServiceType(collecting = false, sdkInt = 28))
        assertEquals(0, foregroundServiceType(collecting = true, sdkInt = 28))
    }

    @Test fun lastReceiptOrAckWriteKeepsStoppedServiceAlive() {
        for (status in TrackingStatus.entries) {
            assertFalse(shouldStopTrackingService(status, SubmissionState.Idle, receiptWritesPending = true))
            assertFalse(shouldStopTrackingService(status, SubmissionState.Submitting, receiptWritesPending = false))
            assertEquals(!status.isCollecting, shouldStopTrackingService(status, SubmissionState.Idle, false))
        }
    }
}
