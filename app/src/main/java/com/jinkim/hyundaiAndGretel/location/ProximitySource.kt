package com.jinkim.hyundaiAndGretel.location

interface ProximitySource {
    fun addProximity(
        latitude: Double, longitude: Double, radiusMeters: Float,
        onRegistered: (String) -> Unit,
        onEnter: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) { throw UnsupportedOperationException("Proximity unavailable") }
    fun removeProximity(alertId: String) { throw UnsupportedOperationException("Proximity unavailable") }
}
