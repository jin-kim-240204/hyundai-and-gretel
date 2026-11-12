package com.jinkim.hyundaiAndGretel.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.os.SystemClock

class PleosLocationSource(context: Context) : LocationSource {
    private val sdk = FusedLocation(context.applicationContext)
    private var initialized = false
    private var listener: FusedLocationListener? = null

    override fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean {
        if (!sdk.isSupported()) return false
        initialized = true
        sdk.initialize()
        listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                val receivedAt = SystemClock.elapsedRealtime()
                onLocation(location?.let { LocationFix(it.latitude, it.longitude, receivedAt) }, receivedAt)
            }
            override fun onError(exception: Exception) = onError.invoke(exception)
        }
        sdk.registerFusedLocationCallback(checkNotNull(listener))
        return true
    }

    override fun addProximity(
        latitude: Double, longitude: Double,
        onRegistered: (String) -> Unit, onEnter: (String) -> Unit, onError: (Exception) -> Unit,
    ) {
        check(initialized)
        sdk.addProximityAlert(
            ProximityAlertRequest(latitude, longitude, 150f, setOf(ProximityTransition.ENTER)),
            object : ProximityAlertListener {
                override fun onRegistered(alertId: String) = onRegistered.invoke(alertId)
                override fun onTransition(event: ProximityAlertEvent) {
                    if (event.transition == ProximityTransition.ENTER) onEnter(event.alertId)
                }
                override fun onError(exception: Exception) = onError.invoke(exception)
            },
        )
    }

    override fun removeProximity(alertId: String) {
        check(initialized)
        sdk.removeProximityAlert(alertId)
    }

    override fun stop() {
        val previous = listener
        listener = null
        try {
            if (previous != null) sdk.unregisterFusedLocationCallback(previous)
        } finally {
            if (initialized) {
                initialized = false
                sdk.release()
            }
        }
    }
}
