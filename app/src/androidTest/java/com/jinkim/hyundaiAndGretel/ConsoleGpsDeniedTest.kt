package com.jinkim.hyundaiAndGretel

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

/** Run with mock_location denied to reproduce the stopped console-GPS regression. */
class ConsoleGpsDeniedTest {
    @Test fun missingMockPermissionShowsReasonInsteadOfFrozenGpsFallback() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("road-note-demo", Context.MODE_PRIVATE).edit()
            .putBoolean("consoleGpsSelected", true).commit()
        val controller = (context.applicationContext as RoadNoteApplication).controller
        withContext(Dispatchers.Main) {
            controller.stop()
            controller.startDemo()
            val state = controller.state.value
            assertFalse(state.running)
            assertFalse(state.debugGpsActive)
            assertFalse(state.canReport)
            assertNull(state.location)
            assertTrue(state.debugGpsIssue.contains("모의 위치 권한"))
        }
    }
}
