package com.jinkim.hyundaiAndGretel.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationFix(val latitude: Double, val longitude: Double, val receivedElapsedMillis: Long)

enum class TrackingStatus { STOPPED, STARTING, WAITING, RECEIVING, UNAVAILABLE, UNSUPPORTED, FAILED }

interface LocationSource {
    fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean
    fun stop()
}

/** Commands and callback delivery are serialized on the owner's Main scope. */
class LocationTracking(
    private val scope: CoroutineScope,
    private val createSource: () -> LocationSource,
    private val log: (String) -> Unit,
    private val onFailure: () -> Unit,
    private val clock: () -> Long,
) {
    private val mutableStatus = MutableStateFlow(TrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private var source: LocationSource? = null
    private var generation = 0L
    private val buffer = LocationBuffer()

    fun start() {
        if (source != null) return
        buffer.reset()
        val session = ++generation
        update(TrackingStatus.STARTING)
        try {
            val current = createSource()
            source = current
            val supported = current.start(
                onLocation = { fix, receivedAt ->
                    scope.launch {
                        if (generation != session) return@launch
                        when (buffer.offer(fix, receivedAt, clock())) {
                            LocationBuffer.Update.IGNORED -> Unit
                            LocationBuffer.Update.INVALIDATED -> update(TrackingStatus.UNAVAILABLE)
                            LocationBuffer.Update.ACCEPTED -> {
                                update(TrackingStatus.RECEIVING)
                                log("session=$session LOCATION lat=${fix?.latitude} lon=${fix?.longitude} elapsed=$receivedAt")
                                log("session=$session BUFFER_APPEND size=${buffer.size} oldest=${buffer.oldestElapsedMillis} latest=${buffer.latestElapsedMillis}")
                            }
                        }
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
        buffer.reset()
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

    /** Called on the tracking context; reading the clock and selecting never suspends. */
    fun selectAnchor(): AnchorSelection = buffer.select(clock())

    private fun update(value: TrackingStatus) {
        if (mutableStatus.value == value) return
        mutableStatus.value = value
        log("session=$generation STATE $value")
    }
}
