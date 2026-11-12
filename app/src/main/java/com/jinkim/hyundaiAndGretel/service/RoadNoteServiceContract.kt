package com.jinkim.hyundaiAndGretel.service

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import kotlinx.coroutines.flow.StateFlow

interface RoadNoteServiceContract {
    val canStartTracking: Boolean get() = true
    val uiRestrictions: StateFlow<com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions>
    val status: StateFlow<TrackingStatus>
    val submissionState: StateFlow<SubmissionState>
    val inboxState: StateFlow<InboxState>
    val pickupState: StateFlow<PickupState>
    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>)
    fun submitRoadNote(content: String, category: NoteCategory)
}
