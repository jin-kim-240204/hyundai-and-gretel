package com.jinkim.hyundaiAndGretel

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.pleos.playground.location.fused.model.ProximityTransition
import com.jinkim.hyundaiAndGretel.location.PleosLocationSource
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/** Opt-in hardware integration: run with -e class ...ProximityTest and inject GPS when READY. */
@RunWith(AndroidJUnit4::class)
class ProximityTest {
    @Test fun injectedRouteProducesEnterAndExit() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName,
            "pleos.car.permission.FUSED_LOCATION")
        val events = CopyOnWriteArrayList<ProximityTransition>()
        val errors = CopyOnWriteArrayList<String>()
        val source = PleosLocationSource(instrumentation.targetContext)
        val registered = CompletableDeferred<String>()
        try {
            withContext(Dispatchers.Main) {
                source.start({}, { errors += it.toString() })
                source.register(37.4979, 127.0276,
                    { registered.complete(it) },
                    { events += it.transition; Log.i("RoadNoteTest", "PROXIMITY ${it.transition}") },
                    { errors += it.toString() })
            }
            withTimeout(10_000) { registered.await() }
            Log.i("RoadNoteTest", "READY_FOR_PROXIMITY_ROUTE")
            withTimeout(65_000) {
                while (!(events.contains(ProximityTransition.ENTER) && events.contains(ProximityTransition.EXIT))) delay(100)
            }
            assertTrue(errors.toString(), errors.isEmpty())
            withContext(Dispatchers.Main) { source.remove(registered.await()) }
            Log.i("RoadNoteTest", "PROXIMITY_REMOVED")
        } finally {
            withContext(Dispatchers.Main) { source.stop() }
        }
    }
}
