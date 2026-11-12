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
import com.jinkim.hyundaiAndGretel.demo.DemoPanel
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

/** 화면 상태 구독과 이벤트 연결은 여기서, 실제 화면 구성은 Screen에서 담당한다. */
@Composable
fun RoadNotesRoute(viewModel: RoadNotesViewModel, window: Window) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    KeepScreenOnWhileTracking(window, state.trackingStatus.isCollecting)

    HyundaiAndGretelTheme {
        LocationPermissionGate(state.restrictions, onStopTracking = viewModel::stop) {
            Box(Modifier.fillMaxSize()) {
                RoadNotesScreen(
                    state = state,
                    startAllowed = viewModel.canStartTracking,
                    onStartTracking = viewModel::start,
                    onStopTracking = viewModel::stop,
                    onConfirmReceipt = { key ->
                        viewModel.confirmReceipt(key, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                    },
                    onContentShown = viewModel::markPresented,
                )
                // 디버그 전용 UI 연결은 이 한 곳에만 둔다. 릴리스 구현은 아무것도 표시하지 않는다.
                DemoPanel(viewModel, onStart = viewModel::start, onStop = viewModel::stop)
            }
        }
    }
}
