package com.jinkim.hyundaiAndGretel.location

import android.content.Context
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import kotlinx.coroutines.delay

/** Owned by the session controller; all SDK calls run on Main. */
class PleosLocationSource(private val context: Context) {
    private var sdk: FusedLocation? = null
    private var listener: FusedLocationListener? = null
    private val alerts = mutableSetOf<String>()

    suspend fun start(onLocation: (LocationData?) -> Unit, onError: (Exception) -> Unit) {
        stop()
        val instance = FusedLocation(context.applicationContext)
        check(instance.isSupported()) { "FusedLocation is unsupported" }
        sdk = instance
        instance.initialize()
        // shortcut: SDK exposes no binding-ready callback; replace with readiness API when available.
        delay(1_500)
        if (sdk !== instance) return
        listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) = onLocation(location)
            override fun onError(exception: Exception) = onError(exception)
        }.also(instance::registerFusedLocationCallback)
    }

    fun register(latitude: Double, longitude: Double, onRegistered: (String) -> Unit,
                 onEvent: (ProximityAlertEvent) -> Unit, onError: (Exception) -> Unit) {
        val instance = checkNotNull(sdk) { "Location is not started" }
        instance.addProximityAlert(
            ProximityAlertRequest(latitude, longitude, 150f, setOf(ProximityTransition.ENTER, ProximityTransition.EXIT)),
            object : ProximityAlertListener {
                override fun onRegistered(alertId: String) {
                    if (sdk !== instance) {
                        runCatching { instance.removeProximityAlert(alertId) }
                        return
                    }
                    alerts += alertId
                    onRegistered(alertId)
                }
                override fun onTransition(event: ProximityAlertEvent) = onEvent(event)
                override fun onError(exception: Exception) = onError(exception)
            },
        )
    }

    fun remove(id: String) {
        sdk?.removeProximityAlert(id)
        alerts -= id
    }

    fun stop() {
        val instance = sdk ?: return
        sdk = null
        try {
            alerts.toList().forEach { runCatching { instance.removeProximityAlert(it) } }
            listener?.let { runCatching { instance.unregisterFusedLocationCallback(it) } }
        } finally {
            alerts.clear()
            listener = null
            instance.release()
        }
    }
}
