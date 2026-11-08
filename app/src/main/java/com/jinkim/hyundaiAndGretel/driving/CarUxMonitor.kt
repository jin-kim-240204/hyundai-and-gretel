package com.jinkim.hyundaiAndGretel.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jinkim.hyundaiAndGretel.BuildConfig

internal class CarUxMonitor(
    private val context: Context,
    private val onRestricted: (Boolean) -> Unit,
) : DefaultLifecycleObserver {
    private var started = false
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    override fun onStart(owner: LifecycleOwner) {
        started = true
        onRestricted(true)
        try {
            car = Car.createCar(context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!started) return@createCar
                if (!ready) {
                    manager = null
                    onRestricted(true)
                } else try {
                    val current = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                    manager?.unregisterListener()
                    manager = current
                    current.registerListener(::update)
                    update(current.currentCarUxRestrictions)
                } catch (_: Exception) {
                    onRestricted(true)
                }
            }
        } catch (_: Exception) {
            onRestricted(true)
        }
    }

    private fun update(restrictions: CarUxRestrictions?) {
        val restricted = useCompactDrivingUi(
            restrictions?.isRequiresDistractionOptimization,
            restrictions?.activeRestrictions,
        )
        onRestricted(restricted)
        if (BuildConfig.DEBUG) {
            Log.d("GleoProbe", "[UX] compact=$restricted activeRestrictions=${restrictions?.activeRestrictions}")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        started = false
        onRestricted(true)
        try { manager?.unregisterListener() } catch (_: Exception) { }
        manager = null
        try { car?.disconnect() } catch (_: Exception) { }
        car = null
    }
}
