package com.jinkim.hyundaiAndGretel.notes

import org.junit.Assert.*
import org.junit.Test

class PickupTest {
    private val note = RoadNote("n", "r", "s", Actor.A.id, "pothole", Category.WARNING, 0, 0, 600000,
        DemoRoute.LATITUDE, DemoRoute.longitudeAt(300.0), 90f, 30000, "cell", 300.0)
    private fun location(progress: Double, direction: String = "FORWARD") =
        LocationSample(DemoRoute.LATITUDE, DemoRoute.longitudeAt(progress), 1000, progress, 90f, direction)
    @Test fun excludesBehindExpiredSelfStaleAndOppositeDirection() {
        assertTrue(eligible(note, Actor.B, location(200.0), 1000, 1000))
        assertFalse(eligible(note, Actor.B, location(100.0), 1000, 1000))
        assertFalse(eligible(note, Actor.B, location(301.0), 1000, 1000))
        assertFalse(eligible(note, Actor.A, location(200.0), 1000, 1000))
        assertFalse(eligible(note, Actor.B, location(200.0), 600000, 1000))
        assertFalse(eligible(note, Actor.B, location(200.0), 1000, 6001))
        assertFalse(eligible(note, Actor.B, location(200.0, "REVERSE"), 1000, 1000))
        assertTrue(eligible(note.copy(direction="REVERSE"), Actor.B, location(400.0, "REVERSE"), 1000, 1000))
    }
}
