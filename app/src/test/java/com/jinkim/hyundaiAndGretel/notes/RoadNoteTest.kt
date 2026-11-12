package com.jinkim.hyundaiAndGretel.notes

import org.junit.Assert.*
import org.junit.Test

class RoadNoteTest {
    private fun note() = RoadNote(
        noteId = "00000000-0000-0000-0000-000000000001", authorId = "A",
        content = "도로에 구덩이가 있어요", category = NoteCategory.WARNING,
        latitude = 37.4979, longitude = 127.0276, anchorOffsetMillis = 10_000,
        createdAtUtcMillis = 1_000, expiresAtUtcMillis = 601_000,
    )

    @Test fun unicodeLengthUsesCodePointsWithoutTruncating() {
        val emoji = "\uD83D\uDE80"
        for (content in listOf("가".repeat(500), emoji.repeat(500), "👩‍🚀".repeat(166) + "가나")) {
            assertEquals(500, content.codePointCount(0, content.length))
            assertNull(note().copy(content = content).validationError())
        }
        assertEquals(InvalidNote.CONTENT, note().copy(content = emoji.repeat(501)).validationError())
        assertEquals(InvalidNote.CONTENT, note().copy(content = "").validationError())
    }

    @Test fun rejectsNonFiniteOutOfRangeCoordinatesAndOffsets() {
        for (latitude in listOf(Double.NaN, Double.POSITIVE_INFINITY, 90.01, -90.01)) {
            assertEquals(InvalidNote.COORDINATES, note().copy(latitude = latitude).validationError())
        }
        assertEquals(InvalidNote.COORDINATES, note().copy(longitude = 180.01).validationError())
        for (offset in listOf(-1L, 35_001L)) {
            assertEquals(InvalidNote.OFFSET, note().copy(anchorOffsetMillis = offset).validationError())
        }
        assertNull(note().copy(latitude = -90.0, longitude = 180.0, anchorOffsetMillis = 35_000).validationError())
    }

    @Test fun enforcesExactTtlAndRejectsOverflow() {
        assertNull(note().validationError())
        assertEquals(InvalidNote.TIME, note().copy(expiresAtUtcMillis = 999).validationError())
        assertEquals(InvalidNote.TIME, note().copy(expiresAtUtcMillis = 601_001).validationError())
        assertEquals(InvalidNote.TIME, note().copy(createdAtUtcMillis = Long.MAX_VALUE).validationError())
        assertNull(note().copy(createdAtUtcMillis = Long.MAX_VALUE - RoadNote.TTL_MILLIS,
            expiresAtUtcMillis = Long.MAX_VALUE).validationError())
    }

    @Test fun expiresAtIsExclusive() {
        assertTrue(note().isActiveAt(600_999))
        assertFalse(note().isActiveAt(601_000))
        assertFalse(note().isActiveAt(601_001))
    }

    @Test fun identifiersCannotBeEmptyOrAmbiguous() {
        assertEquals(InvalidNote.ID, note().copy(noteId = "1-1-1-1-1").validationError())
        assertEquals(InvalidNote.AUTHOR, note().copy(authorId = " ").validationError())
    }
}
