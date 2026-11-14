package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.notes.*
import org.junit.Assert.*
import org.junit.Test

class SyntheticRouteTest {
    @Test fun generatedCoordinatesProjectAtBothEndpoints() {
        val setup = SyntheticSession()
        for (progress in listOf(0.0, 610.0, SyntheticRoute.endProgressMeters)) {
            val fix = SyntheticRoute.fixAt(progress, 42)
            val route = requireNotNull(SyntheticRoute.project(fix.latitude, fix.longitude, setup))
            assertEquals(progress, route.progressMeters, 1e-6)
            assertEquals(42L, fix.receivedElapsedMillis)
        }
    }

    @Test fun projectionRejectsUnknownOrOutside() {
        val setup = SyntheticSession()
        val a = SyntheticRoute.project(37.5, 127.03, setup)!!
        assertNull(SyntheticRoute.project(37.501, 127.03, setup))
        assertNull(SyntheticRoute.project(37.5, 127.019, setup))
        assertNull(SyntheticRoute.project(37.5, 127.03, setup.copy(routeId = "R2")))
        assertNull(SyntheticRoute.project(Double.NaN, 127.03, setup))
        assertTrue(SyntheticRoute.matches(a, 37.5, 127.03))
        assertFalse(SyntheticRoute.matches(a.copy(progressMeters = a.progressMeters + 1), 37.5, 127.03))
    }

    @Test fun routeMetadataValidationIsSeparateFromNoteValidation() {
        val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
            37.5, 127.03, 0, 0, RoadNote.TTL_MILLIS)
        assertNull(note.validationError())
        val route = SyntheticRoute.project(note.latitude, note.longitude, SyntheticSession())!!
        assertTrue(note.copy(route = route).hasValidRouteMetadata())
        assertFalse(note.copy(route = route.copy(routeId = "R2")).hasValidRouteMetadata())
        assertFalse(note.copy(route = route.copy(progressMeters = Double.NaN)).hasValidRouteMetadata())
        assertNull(note.copy(route = route.copy(routeId = "R2")).validationError())
    }
}
