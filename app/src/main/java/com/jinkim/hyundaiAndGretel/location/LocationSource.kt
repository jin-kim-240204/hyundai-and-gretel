package com.jinkim.hyundaiAndGretel.location

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val receivedElapsedMillis: Long,
)

interface LocationSource {
    /**
     * Returns false only when the SDK is unsupported. Initialization/registration failures throw.
     * True means the calls returned, not that a first location or SDK connection is ready.
     * receivedElapsedMillis is monotonic callback-entry time in ms, including null invalidations.
     */
    fun start(
        onLocation: (fix: LocationFix?, receivedElapsedMillis: Long) -> Unit,
        onError: (Exception) -> Unit,
    ): Boolean
    fun stop()
}
