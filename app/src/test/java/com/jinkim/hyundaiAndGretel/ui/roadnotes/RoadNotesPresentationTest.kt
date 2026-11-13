package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.car.drivingstate.CarUxRestrictions as Flags
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.pickup.InboxItem
import com.jinkim.hyundaiAndGretel.notes.pickup.InboxState
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteTone
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoadNotesPresentationTest {
    private val note = RoadNote("note", "author", "전방 공사", NoteCategory.WARNING, 0.0, 0.0, 0L, 0L, 600_000L)
    private val item = InboxItem(note, "recipient", 1L, 10.0, displayedAtUtcMillis = null)
    private val key = ReceiptKey("note", "recipient")
    private val contentKey = "receipt:recipient/note"
    private val state = RoadNotesUiState(inbox = InboxState(listOf(item)))

    @Test
    fun trackingDistinguishesWaitingReceivingAndErrors() {
        val expected = mapOf(
            TrackingStatus.STOPPED to R.string.location_stopped,
            TrackingStatus.STARTING to R.string.location_starting,
            TrackingStatus.WAITING to R.string.location_waiting,
            TrackingStatus.RECEIVING to R.string.location_receiving,
            TrackingStatus.UNAVAILABLE to R.string.location_unavailable,
            TrackingStatus.UNSUPPORTED to R.string.location_unsupported,
            TrackingStatus.FAILED to R.string.location_failed,
        )
        assertEquals(expected, TrackingStatus.entries.associateWith { it.messageRes })
    }

    @Test
    fun submissionDistinguishesSavingSavedAndFailure() {
        assertEquals(R.string.road_note_saving, SubmissionState.Submitting.messageRes)
        assertEquals(R.string.road_note_saved, SubmissionState.Saved(note).messageRes)
        assertEquals(R.string.road_note_save_failed, SubmissionState.Failed(SubmissionFailure.STORAGE).messageRes)
        assertNull(SubmissionState.Idle.messageRes)
        assertNull(state.submission.messageRes)
    }

    @Test
    fun pendingReceiptKeepsItsIdentityAndCategoryWhileConfirmationDisablesAction() {
        assertEquals(
            VisibleReceipt(key, contentKey, note.content, RoadNoteTone.Warning, R.string.road_note_warning_label, false),
            state.visibleReceipt(),
        )
        val info = item.copy(note = note.copy(category = NoteCategory.INFORMATION))
        val confirming = state.copy(inbox = InboxState(listOf(info), confirming = key))
        assertEquals(
            VisibleReceipt(key, contentKey, note.content, RoadNoteTone.Information, R.string.road_note_information_label, true),
            confirming.visibleReceipt(),
        )
        assertNull(state.copy(inbox = InboxState(listOf(item.copy(displayedAtUtcMillis = 2L)))).visibleReceipt())
    }

    @Test
    fun restrictedReceiptHasNoBodyOrConfirmationAction() {
        val policy = UiRestrictions(connected = true, optimized = true)
        assertNull(state.copy(restrictions = policy.copy(flags = Flags.UX_RESTRICTIONS_NO_TEXT_MESSAGE)).visibleReceipt())
        assertNull(state.copy(restrictions = policy.copy(
            flags = Flags.UX_RESTRICTIONS_LIMIT_STRING_LENGTH, stringLimit = 2,
        )).visibleReceipt())

        val limited = state.copy(
            restrictions = policy.copy(flags = Flags.UX_RESTRICTIONS_LIMIT_CONTENT, itemLimit = 1),
            visitedContent = setOf("other"),
        )
        assertNull(limited.visibleReceipt())
        assertEquals(state.visibleReceipt(), limited.copy(visitedContent = setOf(contentKey)).visibleReceipt())
        assertNull(limited.copy(
            restrictions = limited.restrictions.copy(depthLimit = 0), visitedContent = setOf(contentKey),
        ).visibleReceipt())
    }

    @Test
    fun confirmationRequiresCurrentLayoutToFitAndNoConfirmationInProgress() {
        val receipt = requireNotNull(state.visibleReceipt())
        val input = layoutInput(receipt)
        val fits = ReceiptLayoutMeasurement(input, wholeMessageFits = true)

        assertEquals(ReceiptConfirmation.AwaitingLayout, receipt.confirmation(input, null))
        assertEquals(ReceiptConfirmation.Ready, receipt.confirmation(input, fits))
        assertEquals(
            ReceiptConfirmation.TextOverflow,
            receipt.confirmation(input, fits.copy(wholeMessageFits = false)),
        )
        assertEquals(
            ReceiptConfirmation.Confirming,
            receipt.copy(confirming = true).confirmation(input, fits),
        )
    }

    @Test
    fun previousLayoutCannotEnableConfirmationAfterReceiptOrLayoutInputsChange() {
        val receipt = requireNotNull(state.visibleReceipt())
        val input = layoutInput(receipt)
        val previous = ReceiptLayoutMeasurement(input, wholeMessageFits = true)

        val changedInputs = listOf(
            input.copy(key = key.copy(noteId = "new")),
            input.copy(key = key.copy(recipientId = "another")),
            input.copy(message = "전방 공사로 우회"),
            input.copy(constraints = Constraints(maxWidth = 480)),
            input.copy(density = 1.5f),
            input.copy(fontScale = 1.25f),
            input.copy(layoutDirection = LayoutDirection.Rtl),
            input.copy(locales = "ja-JP"),
            input.copy(restrictions = UiRestrictions(connected = true, optimized = true)),
        )
        for (changed in changedInputs) {
            val current = receipt.copy(key = changed.key, message = changed.message)
            assertEquals(ReceiptConfirmation.AwaitingLayout, current.confirmation(changed, previous))
            val remeasured = ReceiptLayoutMeasurement(changed, wholeMessageFits = true)
            assertEquals(ReceiptConfirmation.Ready, current.confirmation(changed, remeasured))
        }
    }

    private fun layoutInput(receipt: VisibleReceipt) = ReceiptLayoutInput(
        key = receipt.key,
        message = receipt.message,
        constraints = Constraints(maxWidth = 1024),
        density = 1f,
        fontScale = 1f,
        layoutDirection = LayoutDirection.Ltr,
        locales = "ko-KR",
        restrictions = state.restrictions,
    )
}
