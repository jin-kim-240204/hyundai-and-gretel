package com.jinkim.hyundaiAndGretel.location

/** 등록 함수의 반환은 등록 성공이 아니다. 이 소스가 비동기 콜백으로 등록 ID를 전달한다. */
interface ProximitySource {
    fun addProximity(
        latitude: Double, longitude: Double,
        onRegistered: (String) -> Unit,
        onEnter: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) { throw UnsupportedOperationException("Proximity unavailable") }
    fun removeProximity(alertId: String) { throw UnsupportedOperationException("Proximity unavailable") }
}
