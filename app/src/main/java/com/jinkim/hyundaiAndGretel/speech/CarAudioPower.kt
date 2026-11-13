package com.jinkim.hyundaiAndGretel.speech

import android.car.Car
import android.car.hardware.power.CarPowerManager
import android.car.hardware.power.CarPowerPolicyFilter
import android.car.hardware.power.PowerComponent
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 화면 켜짐을 차량 전원 상태로 추측하지 않고 AUDIO 전원 정책을 관측한다. */
class CarAudioPower(
    private val scope: CoroutineScope,
    private val log: (String) -> Unit = {},
    private val onDisabled: () -> Unit,
) {
    var enabled = false
        private set
    private var car: Car? = null
    private var manager: CarPowerManager? = null
    private var listener: CarPowerManager.CarPowerPolicyListener? = null
    private var closed = false
    private var owner: Any? = null

    fun start(context: Context) {
        log("POWER_CONNECTING")
        try {
            car = Car.createCar(context.applicationContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                scope.launch {
                    if (closed) return@launch
                    clearListener()
                    disable()
                    if (!ready) {
                        log("POWER_DISCONNECTED")
                        return@launch
                    }
                    try {
                        val next = connected.getCarManager(Car.POWER_SERVICE) as CarPowerManager
                        val token = Any()
                        owner = token
                        val callback = CarPowerManager.CarPowerPolicyListener { policy ->
                            scope.launch {
                                if (!closed && owner === token) {
                                    applyPolicy(policy.isComponentEnabled(PowerComponent.AUDIO))
                                }
                            }
                        }
                        manager = next
                        listener = callback
                        next.addPowerPolicyListener(context.mainExecutor,
                            CarPowerPolicyFilter.Builder().setComponents(PowerComponent.AUDIO).build(), callback)
                        val policy = next.currentPowerPolicy
                        if (policy == null) {
                            // The registered listener can still deliver a policy after an empty initial snapshot.
                            log("POWER_POLICY_UNAVAILABLE")
                        } else {
                            applyPolicy(policy.isComponentEnabled(PowerComponent.AUDIO))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        policyFailed(error)
                    } catch (error: LinkageError) {
                        policyFailed(error)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            policyFailed(error)
        } catch (error: LinkageError) {
            policyFailed(error)
        }
    }

    private fun applyPolicy(audioEnabled: Boolean) {
        enabled = audioEnabled
        log(if (enabled) "POWER_AUDIO_ENABLED" else "POWER_AUDIO_DISABLED")
        if (!enabled) onDisabled()
    }

    private fun policyFailed(error: Throwable) {
        log("POWER_POLICY_FAILED ${error.javaClass.simpleName}")
        clearListener()
        disable()
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
        if (previous != null && callback != null) {
            try {
                previous.removePowerPolicyListener(callback)
            } catch (error: Exception) {
                log("POWER_LISTENER_CLEANUP ${error.javaClass.simpleName}")
            } catch (error: LinkageError) {
                log("POWER_LISTENER_CLEANUP ${error.javaClass.simpleName}")
            }
        }
    }

    fun close() {
        closed = true
        disable()
        clearListener()
        car?.disconnect()
        car = null
    }
}
