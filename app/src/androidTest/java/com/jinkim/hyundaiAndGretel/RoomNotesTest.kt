package com.jinkim.hyundaiAndGretel

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.data.*
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RoomNotesTest {
    @Test fun idempotencyReceiptsAndCascade() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        try {
            val spatial = SpatialIndex()
            val notes = RoomNotes(db, spatial)
            val receipts = ReceiptStore(db)
            val n = RoadNote("note", "req", "scenario", Actor.A.id, "pothole", Category.WARNING,
                1000, 1000, 601000, DemoRoute.LATITUDE, DemoRoute.longitudeAt(300.0), 90f,
                30000, spatial.cell(DemoRoute.LATITUDE, DemoRoute.longitudeAt(300.0)), 300.0)
            assertEquals(n, notes.create(n)); assertEquals(n, notes.create(n))
            assertEquals(1, db.notes().noteCount("scenario"))
            try { notes.create(n.copy(content="different")); fail("conflicting payload accepted") } catch (_: IllegalStateException) { }
            val loc = LocationSample(DemoRoute.LATITUDE, DemoRoute.longitudeAt(200.0), 2000, 200.0, 90f)
            assertFalse(receipts.record(n.id, "scenario", Actor.A, loc, 2000, 2000))
            assertFalse(receipts.record(n.id, "scenario", Actor.B, loc, 601000, 2000))
            val results = coroutineScope { (1..8).map { async(Dispatchers.IO) { receipts.record(n.id, "scenario", Actor.B, loc, 2000, 2000) } }.awaitAll() }
            assertEquals(1, results.count { it })
            assertEquals(1, db.notes().receiptCount(n.id, Actor.B.id))
            assertEquals(1, db.notes().noteCount("scenario"))
            receipts.markDisplayed(n.id, Actor.B, 2500)
            notes.deleteScenario("scenario")
            assertEquals(0, db.notes().receiptCount(n.id, Actor.B.id))
        } finally { db.close() }
    }
    @Test fun spatialCoverIncludesBoundaryAndAheadPoints() {
        val spatial = SpatialIndex()
        for (x in 0..1000 step 25) {
            val lon = DemoRoute.longitudeAt(x.toDouble())
            val cells = spatial.nearby(DemoRoute.LATITUDE, lon)
            for (offset in -350..350 step 25) {
                assertTrue(spatial.cell(DemoRoute.LATITUDE, DemoRoute.longitudeAt(x+offset.toDouble())) in cells)
            }
        }
    }
}
