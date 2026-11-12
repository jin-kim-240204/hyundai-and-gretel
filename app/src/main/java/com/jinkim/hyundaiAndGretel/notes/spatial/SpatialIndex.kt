package com.jinkim.hyundaiAndGretel.notes.spatial

interface SpatialIndex {
    fun cell(latitude: Double, longitude: Double): String
    fun cover(latitude: Double, longitude: Double): Set<String>
}

class SpatialUnavailableException(cause: Throwable) : RuntimeException(cause)

object CandidateArea {
    const val RADIUS_METERS = 400.0
    const val MAX_QUERY_CELLS = 512

    fun contains(latitude: Double, longitude: Double) =
        latitude in 37.4900..37.5100 && longitude in 127.0200..127.0500
}
