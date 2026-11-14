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

    @Test fun demoOnlyStorageHasItsOwnMessageBeforeReleaseSubmission() {
        assertEquals(R.string.road_note_demo_required, SubmissionState.Idle.statusMessageRes(demoBuild = false))
        assertNull(SubmissionState.Idle.statusMessageRes(demoBuild = true))
        assertEquals(R.string.road_note_demo_required,
            SubmissionState.Failed(SubmissionFailure.DEMO_REQUIRED).messageRes)
    }

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
    fun guidanceControlsDisplayedTextAndStringLimit() {
        val guidance = "공사 아님"
        val guided = item.copy(note = note.copy(content = "가".repeat(500), receiverGuidance = guidance))
        val policy = UiRestrictions(
            connected = true, optimized = true, flags = Flags.UX_RESTRICTIONS_LIMIT_STRING_LENGTH,
            stringLimit = guidance.length,
        )
        val visible = state.copy(inbox = InboxState(listOf(guided)), restrictions = policy).visibleReceipt()
        assertEquals(guidance, visible?.message)
        assertEquals(contentKey, visible?.contentKey)
        assertEquals(key, visible?.key)
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
        val input = layoutInput(requireNotNull(state.visibleReceipt()))
        val page = ReceiptPages(input)
        assertEquals(false, page.canConfirm(input, confirming = false))
        assertEquals(false, page.measured(false).canConfirm(input, confirming = false))
        assertEquals(true, page.measured(true).canConfirm(input, confirming = false))
        assertEquals(false, page.measured(true).canConfirm(input, confirming = true))
    }

    @Test
    fun previousLayoutCannotEnableConfirmationAfterReceiptOrLayoutInputsChange() {
        val input = layoutInput(requireNotNull(state.visibleReceipt()))
        val previous = ReceiptPages(input).measured(true)
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
            assertEquals(false, previous.canConfirm(changed, confirming = false))
            val remeasured = ReceiptPages(changed).measured(true)
            assertEquals(true, remeasured.canConfirm(changed, confirming = false))
        }
    }

    @Test
    fun pagesPreserveOriginalTextAndRequireSequentialMeasuredNavigation() {
        val first = "👩‍🚒e\u0301\r\n\n"
        val middle = "🚙전방"
        val last = "공사"
        val input = layoutInput(requireNotNull(state.visibleReceipt())).copy(
            message = first + middle + last,
            restrictions = UiRestrictions(connected = true),
        )
        val pending = ReceiptPages(input).measured(false, first.length)
        assertEquals(pending, pending.next())
        assertEquals(false, pending.canConfirm(input, confirming = false))

        val unseen = pending.measured(true)
        assertEquals(unseen, unseen.next())
        val pageOne = unseen.copy(bodySeen = true)
        val pageTwo = pageOne.next().measured(false, middle.length).measured(true).copy(bodySeen = true)
        assertEquals(first, pageOne.text)
        assertEquals(middle, pageTwo.text)
        assertEquals(false, pageTwo.canConfirm(input, confirming = false))
        assertEquals(first, pageTwo.previous().text)
        assertEquals(false, pageTwo.previous().canGoNext)

        val unmeasuredLast = pageTwo.next()
        assertEquals(false, unmeasuredLast.canConfirm(input, confirming = false))
        val unseenLast = unmeasuredLast.measured(true)
        assertEquals(false, unseenLast.canConfirm(input, confirming = false))
        val pageThree = unseenLast.copy(bodySeen = true)
        assertEquals(input.message, pageOne.text + pageTwo.text + pageThree.text)
        assertEquals(true, pageThree.canConfirm(input, confirming = false))
        assertEquals(false, pageThree.previous().canConfirm(input, confirming = false))

        val restricted = input.copy(restrictions = UiRestrictions(connected = true, optimized = true))
        assertEquals(false, pageThree.canConfirm(restricted, confirming = false))
        val reset = ReceiptPages(restricted).measured(false, first.length)
        assertEquals(input.message, reset.text)
        assertEquals(reset, reset.next())
        assertEquals(false, reset.canConfirm(restricted, confirming = false))
    }

    @Test
    fun measuredBoundaryNeverSplitsGraphemesOrDiscardsNewlines() {
        val emoji = "👩‍🚒"
        val accent = "e\u0301"
        val breaks = "\r\n\n"
        val content = emoji + accent + breaks + "긴 본문"
        assertNull(fittingReceiptPrefix(content, listOf(1, 2)) { true })
        assertEquals(emoji.length, fittingReceiptPrefix(content, listOf(emoji.length + 1)) { true })
        assertEquals(
            emoji.length + accent.length,
            fittingReceiptPrefix(content, listOf(emoji.length + accent.length + 1)) { true },
        )
        assertEquals(
            emoji.length + accent.length + breaks.length,
            fittingReceiptPrefix(content, listOf(emoji.length + accent.length + breaks.length)) { true },
        )
        assertNull(fittingReceiptPrefix(content, listOf(content.length)) { false })
    }

    @Test
    fun pagingRequiresKnownUnrestrictedPolicyWhileShortNotesKeepExistingRules() {
        val input = layoutInput(requireNotNull(state.visibleReceipt()))
        val policies = listOf(
            UiRestrictions(),
            UiRestrictions(connected = true, optimized = true),
            UiRestrictions(connected = true, flags = Flags.UX_RESTRICTIONS_NO_SETUP),
        )
        for (policy in policies) {
            val current = input.copy(restrictions = policy)
            val overflow = ReceiptPages(current).measured(false, prefixLength = 1)
            assertNull(overflow.end)
            assertEquals(false, overflow.canGoNext)
            assertEquals(false, overflow.canConfirm(current, confirming = false))
            assertEquals(true, ReceiptPages(current).measured(true).canConfirm(current, confirming = false))
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
