package com.jinkim.hyundaiAndGretel.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TrackingStatus {
    STOPPED, STARTING, WAITING, RECEIVING, UNAVAILABLE, UNSUPPORTED, FAILED;

    val isCollecting: Boolean get() = when (this) {
        STARTING, WAITING, RECEIVING, UNAVAILABLE -> true
        STOPPED, UNSUPPORTED, FAILED -> false
    }
}

class LocationTracking(
    private val scope: CoroutineScope,
    private val createSource: () -> LocationSource,
    private val log: (String) -> Unit,
    private val onFailure: () -> Unit,
    private val clock: () -> Long,
    private val acceptLocation: (LocationFix?) -> Boolean = { true },
) {
    private val mutableStatus = MutableStateFlow(TrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private val mutablePosition = MutableStateFlow<TrackedPosition?>(null)
    val currentPosition = mutablePosition.asStateFlow()
    private var source: LocationSource? = null
    private var generation = 0L
    val sessionId get() = generation
    val proximitySource: ProximitySource? get() = source
    fun ownsSource(owner: ProximitySource): Boolean = source === owner
    private val buffer = LocationBuffer()

    fun start() {
        if (source != null) return
        val session = prepareSession()
        try {
            val current = createSource()
            source = current
            val isSupported = registerCallbacks(current, session)
            handleRegistration(isSupported, session)
        } catch (cancelled: CancellationException) {
            stop()
            throw cancelled
        } catch (error: Exception) {
            fail(error)
        }
    }

    private fun prepareSession(): Long {
        mutablePosition.value = null
        buffer.reset()
        val session = ++generation
        update(TrackingStatus.STARTING)
        return session
    }

    private fun registerCallbacks(current: LocationSource, session: Long): Boolean = current.start(
        onLocation = { fix, receivedElapsedMillis ->
            scope.launch { handleLocation(session, fix, receivedElapsedMillis) }
        },
        onError = { error ->
            scope.launch { handleError(session, error) }
        },
    )

    private fun handleRegistration(isSupported: Boolean, session: Long) {
        if (!isSupported) {
            stop()
            update(TrackingStatus.UNSUPPORTED)
            onFailure()
            return
        }
        log("session=$session REGISTERED")
        update(TrackingStatus.WAITING)
    }

    private fun handleLocation(session: Long, fix: LocationFix?, receivedElapsedMillis: Long) {
        if (generation != session) return
        if (!acceptLocation(fix)) return
        when (buffer.offer(fix = fix, receivedElapsedMillis = receivedElapsedMillis, nowElapsedMillis = clock())) {
            LocationBuffer.Update.IGNORED -> Unit
            LocationBuffer.Update.INVALIDATED -> {
                mutablePosition.value = null
                update(TrackingStatus.UNAVAILABLE)
            }
            LocationBuffer.Update.ACCEPTED -> {
                mutablePosition.value = TrackedPosition(session, checkNotNull(fix))
                update(TrackingStatus.RECEIVING)
                log("session=$session LOCATION lat=${fix?.latitude} lon=${fix?.longitude} elapsed=$receivedElapsedMillis")
                log("session=$session BUFFER_APPEND size=${buffer.size} oldest=${buffer.oldestElapsedMillis} latest=${buffer.latestElapsedMillis}")
            }
        }
    }

    private fun handleError(session: Long, error: Exception) {
        if (generation == session) fail(error)
    }

    fun stop() {
        mutablePosition.value = null
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

    fun fail(error: Exception, operation: String = "SDK") {
        if (error is CancellationException) {
            stop()
            throw error
        }
        stop()
        update(TrackingStatus.FAILED)
        log("session=$generation ${operation}_ERROR ${error.javaClass.simpleName}")
        onFailure()
    }

    fun bufferSnapshot() = LocationBufferSnapshot(buffer.size, buffer.oldestElapsedMillis, buffer.latestElapsedMillis)

    fun selectAnchor(): AnchorSelection = buffer.select(clock())
    fun selectAnchorAt(receivedElapsedMillis: Long): AnchorSelection = buffer.selectAt(receivedElapsedMillis, clock())

    private fun update(value: TrackingStatus) {
        if (mutableStatus.value == value) return
        mutableStatus.value = value
        log("session=$generation STATE $value")
    }
}
