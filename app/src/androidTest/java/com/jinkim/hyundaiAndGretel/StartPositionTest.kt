package com.jinkim.hyundaiAndGretel

import android.content.Intent
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.demo.ReportResult
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** Requires mock_location app-op for this debug app on the Pleos emulator. */
class StartPositionTest {
    private suspend fun console(path: String, method: String = "PUT") = withContext(Dispatchers.IO) {
        val connection = URL("http://127.0.0.1:8088$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 2000; connection.readTimeout = 2000
        try {
            check(connection.responseCode == 200) { "Console request failed: $path" }
            connection.inputStream.bufferedReader().use { it.readText() }
        }
        finally { connection.disconnect() }
    }
    @Test fun resetPositionsReachSdkAndConsoleSpeedMovesGps() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "pleos.car.permission.FUSED_LOCATION")
        instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        context.getSharedPreferences("road-note-demo", android.content.Context.MODE_PRIVATE).edit()
            .putBoolean("consoleGpsSelected", true).commit()
        val controller = (context.applicationContext as RoadNoteApplication).controller
        console("/browser/session?client=road-note-position-test", "POST")
        console("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0")
        console("/vehicle/gear?gear=Park")
        try {
            withContext(Dispatchers.Main) {
                controller.reset(); controller.startDemo()
            }
            withTimeout(10_000) { while (controller.state.value.location == null) delay(100) }
            assertEquals(0.0, controller.state.value.location!!.progressMeters, 2.0)
            assertTrue(controller.state.value.debugGpsActive)
            Log.i("RoadNotePositions", "A_START_CONFIRMED")
            withTimeout(3_000) { while (!controller.state.value.canReport) delay(50) }
            val saved = withContext(Dispatchers.Main) { controller.report("position test", Category.WARNING) }
            assertTrue(saved.toString(), saved is ReportResult.Saved)
            val note = (saved as ReportResult.Saved).note
            assertTrue("Cold-start report waited for history", note.offsetMillis in 0..5_000)
            withContext(Dispatchers.Main) { controller.switchActor(Actor.B) }
            withTimeout(10_000) { while (controller.state.value.location == null) delay(100) }
            assertEquals(note.progressMeters-600, controller.state.value.location!!.progressMeters, 2.0)
            assertTrue(controller.state.value.inbox.isEmpty())
            withTimeout(3000) { while (controller.state.value.plotNotes.none { it.id==note.id }) delay(100) }
            assertTrue(controller.state.value.plotNotes.any { it.id == note.id })
            Log.i("RoadNotePositions", "B_START_600M_BEHIND_CONFIRMED")
            console("/browser/session?client=road-note-position-test", "POST")
            console("/vehicle/gear?gear=Drive")
            console("/vehicle/controls?speed=36&accelerator=0&brake=0&steering=0")
            withTimeout(12_000) {
                while ((controller.state.value.location?.progressMeters ?: -10000.0) < note.progressMeters-580) delay(100)
            }
            // Releasing the accelerator makes the console coast; compare its current speed, not the initial setpoint.
            val consoleSpeed = JSONObject(console("/vehicle/state", "GET")).getJSONObject("controls").getDouble("speed_kph")
            assertTrue(controller.state.value.debugSpeedKph > 0)
            assertEquals(consoleSpeed, controller.state.value.debugSpeedKph, 3.0)
            Log.i("RoadNotePositions", "CONSOLE_SPEED_MOVES_REAL_SDK_POSITION")
            withContext(Dispatchers.Main) { controller.useExternalGps() }
            assertFalse(controller.state.value.debugGpsActive)
        } finally {
            withContext(Dispatchers.Main) { controller.stop() }
            context.getSharedPreferences("road-note-demo", android.content.Context.MODE_PRIVATE).edit()
                .putBoolean("consoleGpsSelected", true).commit()
            console("/browser/session?client=road-note-position-test", "POST")
            console("/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0")
            console("/vehicle/gear?gear=Park")
            console("/browser/session?client=road-note-position-test&closing=1", "POST")
        }
    }
}
