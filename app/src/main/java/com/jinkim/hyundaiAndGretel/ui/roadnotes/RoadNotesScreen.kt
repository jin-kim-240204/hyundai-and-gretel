package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

@Composable
fun RoadNotesScreen(
    state: RoadNotesUiState,
    startCommandFailed: Boolean,
    permissionMessage: String,
    onStart: () -> Unit,
    startAllowed: Boolean = true,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmReceipt: (ReceiptKey) -> Unit,
    onContentShown: (String) -> Unit,
    debugTools: @Composable () -> Unit = {},
) {
    val status = state.trackingStatus
    val policy = state.restrictions
    HyundaiAndGretelTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally) {
                PolicyText(stringResource(R.string.app_name), policy, style = MaterialTheme.typography.headlineLarge)
                // 리뷰용: 제품 화면은 전달받은 상태로 문구와 버튼을 그린다. 버튼 콜백을 통해서만 외부 작업을 요청한다.
                val statusMessage = stringResource(when (status) {
                    TrackingStatus.STOPPED -> R.string.location_stopped
                    TrackingStatus.STARTING -> R.string.location_starting
                    TrackingStatus.WAITING -> R.string.location_waiting
                    TrackingStatus.RECEIVING -> R.string.location_receiving
                    TrackingStatus.UNAVAILABLE -> R.string.location_unavailable
                    TrackingStatus.UNSUPPORTED -> R.string.location_unsupported
                    TrackingStatus.FAILED -> R.string.location_failed
                })
                PolicyText(statusMessage, policy)
                if (startCommandFailed) PolicyText(stringResource(R.string.location_command_failed), policy)
                if (permissionMessage.isNotEmpty()) PolicyText(permissionMessage, policy)
                Button(onClick = onStart, enabled = state.canStart && startAllowed,
                    modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.location_start), policy) }
                Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) {
                    PolicyText(stringResource(R.string.location_stop), policy)
                }
                if (state.pickup is PickupState.Failed) PolicyText(stringResource(R.string.pickup_failed), policy)
                if (state.inbox.error) PolicyText(stringResource(R.string.receipt_failed), policy)
                when (state.submission) {
                    is SubmissionState.Saved -> PolicyText("도로 쪽지를 남겼습니다", policy)
                    SubmissionState.Submitting -> PolicyText("제보를 저장하고 있습니다", policy)
                    is SubmissionState.Failed -> PolicyText("제보를 저장하지 못했습니다", policy)
                    else -> Unit
                }
                // 리뷰용: 제품 수신함의 현재 한 건을 표시한다. 확인 버튼만 저장을 요청하며, 화면 구성 자체로 확인 처리하지 않는다.
                state.inbox.pending?.let { item ->
                    val id = "receipt:${item.recipientId}/${item.note.noteId}"
                    if (policy.canShowBody && policy.fits(item.note.content) && policy.allowsContent(id, state.visitedContent)) {
                        SideEffect { onContentShown(id) } // 화면 표시 항목 수만 기록한다. 쪽지 확인 처리는 하지 않는다.
                        PolicyText(stringResource(R.string.receipt_title), policy)
                        PolicyText(item.note.content, policy)
                        Button(onClick = { onConfirmReceipt(ReceiptKey(item.note.noteId, item.recipientId)) },
                            enabled = state.inbox.confirming == null, modifier = Modifier.heightIn(min = 56.dp)) {
                            PolicyText(stringResource(if (state.inbox.confirming == null) R.string.receipt_confirm else R.string.receipt_confirming), policy)
                        }
                    } else PolicyText("수신한 쪽지가 있습니다 · 현재 표시 제한", policy)
                }
                if (permissionMessage.isNotEmpty()) Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.location_permission_settings), policy) }
            }
            // 리뷰용: 시연 패널은 제품 UI와 분리된 빌드별 슬롯이다. 릴리스 구현에서는 시연 도구를 표시하지 않는다.
            debugTools()
            }
        }
    }
}
