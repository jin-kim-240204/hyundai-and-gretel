package com.jinkim.hyundaiAndGretel.notes

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

/** 컴파일만 확인했다. 새 스키마의 초기화·결합 조회·롤백은 별도 Android 실행 검증이 필요하다. */
class RoomScenarioStoreTest {
    @Test fun currentDatabasePreservesUnscopedHistoryAndOtherScenariosAcrossResetAndRollback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unscoped = note(1)
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        try {
            val scenarios = RoomScenarioStore(db)
            val receipts = RoomReceiptStore(db, { 10L })
            val index = object : SpatialIndex {
                override fun cell(latitude: Double, longitude: Double) = "fixture"
                override fun cover(latitude: Double, longitude: Double) = setOf("fixture")
            }
            val notes = RoomRoadNoteStore(db, index)
            assertTrue(notes.create(unscoped) is CreateNoteResult.Created)
            assertEquals(ReceiveResult.RECEIVED, receipts.receive(ReceiptRequest(unscoped.noteId, SyntheticSession("B"), LocationFix(37.5,127.03,0))))
            assertNull(scenarios.observeActive().first())
            assertEquals(unscoped.noteId, receipts.observeInbox("B").first().single().note.noteId)
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
            receipts.confirm(unscoped.noteId, "B")
            assertNotNull(receipts.observeInbox("B").first().first { it.note.noteId == unscoped.noteId }.displayedAtUtcMillis)
            val current = scenarios.observeActive().first()!!
            val retained = note(4).copy(scenarioId = current)
            notes.create(retained)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_reset BEFORE INSERT ON demo_state BEGIN SELECT RAISE(ABORT, 'fixture'); END")
            try { scenarios.reset(); fail("Expected rollback") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(current, scenarios.observeActive().first())
            assertNotNull(notes.findById(retained.noteId))
        } finally { db.close() }
    }
    private fun note(id: Int) = RoadNote("00000000-0000-0000-0000-${id.toString().padStart(12,'0')}","A","unscoped",NoteCategory.WARNING,
        37.5,127.03,0,0,600_000,SyntheticRoute.project(37.5,127.03,SyntheticSession()))
}
