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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.notes.pickup.PickupState
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteAction
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteHeader
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteMessageCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStateCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStatus
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions

@Composable
fun RoadNotesScreen(
    state: RoadNotesUiState,
    startAllowed: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onConfirmReceipt: (ReceiptKey) -> Unit,
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
                    .padding(horizontal = RoadNoteDimensions.pagePadding, vertical = PleosSpacing.space04),
                verticalArrangement = Arrangement.spacedBy(PleosSpacing.space04),
            ) {
                RoadNoteHeader(policy.label(stringResource(R.string.road_note_title)))
                if (receipt != null) {
                    SideEffect { onContentShown(receipt.contentKey) }
                    val density = LocalDensity.current
                    val input = ReceiptLayoutInput(
                        key = receipt.key,
                        message = receipt.message,
                        constraints = windowConstraints,
                        density = density.density,
                        fontScale = density.fontScale,
                        layoutDirection = LocalLayoutDirection.current,
                        locales = LocalConfiguration.current.locales.toLanguageTags(),
                        restrictions = policy,
                    )
                    var measured by remember(input) { mutableStateOf<ReceiptLayoutMeasurement?>(null) }
                    // Recreate the text so even identical glyph layout reports a result for the new input.
                    key(input) {
                        RoadNoteMessageCard(
                            label = policy.label(stringResource(receipt.labelRes)),
                            message = receipt.message,
                            modifier = Modifier.padding(top = RoadNoteDimensions.sectionGap - PleosSpacing.space04),
                            tone = receipt.tone,
                            compact = compact,
                            maxMessageLines = 3,
                            onMessageLayout = { layout ->
                                val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
                                measured = ReceiptLayoutMeasurement(
                                    input,
                                    wholeMessageFits = layout.lineCount > 0 && !layout.hasVisualOverflow && !ellipsized,
                                )
                            },
                        )
                    }
                    RoadNoteStatus(
                        trackingMessage,
                        Modifier.padding(top = (if (compact) RoadNoteDimensions.compactStatusGap
                            else RoadNoteDimensions.statusGap) - PleosSpacing.space04),
                    )
                    RoadNoteStatus(policy.label(stringResource(R.string.receipt_title)))
                    val confirmation = receipt.confirmation(input, measured)
                    val notice = when (confirmation) {
                        ReceiptConfirmation.AwaitingLayout -> R.string.receipt_measuring
                        ReceiptConfirmation.TextOverflow -> R.string.receipt_overflow
                        ReceiptConfirmation.Ready, ReceiptConfirmation.Confirming -> null
                    }
                    if (notice != null) RoadNoteStatus(policy.label(stringResource(notice)))
                    RoadNoteAction(
                        text = policy.label(stringResource(
                            if (receipt.confirming) R.string.receipt_confirming else R.string.receipt_confirm,
                        )),
                        onClick = {
                            if (receipt.confirmation(input, measured) == ReceiptConfirmation.Ready) {
                                onConfirmReceipt(receipt.key)
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = confirmation == ReceiptConfirmation.Ready,
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
                state.submission.messageRes?.let { message ->
                    RoadNoteStatus(
                        policy.label(stringResource(message)),
                        loading = state.submission == SubmissionState.Submitting && !policy.optimized,
                    )
                }
                if (state.pickup is PickupState.Failed) RoadNoteStatus(policy.label(stringResource(R.string.pickup_failed)))
                if (state.inbox.error) RoadNoteStatus(policy.label(stringResource(R.string.receipt_failed)))
                RoadNoteAction(
                    text = policy.label(stringResource(R.string.location_start)),
                    onClick = onStartTracking,
                    modifier = Modifier.align(Alignment.End),
                    enabled = state.canStart && startAllowed,
                )
                RoadNoteAction(
                    text = policy.label(stringResource(R.string.location_stop)),
                    onClick = onStopTracking,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
