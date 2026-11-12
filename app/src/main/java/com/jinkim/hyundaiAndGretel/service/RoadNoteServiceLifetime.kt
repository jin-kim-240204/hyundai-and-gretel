package com.jinkim.hyundaiAndGretel.service

import android.content.pm.ServiceInfo
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState

internal fun shouldStopRoadNoteService(status: TrackingStatus, submission: SubmissionState, receiptWritesPending: Boolean = false): Boolean =
    !receiptWritesPending && !status.isCollecting && submission != SubmissionState.Submitting

internal fun foregroundServiceType(collecting: Boolean, sdkInt: Int): Int =
    when {
        sdkInt < 29 -> 0
        collecting -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }
