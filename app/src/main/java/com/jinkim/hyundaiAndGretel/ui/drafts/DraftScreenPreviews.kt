package com.jinkim.hyundaiAndGretel.ui.drafts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import java.time.Instant

@Preview(name = "미동의 · 좁은 창", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentPreview() {
    HyundaiAndGretelTheme(dynamicColor = false) {
        DraftScreen(false, emptyList(), false, null, {}, {}, {})
    }
}

@Preview(name = "초안 · 주간 가로", widthDp = 1280, heightDp = 720)
@Preview(name = "초안 · 좁은 창", widthDp = 360, heightDp = 800)
@Preview(name = "초안 · 큰 글꼴", widthDp = 480, heightDp = 800, fontScale = 1.5f)
@Composable
private fun DraftListPreview() {
    HyundaiAndGretelTheme(darkTheme = false, dynamicColor = false) {
        DraftScreen(true, previewDrafts, false, null, {}, {}, {})
    }
}

@Preview(name = "초안 · 야간 세로", widthDp = 600, heightDp = 900)
@Composable
private fun NightDraftsPreview() {
    HyundaiAndGretelTheme(darkTheme = true, dynamicColor = false) {
        DraftScreen(true, previewDrafts, false, null, {}, {}, {})
    }
}

@Preview(name = "빈 목록", widthDp = 600, heightDp = 800)
@Composable
private fun EmptyDraftsPreview() {
    HyundaiAndGretelTheme(dynamicColor = false) {
        DraftScreen(true, emptyList(), false, null, {}, {}, {})
    }
}

@Preview(name = "처리 중 · 오류", widthDp = 480, heightDp = 800)
@Composable
private fun BusyErrorPreview() {
    HyundaiAndGretelTheme(dynamicColor = false) {
        DraftScreen(true, previewDrafts, true, "초안을 불러오지 못했습니다. 다시 시도해 주세요.", {}, {}, {})
    }
}

private val previewDrafts = LocationMode.entries.mapIndexed { index, mode ->
    RoadNoteDraft(
        id = "preview-$index",
        input = RoadNoteInput(
            content = "오른쪽 차로에 떨어진 물체가 있어요. 뒤따르는 차량은 안전거리를 확보해 주세요. ".repeat(6),
            locationMode = mode,
            placeQuery = if (mode == LocationMode.NAMED_PLACE) "아주 긴 지명과 교차로 이름 ".repeat(12) else null,
            directionMode = if (mode == LocationMode.CURRENT_ROAD) DirectionMode.CURRENT_VEHICLE else DirectionMode.USER_DESCRIPTION,
            directionDescription = "서울에서 부산으로 향하는 방향 ".repeat(8),
        ),
        receivedAt = Instant.parse("2026-11-08T09:00:00Z"),
        updatedAt = Instant.parse("2026-11-08T09:00:00Z"),
        state = if (mode == LocationMode.CURRENT_ROAD || mode == LocationMode.NAMED_PLACE) {
            DraftState.PENDING_LOCATION
        } else {
            DraftState.AWAITING_LOCATION
        },
    )
}
