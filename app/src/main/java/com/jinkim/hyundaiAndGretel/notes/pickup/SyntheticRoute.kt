package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.LocationFix
import kotlin.math.abs
import kotlin.math.cos

data class SyntheticSession(val actorId: String = "A", val routeId: String = "R1") {
    init { require(actorId.isNotBlank()) }
}

data class RouteContext(val routeId: String, val progressMeters: Double)

object SyntheticRoute {
    private const val RADIUS = 6_371_000.0
    private const val LATITUDE = 37.5
    private const val START_LONGITUDE = 127.02
    private const val END_LONGITUDE = 127.05
    private val scale = RADIUS * cos(Math.toRadians(LATITUDE))
    val endProgressMeters = progressAt(END_LONGITUDE)

    private fun progressAt(longitude: Double) = scale * Math.toRadians(longitude - START_LONGITUDE)

    fun fixAt(progressMeters: Double, receivedElapsedMillis: Long): LocationFix {
        require(progressMeters in 0.0..endProgressMeters) { "출발점이 경로 밖입니다" }
        val longitude = START_LONGITUDE + Math.toDegrees(progressMeters / scale)
        return LocationFix(LATITUDE, longitude, receivedElapsedMillis)
    }

    fun project(latitude: Double, longitude: Double, setup: SyntheticSession): RouteContext? {
        if (setup.routeId != "R1" || !latitude.isFinite() || !longitude.isFinite()) return null
        if (longitude !in START_LONGITUDE..END_LONGITUDE) return null
        val crossTrack = RADIUS * Math.toRadians(latitude - LATITUDE)
        if (abs(crossTrack) > 20.0) return null
        return RouteContext(setup.routeId, progressAt(longitude))
    }

    fun matches(context: RouteContext, latitude: Double, longitude: Double): Boolean {
        if (!context.progressMeters.isFinite()) return false
        val projected = project(latitude, longitude, SyntheticSession(routeId = context.routeId))
            ?: return false
        return abs(projected.progressMeters - context.progressMeters) <= 1e-6
    }
}
