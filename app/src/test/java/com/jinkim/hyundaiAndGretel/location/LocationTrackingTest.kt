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
        override fun start(onLocation: (LocationFix?) -> Unit, onError: (Exception) -> Unit): Boolean {
            starts++
            location = onLocation
            error = onError
            startError?.let { throw it }
            return supported
        }
        override fun stop() { stops++; stopError?.let { throw it } }
    }

    @Test fun duplicateStartAndInvalidLocationsKeepOneSubscription() = runTest {
        val source = FakeSource()
        val logs = mutableListOf<String>()
        val tracking = LocationTracking(this, { source }, logs::add, {})
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
        val tracking = LocationTracking(this, { sources.removeFirst() }, {}, {})
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
        val tracking = LocationTracking(this, { source }, {}, {})
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
        val tracking = LocationTracking(this, { source }, {}, { failures++ })
        tracking.start(); source.error(IllegalStateException("sdk")); runCurrent()
        assertEquals(TrackingStatus.FAILED, tracking.status.value)
        assertEquals(1, failures)
        tracking.stop()
        assertEquals(TrackingStatus.STOPPED, tracking.status.value)
        assertEquals(1, source.stops)
    }

    @Test fun unsupportedAndInitializationFailureAreDistinct() = runTest {
        val unsupported = FakeSource().apply { supported = false }
        val tracking = LocationTracking(this, { unsupported }, {}, {})
        tracking.start()
        assertEquals(TrackingStatus.UNSUPPORTED, tracking.status.value)
        assertEquals(1, unsupported.stops)
        val broken = FakeSource().apply { startError = IllegalStateException() }
        val other = LocationTracking(this, { broken }, {}, {})
        other.start()
        assertEquals(TrackingStatus.FAILED, other.status.value)
        assertEquals(1, broken.stops)
    }

    @Test fun cancellationIsRethrownInsteadOfReportedAsSdkFailure() = runTest {
        val source = FakeSource().apply { startError = CancellationException() }
        var failures = 0
        val tracking = LocationTracking(this, { source }, {}, { failures++ })
        try {
            tracking.start()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(TrackingStatus.STOPPED, tracking.status.value)
            assertEquals(1, source.stops)
            assertEquals(0, failures)
        }
    }
}
