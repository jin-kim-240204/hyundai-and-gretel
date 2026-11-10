package com.jinkim.hyundaiAndGretel.location

import org.junit.Assert.*
import org.junit.Test

class LocationBufferTest {
    private fun fix(at: Long, lat: Double = 37.0) = LocationFix(lat, 127.0, at)
    private fun LocationBuffer.add(at: Long) = offer(fix(at), at, at)
    private fun LocationBuffer.anchor(now: Long) = (select(now) as AnchorSelection.Selected).anchor

    @Test fun emptyFirstAndShortHistory() {
        val buffer = LocationBuffer()
        assertEquals(AnchorSelection.Unavailable.EMPTY, buffer.select(100_000))
        buffer.add(100_000)
        assertEquals(0L, buffer.anchor(100_000).offsetMillis)
        buffer.add(110_000)
        assertEquals(10_000L, buffer.anchor(110_000).offsetMillis)
    }

    @Test fun retentionUsesTimeAndIncludesTheFortyFiveSecondBoundary() {
        val buffer = LocationBuffer()
        for (at in 0L..80_000L step 1_000) buffer.add(at)
        assertEquals(35_000L, buffer.oldestElapsedMillis)
        assertEquals(46, buffer.size)
        assertEquals(50_000L, buffer.anchor(80_000).sample.receivedElapsedMillis)
        buffer.offer(fix(80_001), 80_001, 80_001)
        assertEquals(36_000L, buffer.oldestElapsedMillis)
    }

    @Test fun tiesChooseTheOlderActualSample() {
        val buffer = LocationBuffer()
        for (at in listOf(100_000L, 108_000, 112_000, 140_000)) buffer.add(at)
        assertEquals(108_000L, buffer.anchor(140_000).sample.receivedElapsedMillis)
    }

    @Test fun staleQueryAndSixSecondGapKeepHistoricalSamples() {
        val buffer = LocationBuffer()
        for (at in 100_000L..140_000L step 1_000) buffer.add(at)
        assertTrue(buffer.select(145_000) is AnchorSelection.Selected)
        assertEquals(AnchorSelection.Unavailable.STALE, buffer.select(146_000))
        assertTrue(buffer.size > 1)
        buffer.add(146_000)
        assertEquals(116_000L, buffer.anchor(146_000).sample.receivedElapsedMillis)
        assertEquals(30_000L, buffer.anchor(146_000).offsetMillis)
    }

    @Test fun targetToleranceIsIndependentOfLatestFreshness() {
        for ((oldest, accepted) in listOf(125_000L to true, 124_999L to false)) {
            val buffer = LocationBuffer()
            buffer.add(oldest); buffer.add(160_000)
            assertEquals(accepted, buffer.select(160_000) is AnchorSelection.Selected)
            if (!accepted) assertEquals(AnchorSelection.Unavailable.NO_TARGET_SAMPLE, buffer.select(160_000))
        }
    }

    @Test fun nullAndInvalidCoordinatesInvalidateButSameSessionCanRecover() {
        val buffer = LocationBuffer()
        buffer.add(100)
        assertEquals(LocationBuffer.Update.INVALIDATED, buffer.offer(null, 105, 105))
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(104), 104, 106))
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(105), 105, 106))
        assertEquals(0, buffer.size)
        buffer.add(106)
        assertEquals(106L, buffer.anchor(106).sample.receivedElapsedMillis)
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(null, 105, 107))
        assertEquals(1, buffer.size)
        buffer.offer(null, 106, 107)
        assertEquals(0, buffer.size)
        buffer.offer(fix(108, Double.NaN), 108, 108)
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(107), 107, 109))
        buffer.add(109)
        assertEquals(1, buffer.size)
    }

    @Test fun pruningDoesNotForgetInvalidationAndNewSessionResetsIt() {
        val buffer = LocationBuffer()
        buffer.offer(null, 100, 100)
        assertEquals(AnchorSelection.Unavailable.EMPTY, buffer.select(50_000))
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(100), 100, 50_000))
        buffer.reset()
        buffer.add(100)
        assertEquals(1, buffer.size)
    }

    @Test fun timeAndCoordinateBoundariesRejectInvalidDataWithoutDestroyingValidHistory() {
        val buffer = LocationBuffer()
        buffer.add(100)
        for (at in listOf(-1L, 99L, 100L, 201L)) {
            assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(at), at, 200))
        }
        assertEquals(AnchorSelection.Unavailable.INVALID_TIME, buffer.select(99))
        assertEquals(AnchorSelection.Unavailable.INVALID_TIME, buffer.select(-1))
        assertEquals(1, buffer.size)
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(null, 201, 200))
        assertEquals(1, buffer.size)
        for ((lat, lon) in listOf(91.0 to 0.0, 0.0 to 181.0, Double.POSITIVE_INFINITY to 0.0)) {
            buffer.offer(LocationFix(lat, lon, 101), 101, 101)
            assertEquals(0, buffer.size)
        }
    }

    @Test fun stationarySamplesAreNotMergedAndReturnedAnchorIsImmutable() {
        val buffer = LocationBuffer()
        buffer.add(100); buffer.add(101)
        assertEquals(2, buffer.size)
        val anchor = buffer.anchor(101)
        buffer.reset(); buffer.add(102)
        assertEquals(100L, anchor.sample.receivedElapsedMillis)
        assertEquals(1L, anchor.offsetMillis)
    }

    @Test fun delayedSampleCannotBeFreshAndExpiredHistoryCannotBeSelected() {
        val buffer = LocationBuffer()
        buffer.offer(fix(100), 100, 6_000)
        assertEquals(AnchorSelection.Unavailable.STALE, buffer.select(6_000))
        assertEquals(AnchorSelection.Unavailable.EMPTY, buffer.select(45_101))
        assertEquals(LocationBuffer.Update.IGNORED, buffer.offer(fix(101), 101, 50_000))
    }
}
