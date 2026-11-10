package com.jinkim.hyundaiAndGretel.notes

import org.junit.Assert.*
import org.junit.Test

class ProximityCandidatesTest {
    private val location = LocationSample(DemoRoute.LATITUDE, DemoRoute.LONGITUDE, 1000, 0.0, 90f)
    private fun note(progress: Double, direction: String = "FORWARD") = RoadNote(
        "note-$progress", "request-$progress", "scenario", Actor.A.id, "pothole", Category.WARNING,
        0, 0, 600_000, DemoRoute.LATITUDE, DemoRoute.longitudeAt(progress), 90f, 0, "cell", progress,
        direction = direction,
    )

    @Test fun bothDirectionsGiveTheSixteenNearestAlertsPriority() {
        for (direction in listOf("FORWARD", "REVERSE")) {
            val sign = if (direction == "FORWARD") 1 else -1
            val notes = (17 downTo 1).map { note(it * 10.0 * sign, direction) }
            val selected = proximityCandidates(notes, Actor.B, location.copy(direction = direction), emptySet(), 1000, 1000)
            assertEquals((1..17).map { it * 10.0 * sign }, selected.map { it.progressMeters })
            assertFalse(selected.take(16).any { it.progressMeters == 170.0 * sign })
        }
    }

    @Test fun excludesReceivedSelfExpiredBehindOppositeAndStaleLocations() {
        val valid = note(100.0)
        val notes = listOf(valid, note(-10.0), note(120.0, "REVERSE"),
            note(130.0).copy(authorId = Actor.B.id), note(140.0).copy(expiresAt = 1000))
        assertEquals(listOf(valid), proximityCandidates(notes, Actor.B, location, emptySet(), 1000, 1000))
        assertTrue(proximityCandidates(notes, Actor.B, location, setOf(valid.id), 1000, 1000).isEmpty())
        assertTrue(proximityCandidates(notes, Actor.A, location, emptySet(), 1000, 1000).isEmpty())
        assertTrue(proximityCandidates(notes, Actor.B, null, emptySet(), 1000, 1000).isEmpty())
        assertTrue(proximityCandidates(notes, Actor.B, location, emptySet(), 1000, 6001).isEmpty())
        assertTrue(proximityCandidates(notes, Actor.B, location, emptySet(), 1000, 999).isEmpty())
    }
}
