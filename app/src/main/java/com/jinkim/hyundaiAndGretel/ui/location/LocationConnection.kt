package com.jinkim.hyundaiAndGretel.ui.location

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.location.LocationSession
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.SubmissionState
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class LocationUiState(
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

/** Main-confined connection switching, also used directly by JVM tests. */
class LocationConnection(scope: CoroutineScope) {
    private val current = MutableStateFlow<LocationSession?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LocationUiState> = current.flatMapLatest { session ->
        if (session == null) flowOf(LocationUiState())
        else combine(session.status, session.submissionState, session.inboxState, session.pickupState) { status, submission, inbox, pickup ->
            LocationUiState(status, submission, inbox, pickup)
        }.combine(session.uiRestrictions) { state, policy -> state.copy(restrictions = policy) }
    }.buffer(0).stateIn(scope, SharingStarted.Eagerly, LocationUiState())

    fun connect(session: LocationSession?) {
        current.value = session
    }

    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean) {
        current.value?.confirmReceipt(key, screenResumed)
    }

    fun submitTestNote(content: String, category: NoteCategory) {
        val session = current.value ?: return
        val latest = LocationUiState(session.status.value, session.submissionState.value)
        if (latest.canSubmit) session.submitTestNote(content, category)
    }
}
