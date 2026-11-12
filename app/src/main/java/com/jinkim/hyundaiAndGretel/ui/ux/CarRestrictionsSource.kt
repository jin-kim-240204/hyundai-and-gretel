package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Same Main owner as the service. Unknown preserves the last limits, never enables setup. */
class CarRestrictionsSource(private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(UiRestrictions())
    val state = mutable.asStateFlow()
    private var manager: CarUxRestrictionsManager? = null
    private var car: Car? = null
    private var closed = false
    private var connectionOwner: Any? = null

    fun start(context: Context) {
        try {
            car = Car.createCar(context.applicationContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                scope.launch {
                    if (closed) return@launch
                    connectionOwner = null
                    if (!ready) {
                        try { manager?.unregisterListener() } catch (_: Exception) { }
                        manager = null; unknown(); return@launch
                    }
                    try {
                        manager?.unregisterListener()
                        val next = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                        manager = next
                        val owner = Any(); connectionOwner = owner
                        next.registerListener { restrictions -> scope.launch { if (!closed && connectionOwner === owner) apply(restrictions) } }
                        apply(next.currentCarUxRestrictions)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { unknown() }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { unknown() }
        catch (_: LinkageError) { unknown() }
    }
    private fun apply(value: CarUxRestrictions) {
        mutable.value = UiRestrictions(true, value.isRequiresDistractionOptimization, value.activeRestrictions,
            value.maxRestrictedStringLength, value.maxCumulativeContentItems, value.maxContentDepth)
    }
    private fun unknown() { mutable.value = mutable.value.copy(connected = false) }
    fun close() {
        closed = true; connectionOwner = null; unknown()
        try { manager?.unregisterListener() }
        finally { manager = null; car?.disconnect(); car = null }
    }
}
