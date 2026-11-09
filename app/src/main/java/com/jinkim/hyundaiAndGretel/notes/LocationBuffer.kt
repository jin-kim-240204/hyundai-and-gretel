package com.jinkim.hyundaiAndGretel.notes

import kotlin.math.*

object DemoRoute {
    const val ID = "demo-r1-east-v1"
    const val LATITUDE = 37.4979
    const val LONGITUDE = 127.0276
    private val longitudeMeters = 111_195.0 * cos(Math.toRadians(LATITUDE))
    fun longitudeAt(meters: Double) = LONGITUDE + meters / longitudeMeters
    fun progress(latitude: Double, longitude: Double): Double? {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        if (abs(latitude - LATITUDE) * 111_195 > 20) return null
        return (longitude - LONGITUDE) * longitudeMeters
    }
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}

data class LocationSample(val latitude: Double, val longitude: Double, val elapsedMillis: Long,
                          val progressMeters: Double, val heading: Float?)
data class Anchor(val sample: LocationSample, val offsetMillis: Long) {
    val errorMillis get() = abs(offsetMillis - 30_000)
}

/** Session-local callback times, never persisted or represented as GPS measurement times. */
class LocationBuffer {
    private val samples = ArrayDeque<LocationSample>()
    fun clear() = samples.clear()
    fun add(sample: LocationSample) {
        val last = samples.lastOrNull()
        if (last != null && sample.elapsedMillis <= last.elapsedMillis) return
        if (last != null && sample.elapsedMillis - last.elapsedMillis > 5_000) clear()
        samples.addLast(sample)
        prune(sample.elapsedMillis)
    }
    fun snapshot(now: Long): List<LocationSample> {
        prune(now)
        return samples.toList()
    }
    private fun prune(now: Long) {
        while (samples.isNotEmpty() && now - samples.first().elapsedMillis > 45_000) samples.removeFirst()
        if (samples.isNotEmpty() && now - samples.last().elapsedMillis > 5_000) clear()
    }
    fun anchor(now: Long): Anchor? = selectAnchor(snapshot(now), now)
}

fun selectAnchor(samples: List<LocationSample>, now: Long): Anchor? {
    if (samples.isEmpty() || now - samples.first().elapsedMillis < 30_000 || now - samples.last().elapsedMillis !in 0..5_000) return null
    val target = now - 30_000
    val sample = samples.minWith(compareBy<LocationSample> { abs(it.elapsedMillis - target) }.thenBy { it.elapsedMillis })
    return Anchor(sample, now - sample.elapsedMillis).takeIf { it.errorMillis <= 5_000 }
}
