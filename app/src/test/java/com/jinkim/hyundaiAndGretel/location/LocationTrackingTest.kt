package com.jinkim.hyundaiAndGretel.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackingTest {
    private class FakeSource : LocationSource {
        var starts = 0
        var stops = 0
        var supported = true
        var startError: Exception? = null
        var stopError: Exception? = null
        lateinit var location: (LocationFix?) -> Unit
        lateinit var error: (Exception) -> Unit
        lateinit var event: (LocationFix?, Long) -> Unit
        override fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean {
            starts++
            event = onLocation
            location = { fix -> onLocation(fix, fix?.receivedElapsedMillis ?: 0L) }
            error = onError
            startError?.let { throw it }
            return supported
        }
        override fun stop() { stops++; stopError?.let { throw it } }
    }

    @Test fun currentPositionCarriesSessionAndCannotSurviveInvalidationOrRestart() = runTest {
        val first = FakeSource(); val second = FakeSource()
        val sources = ArrayDeque(listOf(first, second))
        val tracking = LocationTracking(this, { sources.removeFirst() }, {}, {}, { 1000L })
        tracking.start()
        first.location(LocationFix(37.5, 127.03, 1))
        runCurrent()
        val original = checkNotNull(tracking.currentPosition.value)
        first.event(null, 2)
        runCurrent()
        assertNull(tracking.currentPosition.value)
        first.location(original.fix)
        runCurrent()
        assertNull(tracking.currentPosition.value)
        tracking.stop()
        tracking.start()
        second.location(LocationFix(37.5, 127.03, 3))
        runCurrent()
        val restarted = checkNotNull(tracking.currentPosition.value)
        assertTrue(restarted.session > original.session)
        first.location(LocationFix(38.0, 128.0, 4))
        runCurrent()
        assertEquals(restarted, tracking.currentPosition.value)
        tracking.stop()
        assertNull(tracking.currentPosition.value)
    }

    @Test fun duplicateStartAndInvalidLocationsKeepOneSubscription() = runTest {
        val source = FakeSource()
        val logs = mutableListOf<String>()
        val tracking = LocationTracking(this, { source }, logs::add, {}, { 1000L })
        tracking.start(); tracking.start()
        assertEquals(1, source.starts)
        for (fix in listOf(null, LocationFix(Double.NaN, 0.0, 1), LocationFix(91.0, 0.0, 1), LocationFix(0.0, 181.0, 1))) {
            source.location(fix); runCurrent()
            assertEquals(TrackingStatus.UNAVAILABLE, tracking.status.value)
        }
        assertEquals(0, source.stops)
        source.location(LocationFix(0.0, 0.0, 7)); runCurrent()
        assertEquals(TrackingStatus.RECEIVING, tracking.status.value)
        assertTrue(logs.any { "elapsed=7" in it })
        tracking.stop()
    }

    @Test fun queuedAndLateCallbacksCannotChangeStoppedOrNewSession() = runTest {
        val first = FakeSource(); val second = FakeSource()
        val sources = ArrayDeque(listOf(first, second))
        val tracking = LocationTracking(this, { sources.removeFirst() }, {}, {}, { 1000L })
        tracking.start()
        first.location(LocationFix(1.0, 2.0, 3))
        tracking.stop(); runCurrent()
        assertEquals(TrackingStatus.STOPPED, tracking.status.value)
        tracking.start()
        first.location(null); first.error(IllegalStateException()); runCurrent()
        assertEquals(TrackingStatus.WAITING, tracking.status.value)
        assertEquals(1, first.stops)
        assertEquals(1, second.starts)
        tracking.stop()
    }

    @Test fun generationRejectsOldCallbacksEvenWhenSourceInstanceIsReused() = runTest {
        val source = FakeSource()
        val tracking = LocationTracking(this, { source }, {}, {}, { 1000L })
        tracking.start()
        val oldLocation = source.location
        val oldError = source.error
        tracking.stop()
        tracking.start()
        oldLocation(null)
        oldError(IllegalStateException("previous session"))
        runCurrent()
        assertEquals(TrackingStatus.WAITING, tracking.status.value)
        source.location(LocationFix(1.0, 2.0, 3))
        runCurrent()
        assertEquals(TrackingStatus.RECEIVING, tracking.status.value)
        tracking.stop()
    }

    @Test fun sdkFailureCleansUpButCleanupErrorDoesNotReactivate() = runTest {
        val source = FakeSource().apply { stopError = IllegalStateException("cleanup") }
        var failures = 0
        val tracking = LocationTracking(this, { source }, {}, { failures++ }, { 1000L })
        tracking.start()
        source.location(LocationFix(37.0, 127.0, 1)); runCurrent()
        assertTrue(tracking.selectAnchor() is AnchorSelection.Selected)
        source.error(IllegalStateException("sdk")); runCurrent()
        assertEquals(AnchorSelection.Unavailable.EMPTY, tracking.selectAnchor())
        assertEquals(TrackingStatus.FAILED, tracking.status.value)
        assertEquals(1, failures)
        tracking.stop()
        assertEquals(TrackingStatus.STOPPED, tracking.status.value)
        assertEquals(1, source.stops)
    }

    @Test fun unsupportedAndInitializationFailureAreDistinct() = runTest {
        val unsupported = FakeSource().apply { supported = false }
        val tracking = LocationTracking(this, { unsupported }, {}, {}, { 1000L })
        tracking.start()
        assertEquals(TrackingStatus.UNSUPPORTED, tracking.status.value)
        assertEquals(1, unsupported.stops)
        val broken = FakeSource().apply { startError = IllegalStateException() }
        val other = LocationTracking(this, { broken }, {}, {}, { 1000L })
        other.start()
        assertEquals(TrackingStatus.FAILED, other.status.value)
        assertEquals(1, broken.stops)
    }

    @Test fun cancellationIsRethrownInsteadOfReportedAsSdkFailure() = runTest {
        val source = FakeSource().apply { startError = CancellationException() }
        var failures = 0
        val tracking = LocationTracking(this, { source }, {}, { failures++ }, { 1000L })
        try {
            tracking.start()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(TrackingStatus.STOPPED, tracking.status.value)
            assertEquals(1, source.stops)
            assertEquals(0, failures)
        }
    }
    @Test fun bufferInvalidationAndQueryUseTheTrackingContextClock() = runTest {
        val source = FakeSource()
        var now = 100_000L
        val tracking = LocationTracking(this, { source }, {}, {}, { now })
        tracking.start()
        for (at in 100_000L..140_000L step 1_000) {
            now = at
            source.event(LocationFix(37.0, 127.0, at), at)
            runCurrent()
        }
        // An external request waits; the newer callback is processed before selection begins.
        now = 141_000
        source.event(LocationFix(37.0, 127.0, now), now)
        runCurrent()
        now = 142_000
        val chosen = tracking.selectAnchor() as AnchorSelection.Selected
        assertEquals(112_000L, chosen.anchor.sample.receivedElapsedMillis)
        now = 143_000
        source.event(null, 143_000)
        source.event(LocationFix(37.0, 127.0, 142_000), 142_000)
        runCurrent()
        assertEquals(AnchorSelection.Unavailable.EMPTY, tracking.selectAnchor())
        now = 144_000
        source.event(LocationFix(37.0, 127.0, now), now)
        runCurrent()
        assertEquals(0L, (tracking.selectAnchor() as AnchorSelection.Selected).anchor.offsetMillis)
        val oldEvent = source.event
        tracking.stop(); tracking.start()
        oldEvent(LocationFix(37.0, 127.0, now), now)
        runCurrent()
        assertEquals(AnchorSelection.Unavailable.EMPTY, tracking.selectAnchor())
        tracking.stop()
    }

    @Test fun queuedErrorIsCheckedWhenHandledNotWhenReceived() = runTest {
        val source = FakeSource()
        var failures = 0
        val tracking = LocationTracking(this, { source }, {}, { failures++ }, { 1000L })
        tracking.start()
        source.error(IllegalStateException("queued before stop"))
        tracking.stop()
        tracking.start()
        runCurrent()
        assertEquals(TrackingStatus.WAITING, tracking.status.value)
        assertEquals(0, failures)
        assertEquals(1, source.stops)
        tracking.stop()
    }

    @Test fun foregroundFailureKeepsFailureStateButNotTheSdkErrorLabel() = runTest {
        val logs = mutableListOf<String>()
        var failures = 0
        val tracking = LocationTracking(this, { FakeSource() }, logs::add, { failures++ }, { 1000L })
        tracking.fail(SecurityException("foreground denied"), operation = "FOREGROUND_START")
        assertEquals(TrackingStatus.FAILED, tracking.status.value)
        assertEquals(1, failures)
        assertTrue(logs.any { "FOREGROUND_START_ERROR" in it })
        assertFalse(logs.any { "SDK_ERROR" in it })
    }

}
