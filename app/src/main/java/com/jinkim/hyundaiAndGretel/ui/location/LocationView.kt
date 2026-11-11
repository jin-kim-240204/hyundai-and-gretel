package com.jinkim.hyundaiAndGretel.ui.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jinkim.hyundaiAndGretel.R
import androidx.compose.runtime.Composable
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

@Composable
fun LocationView(
    state: LocationUiState,
    startCommandFailed: Boolean,
    permissionMessage: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onSubmitTestNote: (String, NoteCategory) -> Unit,
) {
    val status = state.trackingStatus
    HyundaiAndGretelTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                val statusMessage = stringResource(when (status) {
                    TrackingStatus.STOPPED -> R.string.location_stopped
                    TrackingStatus.STARTING -> R.string.location_starting
                    TrackingStatus.WAITING -> R.string.location_waiting
                    TrackingStatus.RECEIVING -> R.string.location_receiving
                    TrackingStatus.UNAVAILABLE -> R.string.location_unavailable
                    TrackingStatus.UNSUPPORTED -> R.string.location_unsupported
                    TrackingStatus.FAILED -> R.string.location_failed
                })
                Text(statusMessage)
                if (startCommandFailed) Text(stringResource(R.string.location_command_failed))
                if (permissionMessage.isNotEmpty()) Text(permissionMessage)
                Button(onClick = onStart, enabled = state.canStart,
                    modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.location_start)) }
                Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.location_stopped))
                }
                DebugNoteSubmission(state = state, onSubmit = onSubmitTestNote)
                if (permissionMessage.isNotEmpty()) Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.location_permission_settings)) }
            }
        }
    }
}
