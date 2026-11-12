package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 서비스와 같은 메인 실행 문맥을 사용한다. 미연결 시 마지막 제한을 유지하고 설정은 허용하지 않는다. */
class CarRestrictionsSource(private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(UiRestrictions())
    val state = mutable.asStateFlow()
    private var manager: CarUxRestrictionsManager? = null
    private var car: Car? = null
    private var closed = false
    private var connectionOwner: Any? = null

    // 리뷰용: 차량 서비스 연결 후 규제 리스너 등록과 현재 규제 조회를 함께 수행한다. 미연결을 설정 허용 상태로 꾸미지 않는다.
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
    // 리뷰용: OS가 제공한 규제와 글자·항목·깊이 제한을 화면 정책 값으로 옮긴다.
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
