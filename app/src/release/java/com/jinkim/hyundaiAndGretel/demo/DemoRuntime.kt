package com.jinkim.hyundaiAndGretel.demo

import android.content.Context
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** No simulation, network, commands or mock provider in this variant. */
@Suppress("UNUSED_PARAMETER")
class DemoRuntime(context: Context, scope: CoroutineScope, host: DemoHost, store: ScenarioStore,
                  active: StateFlow<String?>, restrictions: StateFlow<UiRestrictions>) {
    val enabled = false
    val hasPendingWrites = false
    val transitioning = false
    val canSubmit = false
    val blocksNewWrites = false
    fun requestStart() = false // Existing genuine SDK tracking handles the request.
    fun accept(fix: LocationFix?) = true
    fun stop() = Unit
    fun close() = Unit
    fun onSourceFailure() = Unit
}
