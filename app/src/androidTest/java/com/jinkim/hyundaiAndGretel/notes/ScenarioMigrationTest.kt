package com.jinkim.hyundaiAndGretel.notes

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.storage.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Compile only: migration/reset/JOIN must later be run on an allowed Android target. */
class ScenarioMigrationTest {
    @Test fun versionThreePendingSurvivesScenarioCreationResetAndRollback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "scenario-v3-migration.db"
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile!!.mkdirs()
        val legacy = note(1)
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
                old.execSQL("CREATE TABLE road_notes (noteId TEXT NOT NULL PRIMARY KEY, authorId TEXT NOT NULL, content TEXT NOT NULL, category TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, anchorOffsetMillis INTEGER NOT NULL, createdAtUtcMillis INTEGER NOT NULL, expiresAtUtcMillis INTEGER NOT NULL, h3CellId TEXT, routeId TEXT, direction INTEGER, progressMeters REAL)")
                old.execSQL("CREATE INDEX index_road_notes_h3CellId ON road_notes (h3CellId)")
                old.execSQL("CREATE TABLE note_receipts (noteId TEXT NOT NULL, recipientId TEXT NOT NULL, receivedAtUtcMillis INTEGER NOT NULL, distanceMeters REAL NOT NULL, displayedAtUtcMillis INTEGER, PRIMARY KEY(noteId,recipientId), FOREIGN KEY(noteId) REFERENCES road_notes(noteId) ON DELETE CASCADE)")
                old.execSQL("CREATE INDEX index_note_receipts_recipientId_displayedAtUtcMillis_receivedAtUtcMillis_noteId ON note_receipts (recipientId,displayedAtUtcMillis,receivedAtUtcMillis,noteId)")
                old.execSQL("INSERT INTO road_notes VALUES (?,?,?, ?,?,?, ?,?,?, NULL,NULL,NULL,NULL)", arrayOf(legacy.noteId,"A","legacy","WARNING",37.5,127.03,0,0,600_000))
                old.execSQL("INSERT INTO note_receipts VALUES (?, 'B', 1, 0, NULL)", arrayOf(legacy.noteId))
                old.version = 3
            }
            val db = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_3_4).build()
            try {
                val scenarios = RoomScenarioStore(db)
                val receipts = RoomReceiptStore(db, { 10L })
                val index = object : SpatialIndex {
                    override fun cell(latitude: Double, longitude: Double) = "fixture"
                    override fun cover(latitude: Double, longitude: Double) = setOf("fixture")
                }
                val notes = RoomRoadNoteStore(db, index)
                assertNull(scenarios.observeActive().first())
                assertEquals(legacy.noteId, receipts.observeInbox("B").first().single().note.noteId)
                val active = scenarios.reset()
                val own = note(2).copy(scenarioId = active)
                val other = note(3).copy(scenarioId = "00000000-0000-0000-0000-000000000099")
                assertTrue(notes.create(own) is CreateNoteResult.Created)
                assertTrue(notes.create(other) is CreateNoteResult.Created)
                for (n in listOf(own, other)) {
                    assertEquals(ReceiveResult.RECEIVED, receipts.receive(ReceiptRequest(n.noteId, SyntheticSession("B"), LocationFix(37.5,127.03,0), n.scenarioId)))
                }
                scenarios.reset()
                assertNull(notes.findById(own.noteId))
                assertNotNull(notes.findById(other.noteId))
                assertEquals(2, receipts.observeInbox("B").first().size)
                receipts.confirm(legacy.noteId, "B")
                assertNotNull(receipts.observeInbox("B").first().first { it.note.noteId == legacy.noteId }.displayedAtUtcMillis)
                val current = scenarios.observeActive().first()!!
                val retained = note(4).copy(scenarioId = current)
                notes.create(retained)
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_reset BEFORE INSERT ON demo_state BEGIN SELECT RAISE(ABORT, 'fixture'); END")
                try { scenarios.reset(); fail("Expected rollback") } catch (_: android.database.sqlite.SQLiteException) { }
                assertEquals(current, scenarios.observeActive().first())
                assertNotNull(notes.findById(retained.noteId))
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
    private fun note(id: Int) = RoadNote("00000000-0000-0000-0000-${id.toString().padStart(12,'0')}","A","legacy",NoteCategory.WARNING,
        37.5,127.03,0,0,600_000,SyntheticRoute.project(37.5,127.03,SyntheticSession()))
}
