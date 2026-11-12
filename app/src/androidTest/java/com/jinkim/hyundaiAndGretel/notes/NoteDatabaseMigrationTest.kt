package com.jinkim.hyundaiAndGretel.notes

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialUnavailableException
import com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase
import com.jinkim.hyundaiAndGretel.notes.storage.RoomRoadNoteStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Compiled under the current no-device policy; requires later device execution. */
class NoteDatabaseMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이",
        NoteCategory.WARNING, 37.5, 127.03, 0, 1_000, 601_000)
    private val index = object : SpatialIndex {
        override fun cell(latitude: Double, longitude: Double) = "fixture-cell"
        override fun cover(latitude: Double, longitude: Double) = setOf("fixture-cell")
    }

    @Test fun versionOneRowsSurviveAndAreObservableWithoutBackfill() = checkMigration(1)

    @Test fun versionTwoRowsSurviveWithNullRouteMetadata() = checkMigration(2)

    private fun checkMigration(version: Int) = runBlocking {
        val name = "road-note-migration-fixture.db"
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
                old.execSQL("CREATE TABLE road_notes (noteId TEXT NOT NULL PRIMARY KEY, authorId TEXT NOT NULL, content TEXT NOT NULL, category TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, anchorOffsetMillis INTEGER NOT NULL, createdAtUtcMillis INTEGER NOT NULL, expiresAtUtcMillis INTEGER NOT NULL)")
                old.execSQL("INSERT INTO road_notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(
                    fixture.noteId, fixture.authorId, fixture.content, fixture.category.name,
                    fixture.latitude, fixture.longitude, fixture.anchorOffsetMillis, fixture.createdAtUtcMillis, fixture.expiresAtUtcMillis,
                ))
                if (version == 2) {
                    old.execSQL("ALTER TABLE road_notes ADD COLUMN h3CellId TEXT")
                    old.execSQL("CREATE INDEX index_road_notes_h3CellId ON road_notes (h3CellId)")
                }
                old.version = version
            }
            val db = Room.databaseBuilder(context, NoteDatabase::class.java, name)
                .addMigrations(NoteDatabase.MIGRATION_1_2, NoteDatabase.MIGRATION_2_3).build()
            try {
                val store = RoomRoadNoteStore(db, index)
                assertEquals(fixture, store.findById(fixture.noteId))
                assertNull(db.notes().findById(fixture.noteId)?.routeId)
                assertNull(db.notes().findById(fixture.noteId)?.direction)
                assertNull(db.notes().findById(fixture.noteId)?.progressMeters)
                assertNull(db.notes().findById(fixture.noteId)?.h3CellId)
                // Time is deliberately not filtered by the observable storage query.
                assertEquals(listOf(fixture), store.observeNearby(setOf("fixture-cell")).first())
                assertNull(db.notes().findById(fixture.noteId)?.h3CellId)
                val newer = fixture.copy(noteId = "00000000-0000-0000-0000-000000000002")
                assertEquals(CreateNoteResult.Created(newer), store.create(newer))
                assertEquals("fixture-cell", db.notes().findById(newer.noteId)?.h3CellId)
                assertTrue(store.observeNearby(setOf("other-cell")).first().isEmpty())
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun h3CreationFailureNeverInsertsAnUnindexedNewOriginal() = runBlocking {
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
}
