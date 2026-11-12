package com.jinkim.hyundaiAndGretel.location

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val receivedElapsedMillis: Long,
)

data class TrackedPosition(val session: Long, val fix: LocationFix)

interface LocationSource : ProximitySource {

    fun start(
        onLocation: (fix: LocationFix?, receivedElapsedMillis: Long) -> Unit,
        onError: (Exception) -> Unit,
    ): Boolean
    fun stop()
}
