package com.jinkim.hyundaiAndGretel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RoadNotesRoute(window = window) }
    }
}
