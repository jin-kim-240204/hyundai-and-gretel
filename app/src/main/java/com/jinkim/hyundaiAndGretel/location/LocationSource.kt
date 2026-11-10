package com.jinkim.hyundaiAndGretel.location

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val receivedElapsedMillis: Long,
)

interface LocationSource {
    fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean
    fun stop()
}
