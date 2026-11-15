package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.demo.demoPanelBottomPadding
import com.jinkim.hyundaiAndGretel.notes.pickup.PickupState
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteAction
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteHeader
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStateCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStatus
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions

@Composable
internal fun RoadNotesScreen(
    state: RoadNotesUiState,
    trackingPreparation: String?,
    onConfirmReceipt: (ReceiptKey) -> Unit,
    isReceiptCurrent: (ReceiptLayoutInput) -> Boolean,
    onContentShown: (String) -> Unit,
) {
    val policy = state.restrictions
    val receipt = state.visibleReceipt()
    val trackingMessage = policy.label(stringResource(state.trackingStatus.messageRes))
    Surface(Modifier.fillMaxSize(), color = PleosTheme.colors.background) {
        BoxWithConstraints {
            val compact = maxWidth <= 512.dp
            val windowConstraints = constraints
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = RoadNoteDimensions.pagePadding, vertical = PleosSpacing.space04)
                    .padding(bottom = demoPanelBottomPadding),
                verticalArrangement = Arrangement.spacedBy(PleosSpacing.space04),
            ) {
                RoadNoteHeader(policy.label(stringResource(R.string.road_note_title)))
                if (receipt != null) {
                    ReceiptCard(
                        receipt = receipt,
                        policy = policy,
                        constraints = windowConstraints,
                        compact = compact,
                        trackingMessage = trackingMessage,
                        isReceiptCurrent = isReceiptCurrent,
                        onConfirm = onConfirmReceipt,
                        onContentShown = onContentShown,
                    )
                } else {
                    RoadNoteStateCard(
                        title = policy.label(stringResource(R.string.road_note_intro)),
                        description = trackingMessage,
                    )
                    if (state.inbox.pending != null) {
                        RoadNoteStatus(policy.label(stringResource(R.string.receipt_restricted)))
                    }
                }

                if (state.startCommandFailed) RoadNoteStatus(policy.label(stringResource(R.string.location_command_failed)))
                state.submission.statusMessageRes(demoBuild = true)?.let { message ->
                    RoadNoteStatus(
                        policy.label(stringResource(message)),
                        loading = state.submission == SubmissionState.Submitting && !policy.optimized,
                    )
                }
                if (state.pickup is PickupState.Failed) RoadNoteStatus(policy.label(stringResource(R.string.pickup_failed)))
                if (state.inbox.error) RoadNoteStatus(policy.label(stringResource(R.string.receipt_failed)))
                if (!state.trackingStatus.isCollecting && trackingPreparation != null) {
                    RoadNoteStatus(policy.label(trackingPreparation))
                }
            }
        }
    }
}
