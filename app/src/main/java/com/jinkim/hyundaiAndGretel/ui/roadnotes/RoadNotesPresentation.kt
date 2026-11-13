package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteTone
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

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
    val confirming: Boolean,
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
    return VisibleReceipt(key, contentKey, item.note.content, tone, label, inbox.confirming != null)
}

internal data class ReceiptLayoutInput(
    val key: ReceiptKey,
    val message: String,
    val constraints: Constraints,
    val density: Float,
    val fontScale: Float,
    val layoutDirection: LayoutDirection,
    val locales: String,
    val restrictions: UiRestrictions,
)

internal data class ReceiptLayoutMeasurement(
    val input: ReceiptLayoutInput,
    val wholeMessageFits: Boolean,
)

internal enum class ReceiptConfirmation {
    AwaitingLayout, TextOverflow, Ready, Confirming,
}

internal fun VisibleReceipt.confirmation(
    input: ReceiptLayoutInput,
    measured: ReceiptLayoutMeasurement?,
): ReceiptConfirmation = when {
    confirming -> ReceiptConfirmation.Confirming
    measured == null || measured.input != input -> ReceiptConfirmation.AwaitingLayout
    !measured.wholeMessageFits -> ReceiptConfirmation.TextOverflow
    else -> ReceiptConfirmation.Ready
}
