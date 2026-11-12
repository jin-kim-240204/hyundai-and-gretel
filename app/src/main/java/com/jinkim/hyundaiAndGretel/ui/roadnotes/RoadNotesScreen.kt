package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PolicyText(stringResource(R.string.app_name), policy, style = MaterialTheme.typography.headlineLarge)
            TrackingControls(state, startAllowed, onStartTracking, onStopTracking)
            SubmissionStatus(state.submission, policy)
            if (state.pickup is PickupState.Failed) PolicyText(stringResource(R.string.pickup_failed), policy)
            if (state.inbox.error) PolicyText(stringResource(R.string.receipt_failed), policy)
            state.inbox.pending?.let { item ->
                ReceivedNoteCard(item, state, onConfirmReceipt, onContentShown)
            }
        }
    }
}

@Composable
private fun TrackingControls(state: RoadNotesUiState, startAllowed: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val policy = state.restrictions
    val message = when (state.trackingStatus) {
        TrackingStatus.STOPPED -> R.string.location_stopped
        TrackingStatus.STARTING -> R.string.location_starting
        TrackingStatus.WAITING -> R.string.location_waiting
        TrackingStatus.RECEIVING -> R.string.location_receiving
        TrackingStatus.UNAVAILABLE -> R.string.location_unavailable
        TrackingStatus.UNSUPPORTED -> R.string.location_unsupported
        TrackingStatus.FAILED -> R.string.location_failed
    }
    PolicyText(stringResource(message), policy)
    if (state.startCommandFailed) PolicyText(stringResource(R.string.location_command_failed), policy)
    Button(onClick = onStart, enabled = state.canStart && startAllowed, modifier = Modifier.heightIn(min = 56.dp)) {
        PolicyText(stringResource(R.string.location_start), policy)
    }
    Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) {
        PolicyText(stringResource(R.string.location_stop), policy)
    }
}

@Composable
private fun SubmissionStatus(submission: SubmissionState?, policy: UiRestrictions) {
    val message = when (submission) {
        is SubmissionState.Saved -> "도로 쪽지를 남겼습니다"
        SubmissionState.Submitting -> "제보를 저장하고 있습니다"
        is SubmissionState.Failed -> "제보를 저장하지 못했습니다"
        else -> return
    }
    PolicyText(message, policy)
}

@Composable
private fun ReceivedNoteCard(item: InboxItem, state: RoadNotesUiState, onConfirm: (ReceiptKey) -> Unit, onContentShown: (String) -> Unit) {
    val policy = state.restrictions
    val contentKey = "receipt:${item.recipientId}/${item.note.noteId}"
    if (!policy.canShowBody || !policy.fits(item.note.content) || !policy.allowsContent(contentKey, state.visitedContent)) {
        PolicyText("수신한 쪽지가 있습니다 · 현재 표시 제한", policy)
        return
    }
    SideEffect { onContentShown(contentKey) }
    PolicyText(stringResource(R.string.receipt_title), policy)
    PolicyText(item.note.content, policy)
    Button(
        onClick = { onConfirm(ReceiptKey(item.note.noteId, item.recipientId)) },
        enabled = state.inbox.confirming == null,
        modifier = Modifier.heightIn(min = 56.dp),
    ) {
        PolicyText(stringResource(if (state.inbox.confirming == null) R.string.receipt_confirm else R.string.receipt_confirming), policy)
    }
}
