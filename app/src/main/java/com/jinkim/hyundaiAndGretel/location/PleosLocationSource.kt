package com.jinkim.hyundaiAndGretel.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.os.SystemClock

class PleosLocationSource(context: Context) : LocationSource {
    private val sdk = FusedLocation(context.applicationContext)
    private var initialized = false
    private var listener: FusedLocationListener? = null

    override fun start(onLocation: (LocationFix?) -> Unit, onError: (Exception) -> Unit): Boolean {
        if (!sdk.isSupported()) return false
        initialized = true
        sdk.initialize()
        listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                val receivedAt = SystemClock.elapsedRealtime()
                onLocation(location?.let { LocationFix(it.latitude, it.longitude, receivedAt) })
            }
            override fun onError(exception: Exception) = onError.invoke(exception)
        }
        sdk.registerFusedLocationCallback(checkNotNull(listener))
        return true
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
