package com.jinkim.hyundaiAndGretel.notes

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.spatial.SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class RoomReceiptStoreTest {
    @Test fun transactionUniquenessJoinAckExpiryAndCascade() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        val index = object : SpatialIndex {
            override fun cell(latitude: Double, longitude: Double) = "fixture"
            override fun cover(latitude: Double, longitude: Double) = setOf("fixture")
        }
        var utc = 1_000L
        val receipts = RoomReceiptStore(db, utcMillis = { utc })
        val store = RoomRoadNoteStore(db, index)
        val setup = SyntheticSession("B")
        val position = LocationFix(37.5, 127.03, 0)
        val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
            37.501, 127.029, 0, 0, 600_000)
        try {
            assertTrue(store.create(note) is CreateNoteResult.Created)
            val request = ReceiptRequest(note.noteId, setup, position)
            assertEquals(ReceiveResult.INELIGIBLE, receipts.receive(request.copy(
                position = position.copy(longitude = 127.04),
            )))
            assertEquals(ReceiveResult.INELIGIBLE, receipts.receive(request.copy(scenarioId = note.noteId)))
            assertTrue(receipts.observeInbox("B").first().isEmpty())
            val results = coroutineScope { List(2) { async { receipts.receive(request) } }.awaitAll() }
            assertEquals(1, results.count { it == ReceiveResult.RECEIVED })
            assertEquals(1, results.count { it == ReceiveResult.ALREADY_RECEIVED })
            assertEquals(ReceiveResult.RECEIVED, receipts.receive(request.copy(recipient = SyntheticSession("C"))))
            val joined = receipts.observeInbox("B").first().single()
            assertEquals(note, joined.note)
            assertNull(joined.displayedAtUtcMillis)
            utc = 2_000
            receipts.confirm(note.noteId, "B")
            utc = 3_000
            receipts.confirm(note.noteId, "B")
            assertEquals(2_000L, receipts.observeInbox("B").first().single().displayedAtUtcMillis)
            assertNull(receipts.observeInbox("C").first().single().displayedAtUtcMillis)
            utc = note.expiresAtUtcMillis
            assertEquals(ReceiveResult.INELIGIBLE, receipts.receive(request.copy(recipient = SyntheticSession("D"))))
            assertEquals(1, receipts.observeInbox("B").first().size)
            db.openHelper.writableDatabase.execSQL("DELETE FROM road_notes WHERE noteId = ?", arrayOf(note.noteId))
            assertTrue(receipts.observeInbox("B").first().isEmpty())
        } finally { db.close() }
    }
}
