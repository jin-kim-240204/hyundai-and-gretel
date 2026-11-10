package com.jinkim.hyundaiAndGretel.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationFix(val latitude: Double, val longitude: Double, val receivedElapsedMillis: Long)

enum class TrackingStatus { STOPPED, STARTING, WAITING, RECEIVING, UNAVAILABLE, UNSUPPORTED, FAILED }

interface LocationSource {
    fun start(onLocation: (LocationFix?) -> Unit, onError: (Exception) -> Unit): Boolean
    fun stop()
}

/** Commands and callback delivery are serialized on the owner's Main scope. */
class LocationTracking(
    private val scope: CoroutineScope,
    private val createSource: () -> LocationSource,
    private val log: (String) -> Unit,
    private val onFailure: () -> Unit,
) {
    private val mutableStatus = MutableStateFlow(TrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private var source: LocationSource? = null
    private var generation = 0L

    fun start() {
        if (source != null) return
        val session = ++generation
        update(TrackingStatus.STARTING)
        try {
            val current = createSource()
            source = current
            val supported = current.start(
                onLocation = { fix ->
                    scope.launch {
                        if (generation != session) return@launch
                        if (fix == null || !fix.hasValidCoordinates()) {
                            update(TrackingStatus.UNAVAILABLE)
                            return@launch
                        }
                        update(TrackingStatus.RECEIVING)
                        log("session=$session LOCATION lat=${fix.latitude} lon=${fix.longitude} elapsed=${fix.receivedElapsedMillis}")
                    }
                },
                onError = { error ->
                    scope.launch {
                        if (generation == session) fail(error)
                    }
                },
            )
            if (!supported) {
                stop()
                update(TrackingStatus.UNSUPPORTED)
                onFailure()
                return
            }
            log("session=$session REGISTERED")
            update(TrackingStatus.WAITING)
        } catch (cancelled: CancellationException) {
            stop()
            throw cancelled
        } catch (error: Exception) {
            fail(error)
        }
    }

    fun stop() {
        ++generation
        val previous = source
        source = null
        update(TrackingStatus.STOPPED)
        try {
            previous?.stop()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            log("session=$generation CLEANUP_ERROR ${error.javaClass.simpleName}")
        }
    }

    fun fail(error: Exception) {
        if (error is CancellationException) {
            stop()
            throw error
        }
        stop()
        update(TrackingStatus.FAILED)
        log("session=$generation SDK_ERROR ${error.javaClass.simpleName}")
        onFailure()
    }

    private fun LocationFix.hasValidCoordinates() =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun update(value: TrackingStatus) {
        if (mutableStatus.value == value) return
        mutableStatus.value = value
        log("session=$generation STATE $value")
    }
}
