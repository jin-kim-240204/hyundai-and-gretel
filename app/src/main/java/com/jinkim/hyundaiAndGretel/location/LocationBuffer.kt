package com.jinkim.hyundaiAndGretel.location

import kotlin.math.abs
import java.util.concurrent.TimeUnit

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
    private var lastEventElapsedMillis = -1L
    val size get() = samples.size
    val oldestElapsedMillis get() = samples.firstOrNull()?.receivedElapsedMillis
    val latestElapsedMillis get() = samples.lastOrNull()?.receivedElapsedMillis

    fun offer(fix: LocationFix?, receivedElapsedMillis: Long, nowElapsedMillis: Long): Update {
        if (receivedElapsedMillis < 0 || receivedElapsedMillis > nowElapsedMillis || receivedElapsedMillis < lastEventElapsedMillis) return Update.IGNORED
        if (fix != null && fix.receivedElapsedMillis != receivedElapsedMillis) return Update.IGNORED
        if (fix == null || !fix.hasValidCoordinates()) {
            lastEventElapsedMillis = receivedElapsedMillis
            samples.clear()
            return Update.INVALIDATED
        }
        if (receivedElapsedMillis == lastEventElapsedMillis) return Update.IGNORED
        lastEventElapsedMillis = receivedElapsedMillis
        prune(nowElapsedMillis)
        if (nowElapsedMillis - receivedElapsedMillis > RETENTION_MILLIS) return Update.IGNORED
        samples.addLast(fix)
        return Update.ACCEPTED
    }

    fun select(now: Long): AnchorSelection {
        if (now < 0 || now < lastEventElapsedMillis) return AnchorSelection.Unavailable.INVALID_TIME
        prune(now)
        val oldest = samples.firstOrNull() ?: return AnchorSelection.Unavailable.EMPTY
        val latest = samples.last()
        if (now - latest.receivedElapsedMillis > FRESHNESS_MILLIS) return AnchorSelection.Unavailable.STALE
        val historyCoversTarget = now - oldest.receivedElapsedMillis >= TARGET_MILLIS
        val targetElapsedMillis = now - TARGET_MILLIS
        val sample = when {
            !historyCoversTarget -> oldest
            else -> samples.minWith(
                compareBy<LocationFix> { abs(it.receivedElapsedMillis - targetElapsedMillis) }
                    .thenBy { it.receivedElapsedMillis },
            )
        }
        val targetErrorMillis = abs(sample.receivedElapsedMillis - targetElapsedMillis)
        return when {
            historyCoversTarget && targetErrorMillis > TARGET_TOLERANCE_MILLIS -> AnchorSelection.Unavailable.NO_TARGET_SAMPLE
            else -> AnchorSelection.Selected(LocationAnchor(sample, now))
        }
    }

    fun reset() {
        samples.clear()
        lastEventElapsedMillis = -1L
    }

    private fun prune(now: Long) {
        while (samples.isNotEmpty() && now - samples.first().receivedElapsedMillis > RETENTION_MILLIS) samples.removeFirst()
    }

    private companion object {
        val RETENTION_MILLIS = TimeUnit.SECONDS.toMillis(45)
        val FRESHNESS_MILLIS = TimeUnit.SECONDS.toMillis(5)
        val TARGET_MILLIS = TimeUnit.SECONDS.toMillis(30)
        val TARGET_TOLERANCE_MILLIS = TimeUnit.SECONDS.toMillis(5)
    }
}

private fun LocationFix.hasValidCoordinates() = latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0
