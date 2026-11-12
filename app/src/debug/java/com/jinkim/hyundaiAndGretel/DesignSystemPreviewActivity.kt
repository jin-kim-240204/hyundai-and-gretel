package com.jinkim.hyundaiAndGretel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jinkim.hyundaiAndGretel.ui.components.ReceivedNotePreview

class DesignSystemPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReceivedNotePreview() }
    }
}
