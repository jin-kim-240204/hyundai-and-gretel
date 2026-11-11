package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.SubmissionState
import kotlinx.coroutines.flow.StateFlow

/** Only the current service's state and submission entry point cross the screen boundary. */
interface LocationSession {
    val status: StateFlow<TrackingStatus>
    val submissionState: StateFlow<SubmissionState>
    fun submitTestNote(content: String, category: NoteCategory)
}

