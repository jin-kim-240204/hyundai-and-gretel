package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.SubmissionState
import org.junit.Assert.*
import org.junit.Test

class TrackingReceiptLifetimeTest {
    @Test fun lastReceiptOrAckWriteKeepsStoppedServiceAlive() {
        for (status in TrackingStatus.entries) {
            assertFalse(shouldStopTrackingService(status, SubmissionState.Idle, receiptWritesPending = true))
            assertFalse(shouldStopTrackingService(status, SubmissionState.Submitting, receiptWritesPending = false))
            assertEquals(!status.isCollecting, shouldStopTrackingService(status, SubmissionState.Idle, false))
        }
    }
}
