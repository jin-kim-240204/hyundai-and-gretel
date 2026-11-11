package com.jinkim.hyundaiAndGretel.ui.location

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.SubmissionState

@Composable
internal fun DebugNoteSubmission(state: LocationUiState, onSubmit: (String, NoteCategory) -> Unit) {
    val content = stringResource(R.string.debug_note_fixture)
    val message = when (state.submission.debugMessage()) {
        DebugSubmissionMessage.CONNECTING -> R.string.debug_note_connecting
        DebugSubmissionMessage.READY -> R.string.debug_note_ready
        DebugSubmissionMessage.SAVING -> R.string.debug_note_saving
        DebugSubmissionMessage.SAVED -> R.string.debug_note_saved
        DebugSubmissionMessage.LOCATION_UNAVAILABLE -> R.string.debug_note_no_location
        DebugSubmissionMessage.INVALID_TIME -> R.string.debug_note_invalid_time
        DebugSubmissionMessage.INVALID_INPUT -> R.string.debug_note_invalid_input
        DebugSubmissionMessage.DUPLICATE -> R.string.debug_note_duplicate
        DebugSubmissionMessage.STORAGE -> R.string.debug_note_storage_failed
        DebugSubmissionMessage.CANCELLED -> R.string.debug_note_cancelled
    }
    Text(stringResource(message))
    Button(
        onClick = { onSubmit(content, NoteCategory.WARNING) },
        enabled = state.canSubmit,
        modifier = Modifier.heightIn(min = 56.dp),
    ) {
        if (state.submission == SubmissionState.Submitting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Text(stringResource(R.string.debug_note_submit))
        }
    }
}
