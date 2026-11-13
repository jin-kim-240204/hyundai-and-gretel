package com.jinkim.hyundaiAndGretel.speech

import android.car.Car
import android.car.hardware.power.CarPowerManager
import android.car.hardware.power.CarPowerPolicyFilter
import android.car.hardware.power.PowerComponent
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 화면 켜짐을 차량 전원 상태로 추측하지 않고 AUDIO 전원 정책을 관측한다. */
class CarAudioPower(private val scope: CoroutineScope, private val onDisabled: () -> Unit) {
    var enabled = false
        private set
    private var car: Car? = null
    private var manager: CarPowerManager? = null
    private var listener: CarPowerManager.CarPowerPolicyListener? = null
    private var closed = false
    private var owner: Any? = null

    fun start(context: Context) {
        try {
            car = Car.createCar(context.applicationContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                scope.launch {
                    if (closed) return@launch
                    clearListener()
                    disable()
                    if (!ready) return@launch
                    try {
                        val next = connected.getCarManager(Car.POWER_SERVICE) as CarPowerManager
                        val token = Any()
                        owner = token
                        val callback = CarPowerManager.CarPowerPolicyListener { policy ->
                            scope.launch {
                                if (!closed && owner === token) {
                                    enabled = policy.isComponentEnabled(PowerComponent.AUDIO)
                                    if (!enabled) onDisabled()
                                }
                            }
                        }
                        manager = next
                        listener = callback
                        next.addPowerPolicyListener(context.mainExecutor,
                            CarPowerPolicyFilter.Builder().setComponents(PowerComponent.AUDIO).build(), callback)
                        enabled = next.currentPowerPolicy.isComponentEnabled(PowerComponent.AUDIO)
                    } catch (_: Exception) { clearListener(); disable() }
                }
            }
        } catch (_: Exception) { disable() }
        catch (_: LinkageError) { disable() }
    }

    private fun disable() {
        enabled = false
        onDisabled()
    }

    private fun clearListener() {
        val previous = manager
        val callback = listener
        manager = null
        listener = null
        owner = null
        if (previous != null && callback != null) runCatching { previous.removePowerPolicyListener(callback) }
    }

    fun close() {
        closed = true
        disable()
        clearListener()
        car?.disconnect()
        car = null
    }
}
