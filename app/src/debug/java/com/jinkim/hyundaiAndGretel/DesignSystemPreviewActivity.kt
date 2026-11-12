package com.jinkim.hyundaiAndGretel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jinkim.hyundaiAndGretel.ui.components.ReceivedNotePreview
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteAction
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteHeader
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStateCard
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions

class DesignSystemPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showPermission = intent.getStringExtra("screen") == "permission"
        setContent {
            if (showPermission) PermissionRequiredPreview() else ReceivedNotePreview()
        }
    }
}

@Composable
private fun PermissionRequiredPreview() {
    HyundaiAndGretelTheme {
        Surface(Modifier.fillMaxSize(), color = PleosTheme.colors.background) {
            Column(Modifier.padding(horizontal = RoadNoteDimensions.pagePadding),
                verticalArrangement = Arrangement.spacedBy(PleosSpacing.space04)) {
                RoadNoteHeader(stringResource(R.string.road_note_title))
                RoadNoteStateCard(stringResource(R.string.preview_location_permission_title),
                    stringResource(R.string.preview_location_permission_description))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RoadNoteAction(stringResource(R.string.preview_open_settings), onClick = {})
                }
            }
        }
    }
}
