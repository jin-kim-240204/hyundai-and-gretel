package com.jinkim.hyundaiAndGretel.demo

import android.content.Context
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** 릴리스 빌드에는 시뮬레이션, 콘솔 통신, 시연 명령과 모의 위치 제공자가 없다. */
@Suppress("UNUSED_PARAMETER")
class DemoRuntime(context: Context, scope: CoroutineScope, host: DemoServiceContract, store: ScenarioStore,
                  active: StateFlow<String?>, restrictions: StateFlow<UiRestrictions>) {
    val enabled = false
    val hasPendingWrites = false
    val transitioning = false
    val canSubmit = false
    val blocksNewWrites = false
    fun requestStart() = false // 시연이 요청을 가로채지 않고 기존 실제 SDK 위치 수집으로 넘긴다.
    fun accept(fix: LocationFix?) = true
    fun stop() = Unit
    fun close() = Unit
    fun onSourceFailure() = Unit
}
