package com.jinkim.hyundaiAndGretel.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions

@Preview(name = "수신 · 주간 전체 창", widthDp = 1030, heightDp = 377)
@Preview(name = "수신 · 야간 분할 창", widthDp = 512, heightDp = 377,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReceivedNotePreview() {
    HyundaiAndGretelTheme {
        Surface(Modifier.fillMaxSize(), color = PleosTheme.colors.background) {
            Column(Modifier.padding(horizontal = RoadNoteDimensions.pagePadding)) {
                RoadNoteHeader(stringResource(R.string.road_note_title))
                RoadNoteMessageCard(
                    label = stringResource(R.string.road_note_warning_label),
                    message = stringResource(R.string.road_note_example_warning),
                    modifier = Modifier.padding(top = RoadNoteDimensions.sectionGap),
                    compact = PleosTheme.colors.isDark,
                )
                RoadNoteStatus(stringResource(R.string.road_note_receiving),
                    Modifier.padding(top = if (PleosTheme.colors.isDark) RoadNoteDimensions.compactStatusGap else RoadNoteDimensions.statusGap))
            }
        }
    }
}

@Preview(name = "시작 전", widthDp = 376, heightDp = 292)
@Preview(name = "시작 전 · 야간", widthDp = 376, heightDp = 292,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StateCardPreview() {
    HyundaiAndGretelTheme {
        Surface(color = PleosTheme.colors.background) {
            Column(Modifier.padding(horizontal = RoadNoteDimensions.pagePadding),
                verticalArrangement = Arrangement.spacedBy(PleosSpacing.space04)) {
                RoadNoteHeader(stringResource(R.string.road_note_title))
                RoadNoteStateCard(stringResource(R.string.road_note_intro),
                    stringResource(R.string.road_note_intro_description))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RoadNoteAction(stringResource(R.string.road_note_start), onClick = {})
                }
            }
        }
    }
}

@Preview(name = "기본 버튼 상태", widthDp = 800, heightDp = 480)
@Preview(name = "기본 버튼 상태 · 야간", widthDp = 800, heightDp = 480,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ButtonStatesPreview() {
    HyundaiAndGretelTheme {
        Surface(color = PleosTheme.colors.background) {
            Column(Modifier.padding(PleosSpacing.space05),
                verticalArrangement = Arrangement.spacedBy(PleosSpacing.space05)) {
                PleosButtonSize.entries.forEach { size ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PleosSpacing.space05)) {
                        PleosButton("Label", {}, size = size)
                        PleosButton("Label", {}, size = size, type = PleosButtonType.Secondary)
                        PleosButton("Label", {}, size = size, enabled = false)
                        PleosButton("Label", {}, size = size, loading = true)
                    }
                }
            }
        }
    }
}
