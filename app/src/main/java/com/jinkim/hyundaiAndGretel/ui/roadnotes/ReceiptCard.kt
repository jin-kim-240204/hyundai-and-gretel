package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteAction
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteMessageCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStatus
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

@Composable
internal fun ColumnScope.ReceiptCard(
    receipt: VisibleReceipt,
    policy: UiRestrictions,
    constraints: Constraints,
    compact: Boolean,
    trackingMessage: String,
    isReceiptCurrent: (ReceiptLayoutInput) -> Boolean,
    onConfirm: (ReceiptKey) -> Unit,
    onContentShown: (String) -> Unit,
) {
    val density = LocalDensity.current
    val input = ReceiptLayoutInput(
        key = receipt.key,
        message = receipt.message,
        constraints = constraints,
        density = density.density,
        fontScale = density.fontScale,
        layoutDirection = LocalLayoutDirection.current,
        locales = LocalConfiguration.current.locales.toLanguageTags(),
        restrictions = policy,
    )
    val measurer = rememberTextMeasurer()
    val session = remember(input, measurer) { mutableStateOf(ReceiptPages(input)) }
    val page = session.value
    SideEffect { onContentShown(receipt.contentKey) }
    // Recreate Text when the measured range changes, including repeated identical pages.
    val messageModifier = Modifier.padding(top = RoadNoteDimensions.sectionGap - PleosSpacing.space04)
    key(session, page.start, page.end) {
        RoadNoteMessageCard(
            label = policy.label(stringResource(receipt.labelRes)),
            message = page.text,
            modifier = if (page.fits == true) messageModifier else messageModifier.clearAndSetSemantics {},
            tone = receipt.tone,
            compact = compact,
            maxMessageLines = RECEIPT_LINES,
            onMessageLayout = { layout ->
                val current = session.value
                if (current.start == page.start && current.end == page.end) {
                    val fits = layout.fitsReceiptPage() &&
                        (!page.isPaged || layout.size.height <= constraints.maxHeight)
                    val prefix = if (!fits && page.end == null && policy.allowsReceiptPaging) {
                        fun measure(text: String) = measurer.measure(
                            text = text,
                            style = layout.layoutInput.style,
                            overflow = TextOverflow.Clip,
                            maxLines = RECEIPT_LINES,
                            constraints = layout.layoutInput.constraints,
                        )
                        val probe = measure(page.text)
                        val lineEnds = List(probe.lineCount) { probe.getLineEnd(it, visibleEnd = false) }
                        fittingReceiptPrefix(page.text, lineEnds) {
                            val candidate = measure(it)
                            candidate.fitsReceiptPage() && candidate.size.height <= constraints.maxHeight
                        }
                    } else null
                    session.value = current.measured(fits, prefix)
                }
            },
            messageModifier = Modifier.onGloballyPositioned { coordinates ->
                val current = session.value
                if (current.start == page.start && current.end == page.end &&
                    current.isPaged && !current.bodySeen
                ) {
                    val visible = coordinates.boundsInWindow()
                    if (!visible.isEmpty && abs(visible.width - coordinates.size.width) < 0.5f &&
                        abs(visible.height - coordinates.size.height) < 0.5f
                    ) {
                        session.value = current.copy(bodySeen = true)
                    }
                }
            },
        )
    }
    RoadNoteStatus(
        trackingMessage,
        Modifier.padding(top = (if (compact) RoadNoteDimensions.compactStatusGap
            else RoadNoteDimensions.statusGap) - PleosSpacing.space04),
    )
    RoadNoteStatus(policy.label(stringResource(R.string.receipt_title)))
    val notice = when {
        page.fits == null -> R.string.receipt_measuring
        page.fits == false && !policy.allowsReceiptPaging -> R.string.receipt_paging_restricted
        page.fits == false -> R.string.receipt_overflow
        page.isPaged && !page.bodySeen -> R.string.receipt_show_page
        else -> null
    }
    if (notice != null) RoadNoteStatus(policy.label(stringResource(notice)))

    val actionModifier = Modifier.align(Alignment.End)
    if (page.hasPrevious) {
        RoadNoteAction(stringResource(R.string.receipt_previous), onClick = {
            if (session.value == page && isReceiptCurrent(input)) session.value = page.previous()
        }, modifier = actionModifier, enabled = !receipt.confirming)
    }
    if (page.hasNextPage) {
        RoadNoteAction(stringResource(R.string.receipt_next), onClick = {
            if (session.value == page && isReceiptCurrent(input)) session.value = page.next()
        }, modifier = actionModifier, enabled = page.canGoNext && !receipt.confirming)
    }
    RoadNoteAction(
        text = policy.label(stringResource(if (receipt.confirming) R.string.receipt_confirming else R.string.receipt_confirm)),
        onClick = {
            if (session.value == page && page.canConfirm(input, receipt.confirming) && isReceiptCurrent(input)) {
                onConfirm(receipt.key)
            }
        },
        modifier = actionModifier,
        enabled = page.canConfirm(input, receipt.confirming),
    )
}

private fun TextLayoutResult.fitsReceiptPage(): Boolean =
    lineCount in 1..RECEIPT_LINES && !hasVisualOverflow && (0 until lineCount).none(::isLineEllipsized)
