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
import android.util.Log
import com.jinkim.hyundaiAndGretel.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PleosLocationSource(context: Context) : LocationSource {
    private val sdk = FusedLocation(context.applicationContext)
    private var polling: Job? = null
    private var initialized = false
    private var listener: FusedLocationListener? = null

    // 리뷰용: 실제 Pleos SDK 어댑터: 지원 여부 확인 → 초기화 → 동일 리스너 보관 → 위치 콜백 등록.
    override fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean {
        if (!sdk.isSupported()) return false
        initialized = true
        sdk.initialize()
        listener = object : FusedLocationListener {
            // 리뷰용: SDK 콜백 진입 순간의 단조시각을 기록한다. null 좌표도 무효화 이벤트로 상위 처리자에게 전달한다.
            override fun onChanged(location: LocationData?) {
                val receivedAt = SystemClock.elapsedRealtime()
                onLocation(location?.let { LocationFix(it.latitude, it.longitude, receivedAt) }, receivedAt)
            }
            override fun onError(exception: Exception) = onError.invoke(exception)
        }
        sdk.registerFusedLocationCallback(checkNotNull(listener))
        // shortcut: 임시 1초 캐시 조회 로그, 확인 후 이 polling 코드를 제거한다.
        if (BuildConfig.DEBUG) polling = CoroutineScope(Dispatchers.Main).launch {
            val probe = object : FusedLocationListener {
                override fun onChanged(location: LocationData?) {
                    if (listener != null) Log.d("RoadLocation", "POLL_1S cached lat=${location?.latitude} lon=${location?.longitude}")
                }
                override fun onError(exception: Exception) {
                    Log.d("RoadLocation", "POLL_1S_ERROR ${exception.javaClass.simpleName}")
                }
            }
            while (isActive) {
                try {
                    sdk.getLastKnownLocation(probe)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    probe.onError(error)
                }
                delay(1_000)
            }
        }
        return true
    }

    // 리뷰용: 쪽지 좌표의 전달받은 반경의 ENTER 감시를 SDK에 요청한다. 등록 ID는 반환값이 아니라 이후 콜백으로 도착한다.
    override fun addProximity(
        latitude: Double, longitude: Double, radiusMeters: Float,
        onRegistered: (String) -> Unit, onEnter: (String) -> Unit, onError: (Exception) -> Unit,
    ) {
        check(initialized)
        sdk.addProximityAlert(
            ProximityAlertRequest(latitude, longitude, radiusMeters, setOf(ProximityTransition.ENTER)),
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

    // 리뷰용: 보관한 동일 리스너로 등록을 해제하고, 중간에 예외가 발생해도 SDK release는 실행한다.
    override fun stop() {
        polling?.cancel()
        polling = null
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
