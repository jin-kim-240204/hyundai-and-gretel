package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.SubmissionState

internal fun shouldStopTrackingService(status: TrackingStatus, submission: SubmissionState, receiptWritesPending: Boolean = false): Boolean =
    !receiptWritesPending && !status.isCollecting && submission != SubmissionState.Submitting
