package com.jinkim.hyundaiAndGretel.ui.drafts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DraftScreen(
    consentGranted: Boolean,
    drafts: List<RoadNoteDraft>,
    busy: Boolean,
    errorMessage: String?,
    onConsentChange: (Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "도로 제보 초안 · 미게시",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    DraftText("아직 게시·공유하지 않았습니다.")
                    DraftText("위치는 다음 단계에서 지정합니다.")
                    DraftText("안전하게 정차한 뒤 확인해 주세요.")
                }
            }
            if (busy) {
                item {
                    DraftText("처리 중입니다. 잠시 기다려 주세요.", announce = true)
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                item { DraftText(errorMessage, announce = true) }
            }
            if (!consentGranted) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DraftText("초안을 나중에 확인하도록 저장합니다.")
                        DraftText("제보 내용·장소/방향 표현·시각을 저장합니다.")
                        DraftText("초안은 최초 수신부터 7일간 유효합니다.")
                        DraftText("만료된 초안은 앱 이용 시 삭제합니다.")
                        DraftText("거부하면 초안을 저장하지 않습니다.")
                        DraftText("철회하면 모든 초안을 삭제합니다.")
                        DraftAction("동의하고 저장 허용", busy) { onConsentChange(true) }
                        DraftAction("동의하지 않음", busy) { onConsentChange(false) }
                        DraftAction("새로고침", busy, onRefresh)
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DraftText("최초 수신부터 7일 유효 · ${drafts.size}개")
                        DraftAction("새로고침", busy, onRefresh)
                        DraftText("동의를 철회하면 모든 초안이 삭제됩니다.")
                        DraftAction("동의 철회 및 전체 삭제", busy) { onConsentChange(false) }
                    }
                }
                if (drafts.isEmpty() && !busy) {
                    item { DraftText("저장된 초안이 없습니다.") }
                }
                items(drafts, key = { it.id }) { draft ->
                    DraftItem(draft, busy) { onDelete(draft.id) }
                }
            }
        }
    }
}

@Composable
private fun DraftItem(draft: RoadNoteDraft, busy: Boolean, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DraftText(draft.locationStatus())
            DraftText(draft.input.content)
            DraftText(draft.locationDescription())
            DraftText(draft.directionDescription())
            DraftText("앱 수신: ${receivedTimeFormat.withZone(ZoneId.systemDefault()).format(draft.receivedAt)}")
            DraftAction("이 초안 삭제", busy, onDelete)
        }
    }
}

@Composable
private fun DraftText(text: String, announce: Boolean = false) {
    Text(
        text = text,
        modifier = if (announce) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DraftAction(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(label, fontSize = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

internal fun RoadNoteDraft.locationStatus(): String = when (state) {
    DraftState.PENDING_LOCATION -> "미게시 · 위치 확인 대기"
    DraftState.AWAITING_LOCATION -> "미게시 · 위치 미정"
}

internal fun RoadNoteDraft.locationDescription(): String = when (input.locationMode) {
    LocationMode.CURRENT_ROAD -> "위치 표현: 현재 도로 (위치 미확정)"
    LocationMode.NAMED_PLACE -> "장소 표현: ${input.placeQuery?.takeIf { it.isNotBlank() } ?: "미정"}"
    LocationMode.NEEDS_CLARIFICATION -> "위치 표현: 확인 필요"
    LocationMode.DEFERRED -> "위치 표현: 나중에 지정"
}

internal fun RoadNoteDraft.directionDescription(): String = when (input.directionMode) {
    DirectionMode.CURRENT_VEHICLE -> "방향 표현: 현재 차량 방향 (방향 미확정)"
    DirectionMode.USER_DESCRIPTION -> "방향 표현: ${input.directionDescription?.takeIf { it.isNotBlank() } ?: "미정"}"
    DirectionMode.UNKNOWN -> "방향: 미정"
}

private val receivedTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX", Locale.KOREAN)
