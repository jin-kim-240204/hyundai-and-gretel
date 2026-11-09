package com.jinkim.hyundaiAndGretel.notes

import org.junit.Assert.*
import org.junit.Test

class RoutePlotTest {
    @Test fun movingCarMovesOnStableAxisWhileNoteStays() {
        val first = routePlotRange(0.0, listOf(300.0))
        val second = routePlotRange(200.0, listOf(300.0))
        assertEquals(first, second)
        assertTrue(second.fraction(200.0) > first.fraction(0.0))
        assertEquals(first.fraction(300.0), second.fraction(300.0), 0.0001f)
    }
    @Test fun bSixHundredMetersBehindNoteAndFarNotesAreVisible() {
        val range = routePlotRange(-300.0, listOf(300.0, 2500.0))
        for (progress in listOf(-300.0,300.0,2500.0)) assertTrue(range.fraction(progress) in 0f..1f)
        assertTrue(range.fraction(-300.0) < range.fraction(300.0))
        val overlap = range.fraction(300.0)
        assertEquals(overlap, range.fraction(300.0), 0f)
    }
}
