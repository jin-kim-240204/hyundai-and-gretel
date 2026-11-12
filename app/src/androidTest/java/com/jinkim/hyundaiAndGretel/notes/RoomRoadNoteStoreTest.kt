package com.jinkim.hyundaiAndGretel.notes

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialUnavailableException
import com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase
import com.jinkim.hyundaiAndGretel.notes.storage.RoomRoadNoteStore
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RoomRoadNoteStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun note() = RoadNote(
        noteId = "00000000-0000-0000-0000-000000000001", authorId = "A",
        content = "  도로에 구덩이가 있어요 🚀  ", category = NoteCategory.WARNING,
        latitude = 37.4979, longitude = 127.0276, anchorOffsetMillis = 10_000,
        createdAtUtcMillis = 1_000, expiresAtUtcMillis = 601_000,
    )

    @Test fun concurrentDuplicateIdsNeverReturnExistingAsSuccess() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build().withDatabase { db ->
            val store = RoomRoadNoteStore(db)
            val results = coroutineScope {
                List(12) { async(Dispatchers.Default) { store.create(note()) } }.awaitAll()
            }
            assertEquals(1, results.count { it is CreateNoteResult.Created })
            assertEquals(11, results.count { it == CreateNoteResult.Duplicate })
            val original = store.findById(note().noteId)!!
            assertEquals(note().content.trim(), original.content)
            assertEquals(CreateNoteResult.Duplicate, store.create(note().copy(
                content = "다른 내용", category = NoteCategory.INFORMATION, latitude = 38.0,
                createdAtUtcMillis = 2_000, expiresAtUtcMillis = 602_000,
            )))
            assertEquals(original, store.findById(note().noteId))
            assertEquals(1, rowCount(db))
        }
    }

    @Test fun directInvalidWritesAreRejectedWithoutRows() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build().withDatabase { db ->
            val store = RoomRoadNoteStore(db)
            for (invalid in listOf(note().copy(content = " \n "), note().copy(content = "🚀".repeat(501)),
                note().copy(latitude = Double.NaN), note().copy(anchorOffsetMillis = -1),
                note().copy(createdAtUtcMillis = Long.MAX_VALUE))) {
                assertTrue(store.create(invalid) is CreateNoteResult.Invalid)
            }
            assertEquals(0, rowCount(db))
            val content = "🚀".repeat(500)
            val result = store.create(note().copy(content = " $content ")) as CreateNoteResult.Created
            assertEquals(content, result.note.content)
            assertEquals(result.note, store.findById(result.note.noteId))
        }
    }

    @Test fun originalSurvivesDatabaseReopenEvenWhenExpired() = runBlocking {
        val name = "road-note-reopen-test.db"
        context.deleteDatabase(name)
        try {
            val original = note().copy(content = note().content.trim())
            Room.databaseBuilder(context, NoteDatabase::class.java, name).build().withDatabase { db ->
                assertEquals(CreateNoteResult.Created(original), RoomRoadNoteStore(db).create(note()))
            }
            Room.databaseBuilder(context, NoteDatabase::class.java, name).build().withDatabase { db ->
                assertEquals(original, RoomRoadNoteStore(db).findById(original.noteId))
                assertFalse(original.isActiveAt(original.expiresAtUtcMillis))
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun competingDifferentPayloadsKeepOnlyTheWinner() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build().withDatabase { db ->
            val store = RoomRoadNoteStore(db)
            val results = coroutineScope {
                listOf(note(), note().copy(content = "다른 제보", authorId = "B")).map { input ->
                    async(Dispatchers.Default) { store.create(input) }
                }.awaitAll()
            }
            val winner = results.filterIsInstance<CreateNoteResult.Created>().single().note
            assertEquals(1, results.count { it == CreateNoteResult.Duplicate })
            assertEquals(winner, store.findById(winner.noteId))
            assertEquals(1, rowCount(db))
        }
    }

    @Test fun otherConstraintFailuresAreNotReportedAsDuplicatesOrSuccess() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build().withDatabase { db ->
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_note BEFORE INSERT ON road_notes BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            val store = RoomRoadNoteStore(db)
            assertEquals(CreateNoteResult.StorageFailure, store.create(note()))
            assertNull(store.findById(note().noteId))
            assertEquals(0, rowCount(db))
        }
    }

    @Test fun existingIdAndTriggerFailureFollowDocumentedDuplicatePrecedence() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build().withDatabase { db ->
            val store = RoomRoadNoteStore(db)
            val original = (store.create(note()) as CreateNoteResult.Created).note
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_note BEFORE INSERT ON road_notes BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            assertEquals(CreateNoteResult.Duplicate, store.create(note()))
            assertEquals(original, store.findById(original.noteId))
        }
    }

    @Test fun h3CreationFailureNeverInsertsAnUnindexedNewOriginal() = runBlocking {
        val fixture = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING, 37.5, 127.03, 0, 1_000, 601_000)
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        val broken = object : SpatialIndex {
            override fun cell(latitude: Double, longitude: Double): String = throw SpatialUnavailableException(UnsatisfiedLinkError("test"))
            override fun cover(latitude: Double, longitude: Double): Set<String> = error("unused")
        }
        try {
            val store = RoomRoadNoteStore(db, broken)
            assertEquals(CreateNoteResult.StorageFailure, store.create(fixture))
            assertNull(store.findById(fixture.noteId))
        } finally {
            db.close()
        }
    }

    private fun rowCount(db: NoteDatabase): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM road_notes").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}

private inline fun NoteDatabase.withDatabase(block: (NoteDatabase) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }

}
