package com.jinkim.hyundaiAndGretel.demo

import android.content.Context
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@Suppress("UNUSED_PARAMETER")
class DemoRuntime(context: Context, scope: CoroutineScope, host: DemoServiceContract, store: ScenarioStore,
                  active: StateFlow<String?>, restrictions: StateFlow<UiRestrictions>) {
    val automaticStartBlockReason: String? = null
    val enabled = false
    val hasPendingWrites = false
    val transitioning = false
    val canSubmit = false
    val blocksNewWrites = false
    fun requestStart() = false
    fun accept(fix: LocationFix?) = true
    fun stop() = Unit
    fun close() = Unit
    fun onSourceFailure() = Unit
}
