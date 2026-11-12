package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.notes.*
import org.junit.Assert.*
import org.junit.Test

class SyntheticRouteTest {
    @Test fun projectionRejectsUnknownOrOutsideAndKeepsSignedDirection() {
        val east = SyntheticSession()
        val a = SyntheticRoute.project(37.5, 127.03, east)!!
        val b = SyntheticRoute.project(37.5, 127.031, east)!!
        assertTrue(SyntheticRoute.ahead(b, a)!! > 0)
        assertTrue(SyntheticRoute.ahead(b.copy(direction = -1), a.copy(direction = -1))!! < 0)
        assertNull(SyntheticRoute.ahead(b, a.copy(direction = -1)))
        assertNull(SyntheticRoute.project(37.501, 127.03, east))
        assertNull(SyntheticRoute.project(37.5, 127.019, east))
        assertNull(SyntheticRoute.project(37.5, 127.03, east.copy(routeId = "R2")))
        assertNull(SyntheticRoute.project(Double.NaN, 127.03, east))
        assertTrue(SyntheticRoute.matches(a, 37.5, 127.03))
        assertFalse(SyntheticRoute.matches(a.copy(progressMeters = a.progressMeters + 1), 37.5, 127.03))
    }

    @Test fun legacyNullIsValidButContradictoryMetadataIsRejected() {
        val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
            37.5, 127.03, 0, 0, RoadNote.TTL_MILLIS)
        assertNull(note.validationError())
        val route = SyntheticRoute.project(note.latitude, note.longitude, SyntheticSession())!!
        assertNull(note.copy(route = route).validationError())
        assertEquals(InvalidNote.ROUTE, note.copy(route = route.copy(direction = 0)).validationError())
        assertEquals(InvalidNote.ROUTE, note.copy(route = route.copy(progressMeters = Double.NaN)).validationError())
    }
}
