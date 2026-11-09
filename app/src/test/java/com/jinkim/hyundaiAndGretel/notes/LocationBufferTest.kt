package com.jinkim.hyundaiAndGretel.notes

import org.junit.Assert.*
import org.junit.Test

class LocationBufferTest {
    private fun sample(t: Long) = LocationSample(37.4979, 127.0276, t, 0.0, 90f)
    @Test fun irregularCallbacksKeepPreTargetSampleAndChooseNearest() {
        val buffer = LocationBuffer()
        listOf(0L, 4000, 8000, 9000, 13000, 17000, 21000, 25000, 29000, 33000, 37000, 40000).forEach { buffer.add(sample(it)) }
        assertEquals(31_000L, buffer.anchor(40_000)!!.offsetMillis)
        assertEquals(1_000L, buffer.anchor(40_000)!!.errorMillis)
        assertTrue(buffer.snapshot(40_000).any { it.elapsedMillis == 9000L })
    }
    @Test fun notReadyStaleGapAndResetCannotUseOldHistory() {
        val buffer = LocationBuffer()
        (0L..30_000L step 1000).forEach { buffer.add(sample(it)) }
        assertNotNull(buffer.anchor(30_000))
        assertNull(buffer.anchor(35_001))
        buffer.add(sample(36_000))
        assertNull(buffer.anchor(36_000))
        buffer.clear()
        assertTrue(buffer.snapshot(36_000).isEmpty())
    }
    @Test fun tieChoosesOlderAndRetentionIsTimeBased() {
        val samples = listOf(sample(0), sample(9000), sample(11000), sample(40000))
        assertEquals(31_000, selectAnchor(samples, 40_000)!!.offsetMillis)
        val buffer = LocationBuffer()
        (0L..60_000L step 1000).forEach { buffer.add(sample(it)) }
        assertEquals(15_000L, buffer.snapshot(60_000).first().elapsedMillis)
    }
    @Test fun routeRejectsInvalidAndOffRouteAndConvertsMeters() {
        assertNull(DemoRoute.progress(Double.NaN, 127.0))
        assertNull(DemoRoute.progress(38.0, 127.0))
        assertEquals(300.0, DemoRoute.progress(DemoRoute.LATITUDE, DemoRoute.longitudeAt(300.0))!!, 0.01)
        assertEquals(150.0, DemoRoute.distance(DemoRoute.LATITUDE, DemoRoute.LONGITUDE, DemoRoute.LATITUDE, DemoRoute.longitudeAt(150.0)), 0.1)
    }
}
