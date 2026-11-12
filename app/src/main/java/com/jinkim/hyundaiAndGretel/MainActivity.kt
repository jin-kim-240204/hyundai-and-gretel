package com.jinkim.hyundaiAndGretel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesRoute
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RoadNotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RoadNotesRoute(viewModel, window) }
    }
}
