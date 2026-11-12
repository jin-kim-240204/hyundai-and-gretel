package com.jinkim.hyundaiAndGretel.ui.roadnotes

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.RoadNoteServiceContract
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class RoadNoteServiceConnection(scope: CoroutineScope) {
    private val current = MutableStateFlow<RoadNoteServiceContract?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<RoadNotesUiState> = current.flatMapLatest { session ->
        if (session == null) flowOf(RoadNotesUiState())
        else combine(session.status, session.submissionState, session.inboxState, session.pickupState) { status, submission, inbox, pickup ->
            RoadNotesUiState(status, submission, inbox, pickup)
        }.combine(session.uiRestrictions) { state, policy -> state.copy(restrictions = policy) }
    }.buffer(0).stateIn(scope, SharingStarted.Eagerly, RoadNotesUiState())

    val canStartTracking get() = current.value?.canStartTracking ?: true

    fun connect(session: RoadNoteServiceContract?) {
        current.value = session
    }

    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>) {
        current.value?.confirmReceipt(key, screenResumed, visitedContent)
    }

    fun submitRoadNote(content: String, category: NoteCategory) {
        val session = current.value ?: return
        val latest = RoadNotesUiState(session.status.value, session.submissionState.value)
        if (latest.canSubmit) session.submitRoadNote(content, category)
    }
}
