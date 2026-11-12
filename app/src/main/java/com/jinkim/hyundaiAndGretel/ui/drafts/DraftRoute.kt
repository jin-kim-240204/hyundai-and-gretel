package com.jinkim.hyundaiAndGretel.ui.drafts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ViewModel 상태를 생명주기에 맞춰 수집하고 화면 이벤트를 연결한다.
 * 첫 진입과 포그라운드 복귀 때 저장소를 다시 조회한다.
 */
@Composable
fun DraftRoute(viewModel: DraftsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    DraftScreen(
        state = state,
        onConsentChange = viewModel::setConsent,
        onDelete = viewModel::delete,
        onRefresh = viewModel::refresh,
    )
}
