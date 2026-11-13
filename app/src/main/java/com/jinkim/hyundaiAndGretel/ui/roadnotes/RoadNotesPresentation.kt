package com.jinkim.hyundaiAndGretel.ui.roadnotes

import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteTone

internal val TrackingStatus.messageRes: Int
    get() = when (this) {
        TrackingStatus.STOPPED -> R.string.location_stopped
        TrackingStatus.STARTING -> R.string.location_starting
        TrackingStatus.WAITING -> R.string.location_waiting
        TrackingStatus.RECEIVING -> R.string.location_receiving
        TrackingStatus.UNAVAILABLE -> R.string.location_unavailable
        TrackingStatus.UNSUPPORTED -> R.string.location_unsupported
        TrackingStatus.FAILED -> R.string.location_failed
    }

internal val SubmissionState?.messageRes: Int?
    get() = when (this) {
        is SubmissionState.Saved -> R.string.road_note_saved
        SubmissionState.Submitting -> R.string.road_note_saving
        is SubmissionState.Failed -> R.string.road_note_save_failed
        SubmissionState.Idle, null -> null
    }

internal data class VisibleReceipt(
    val key: ReceiptKey,
    val contentKey: String,
    val message: String,
    val tone: RoadNoteTone,
    val labelRes: Int,
    val canConfirm: Boolean,
)

internal fun RoadNotesUiState.visibleReceipt(): VisibleReceipt? {
    val item = inbox.pending ?: return null
    val key = ReceiptKey(item.note.noteId, item.recipientId)
    val contentKey = "receipt:${key.recipientId}/${key.noteId}"
    if (!restrictions.canShowBody || !restrictions.fits(item.note.content) ||
        !restrictions.allowsContent(contentKey, visitedContent)
    ) return null

    val (tone, label) = when (item.note.category) {
        NoteCategory.WARNING -> RoadNoteTone.Warning to R.string.road_note_warning_label
        NoteCategory.INFORMATION -> RoadNoteTone.Information to R.string.road_note_information_label
    }
    return VisibleReceipt(key, contentKey, item.note.content, tone, label, inbox.confirming == null)
}
