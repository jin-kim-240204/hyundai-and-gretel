package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.SubmissionState
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import kotlinx.coroutines.flow.StateFlow

/** Only the current service's state and submission entry point cross the screen boundary. */
interface LocationSession {
    val uiRestrictions: StateFlow<com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions>
    val status: StateFlow<TrackingStatus>
    val submissionState: StateFlow<SubmissionState>
    val inboxState: StateFlow<InboxState>
    val pickupState: StateFlow<PickupState>
    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean)
    fun submitTestNote(content: String, category: NoteCategory)
}
