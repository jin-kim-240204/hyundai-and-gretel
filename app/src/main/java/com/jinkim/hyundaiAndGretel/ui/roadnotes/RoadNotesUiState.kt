package com.jinkim.hyundaiAndGretel.ui.roadnotes

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.pickup.InboxState
import com.jinkim.hyundaiAndGretel.notes.pickup.PickupState
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

data class RoadNotesUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.STOPPED,
    val submission: SubmissionState? = null,
    val inbox: InboxState = InboxState(),
    val pickup: PickupState = PickupState.Waiting,
    val restrictions: UiRestrictions = UiRestrictions(),
    val visitedContent: Set<String> = emptySet(),
) {
    val canSubmit get() = submission != null && trackingStatus.isCollecting && submission != SubmissionState.Submitting
    val canStart get() = !trackingStatus.isCollecting && trackingStatus != TrackingStatus.UNSUPPORTED &&
        submission != SubmissionState.Submitting
}

