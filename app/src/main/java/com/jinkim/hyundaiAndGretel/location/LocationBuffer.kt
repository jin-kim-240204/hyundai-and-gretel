package com.jinkim.hyundaiAndGretel.location

import kotlin.math.abs

data class LocationAnchor(val sample: LocationFix, val selectedElapsedMillis: Long) {
    val offsetMillis get() = selectedElapsedMillis - sample.receivedElapsedMillis
}

sealed interface AnchorSelection {
    data class Selected(val anchor: LocationAnchor) : AnchorSelection
    enum class Unavailable : AnchorSelection { INVALID_TIME, EMPTY, STALE, NO_TARGET_SAMPLE }
}

/** Confined to the tracking owner's execution context; times are callback receipt milliseconds. */
class LocationBuffer {
    enum class Update { ACCEPTED, INVALIDATED, IGNORED }

    private val samples = ArrayDeque<LocationFix>()
    // Retained across pruning/invalidations so a delayed callback cannot restore older data.
    private var lastEventAt = -1L
    val size get() = samples.size
    val oldestElapsedMillis get() = samples.firstOrNull()?.receivedElapsedMillis
    val latestElapsedMillis get() = samples.lastOrNull()?.receivedElapsedMillis

    fun offer(fix: LocationFix?, receivedAt: Long, now: Long): Update {
        if (receivedAt < 0 || receivedAt > now || receivedAt < lastEventAt) return Update.IGNORED
        if (fix != null && fix.receivedElapsedMillis != receivedAt) return Update.IGNORED
        if (fix == null || !fix.hasValidCoordinates()) {
            lastEventAt = receivedAt
            samples.clear()
            return Update.INVALIDATED
        }
        if (receivedAt == lastEventAt) return Update.IGNORED
        lastEventAt = receivedAt
        prune(now)
        if (now - receivedAt > RETENTION_MILLIS) return Update.IGNORED
        samples.addLast(fix)
        return Update.ACCEPTED
    }

    fun select(now: Long): AnchorSelection {
        if (now < 0 || now < lastEventAt) return AnchorSelection.Unavailable.INVALID_TIME
        prune(now)
        val oldest = samples.firstOrNull() ?: return AnchorSelection.Unavailable.EMPTY
        val latest = samples.last()
        if (now - latest.receivedElapsedMillis > FRESHNESS_MILLIS) return AnchorSelection.Unavailable.STALE
        val sample = if (now - oldest.receivedElapsedMillis < TARGET_MILLIS) oldest else {
            samples.minWith(compareBy<LocationFix> { abs(now - it.receivedElapsedMillis - TARGET_MILLIS) }
                .thenBy { it.receivedElapsedMillis })
        }
        val anchor = LocationAnchor(sample, now)
        if (now - oldest.receivedElapsedMillis >= TARGET_MILLIS && abs(anchor.offsetMillis - TARGET_MILLIS) > TARGET_TOLERANCE_MILLIS) {
            return AnchorSelection.Unavailable.NO_TARGET_SAMPLE
        }
        return AnchorSelection.Selected(anchor)
    }

    fun reset() {
        samples.clear()
        lastEventAt = -1L
    }

    private fun prune(now: Long) {
        while (samples.isNotEmpty() && now - samples.first().receivedElapsedMillis > RETENTION_MILLIS) samples.removeFirst()
    }

    private companion object {
        const val RETENTION_MILLIS = 45_000L
        const val FRESHNESS_MILLIS = 5_000L
        const val TARGET_MILLIS = 30_000L
        const val TARGET_TOLERANCE_MILLIS = 5_000L
    }
}

internal fun LocationFix.hasValidCoordinates() = latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0
