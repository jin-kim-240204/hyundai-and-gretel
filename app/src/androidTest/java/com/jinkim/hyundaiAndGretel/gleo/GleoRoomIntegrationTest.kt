package com.jinkim.hyundaiAndGretel.gleo

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase
import com.jinkim.hyundaiAndGretel.notes.storage.RoomRoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GleoRoomIntegrationTest {
    @Test fun parsedReportCommitsCurrentRoomModelBeforeSuccessCallback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        try {
            val submission = RoadNoteSubmission(this, RoomRoadNoteStore(db), {
                AnchorSelection.Selected(LocationAnchor(LocationFix(37.5, 127.03, 0), 30_000))
            }, { 1_000 }, {})
            val processor = GleoActionProcessor { content, category, _ -> submission.submit(content, category).await() }
            val action = parseGleoAction("""{"content":"구덩이","category":"WARNING","locationMode":"CURRENT_ROAD"}""")
            val response = processor.process(action, 30_000)
            assertEquals(GleoStatus.SUCCESS, response.status)
            val stored = db.notes().findById(requireNotNull(response.noteId))
            assertNotNull(stored)
            assertEquals(30_000L, stored!!.anchorOffsetMillis)
            val callback = JSONObject(response.toCallbackJson())
            assertEquals("success", callback.getString("status"))
            val data = JSONArray(callback.getString("data")).getJSONObject(0)
            assertEquals(stored.noteId, data.getString("noteId"))
            assertEquals("WARNING", data.getString("category"))
            assertTrue(data.getBoolean("storedLocally"))
            assertFalse(data.has("draftId"))
            for (raw in listOf("{}", "not json", "{}{}", "[1]"))
                assertTrue(parseGleoAction(raw) is ParsedGleoAction.Rejected)
        } finally { db.close() }
    }
}
