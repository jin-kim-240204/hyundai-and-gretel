package com.jinkim.hyundaiAndGretel.location

import android.content.pm.ServiceInfo
import com.jinkim.hyundaiAndGretel.notes.SubmissionState

internal fun shouldStopTrackingService(status: TrackingStatus, submission: SubmissionState, receiptWritesPending: Boolean = false): Boolean =
    !receiptWritesPending && !status.isCollecting && submission != SubmissionState.Submitting

internal fun foregroundServiceType(collecting: Boolean, sdkInt: Int): Int =
    when {
        sdkInt < 29 -> 0
        collecting -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }
