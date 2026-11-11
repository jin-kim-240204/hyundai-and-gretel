package com.jinkim.hyundaiAndGretel.ui.location

import com.jinkim.hyundaiAndGretel.notes.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.SubmissionState

internal enum class DebugSubmissionMessage {
    CONNECTING, READY, SAVING, SAVED, LOCATION_UNAVAILABLE, INVALID_TIME, INVALID_INPUT, DUPLICATE, STORAGE, CANCELLED,
}

internal fun SubmissionState?.debugMessage(): DebugSubmissionMessage = when (this) {
    null -> DebugSubmissionMessage.CONNECTING
    SubmissionState.Idle -> DebugSubmissionMessage.READY
    SubmissionState.Submitting -> DebugSubmissionMessage.SAVING
    is SubmissionState.Saved -> DebugSubmissionMessage.SAVED
    is SubmissionState.Failed -> when (reason) {
        SubmissionFailure.LOCATION_UNAVAILABLE -> DebugSubmissionMessage.LOCATION_UNAVAILABLE
        SubmissionFailure.INVALID_TIME -> DebugSubmissionMessage.INVALID_TIME
        SubmissionFailure.INVALID_INPUT -> DebugSubmissionMessage.INVALID_INPUT
        SubmissionFailure.DUPLICATE -> DebugSubmissionMessage.DUPLICATE
        SubmissionFailure.STORAGE -> DebugSubmissionMessage.STORAGE
        SubmissionFailure.CANCELLED -> DebugSubmissionMessage.CANCELLED
    }
}
