package com.jinkim.hyundaiAndGretel.notes

import kotlin.math.ceil
import kotlin.math.floor

data class RoutePlotRange(val startMeters: Double, val endMeters: Double) {
    fun fraction(progress: Double): Float = ((progress-startMeters)/(endMeters-startMeters)).toFloat()
}

/** Stable world-distance axis for the small local demo, independent of nearby-fetch boundaries. */
fun routePlotRange(current: Double?, notes: List<Double>): RoutePlotRange {
    val points = (notes + listOfNotNull(current)).filter { it.isFinite() }
    return RoutePlotRange(
        minOf(-1000.0, floor(((points.minOrNull() ?: 0.0)-100)/500)*500),
        maxOf(1500.0, ceil(((points.maxOrNull() ?: 0.0)+100)/500)*500),
    )
}
