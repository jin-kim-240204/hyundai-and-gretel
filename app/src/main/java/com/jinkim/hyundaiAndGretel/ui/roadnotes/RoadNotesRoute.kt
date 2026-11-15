package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinkim.hyundaiAndGretel.demo.DemoPanel
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

@Composable
fun RoadNotesRoute(
    window: Window,
    viewModel: RoadNotesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preparation by viewModel.trackingPreparation.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    KeepScreenOnWhileTracking(window, state.trackingStatus.isCollecting)

    HyundaiAndGretelTheme {
        LocationPermissionGate(state.restrictions, onStopTracking = viewModel::stopTracking, onPermissionsReady = viewModel::ensureTracking, onPermissionsLost = viewModel::onPermissionsLost) {
            Box(Modifier.fillMaxSize()) {
                RoadNotesScreen(
                    state = state,
                    trackingPreparation = preparation,
                    onConfirmReceipt = { key ->
                        viewModel.confirmReceipt(key, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                    },
                    isReceiptCurrent = viewModel::isReceiptCurrent,
                    onContentShown = viewModel::markPresented,
                )

                DemoPanel(viewModel, onStart = viewModel::startTracking, onStop = viewModel::stopTracking)
            }
        }
    }
}
