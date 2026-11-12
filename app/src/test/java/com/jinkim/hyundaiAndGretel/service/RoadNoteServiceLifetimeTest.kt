package com.jinkim.hyundaiAndGretel.service

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import org.junit.Assert.*
import org.junit.Test

class RoadNoteServiceLifetimeTest {
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
            assertFalse(shouldStopRoadNoteService(status, SubmissionState.Idle, receiptWritesPending = true))
            assertFalse(shouldStopRoadNoteService(status, SubmissionState.Submitting, receiptWritesPending = false))
            assertEquals(!status.isCollecting, shouldStopRoadNoteService(status, SubmissionState.Idle, false))
        }
    }
}
