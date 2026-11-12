package com.jinkim.hyundaiAndGretel.notes.spatial

import kotlin.math.*

internal const val EARTH_RADIUS_METERS = 6_371_000.0

fun distanceMeters(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
    val deltaLatitude = Math.toRadians(toLatitude - fromLatitude)
    val deltaLongitude = Math.toRadians(toLongitude - fromLongitude)
    val latitude1 = Math.toRadians(fromLatitude)
    val latitude2 = Math.toRadians(toLatitude)
    val a = sin(deltaLatitude / 2).pow(2) + cos(latitude1) * cos(latitude2) * sin(deltaLongitude / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
