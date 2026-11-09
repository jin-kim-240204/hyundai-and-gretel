package com.jinkim.hyundaiAndGretel

import android.content.Intent
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.demo.ReportResult
import com.jinkim.hyundaiAndGretel.gleo.GLEO_ACTION
import com.jinkim.hyundaiAndGretel.gleo.callbackJson
import org.json.JSONObject
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Requires ADB GPS replay; see docs/LOCATION-DEMO.md. */
class DemoFlowTest {
    @Test fun plantAndPickupUsingRealSdk() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val token = InstrumentationRegistry.getArguments().getString("runToken") ?: "manual"
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "pleos.car.permission.FUSED_LOCATION")
        instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val controller = (context.applicationContext as RoadNoteApplication).controller
        try {
            withContext(Dispatchers.Main) { controller.reset(); controller.start() }
            delay(2000)
            Log.i("RoadNoteE2E", "$token A_READY")
            withTimeout(55_000) { while (!controller.state.value.canReport) delay(100) }
            val oldContext = withContext(Dispatchers.Main) { controller.capture() }
            context.sendBroadcast(Intent(GLEO_ACTION).setPackage(context.packageName).putExtra("ACTION_PARAM",
                """{"content":"도로에 구덩이가 있어요","category":"WARNING","locationMode":"CURRENT_ROAD"}"""))
            withTimeout(5000) { while (controller.state.value.nearbyNotes.isEmpty()) delay(50) }
            val note = controller.state.value.nearbyNotes.single()
            assertEquals("success", JSONObject(callbackJson(ReportResult.Saved(note))).getString("status"))
            assertTrue(note.offsetMillis in 25_000..35_000)
            assertTrue(controller.state.value.location!!.progressMeters - note.progressMeters > 200)
            withContext(Dispatchers.Main) { controller.switchActor(Actor.B) }
            val stale = withContext(Dispatchers.Main) { controller.report("old session", Category.WARNING, oldContext) }
            assertEquals(ReportResult.Failed("SESSION_NOT_READY"), stale)
            delay(2000)
            Log.i("RoadNoteE2E", "$token B_READY ${note.progressMeters}")
            withTimeout(50_000) { while (controller.state.value.registeredCount == 0) delay(100) }
            assertTrue("B already received outside radius", controller.state.value.inbox.isEmpty())
            withTimeout(50_000) { while (controller.state.value.inbox.isEmpty()) delay(100) }
            assertEquals(note.id, controller.state.value.inbox.single().note.id)
            delay(3000)
            assertEquals(1, controller.state.value.inbox.size)
            Log.i("RoadNoteE2E", "$token FLOW_PASS")
        } finally { withContext(Dispatchers.Main) { controller.stop() } }
    }
}
