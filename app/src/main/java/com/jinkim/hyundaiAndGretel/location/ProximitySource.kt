package com.jinkim.hyundaiAndGretel.location

/** add returning is not registration success. IDs are delivered asynchronously by this owner. */
interface ProximitySource {
    fun addProximity(
        latitude: Double, longitude: Double,
        onRegistered: (String) -> Unit,
        onEnter: (String) -> Unit,
        onError: (Exception) -> Unit,
    ) { throw UnsupportedOperationException("Proximity unavailable") }
    fun removeProximity(alertId: String) { throw UnsupportedOperationException("Proximity unavailable") }
}
