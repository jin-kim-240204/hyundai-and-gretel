package com.jinkim.hyundaiAndGretel

import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.demo.ReportResult
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionGuardTest {
    @Test fun anchorSnapshotCannotSaveWhenCurrentLocationIsUnavailable() = runBlocking<Unit> {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as RoadNoteApplication
        withContext(Dispatchers.Main) {
            val controller = app.controller
            try {
                controller.reset()
                controller.start()
                val context = controller.capture()
                val sample = LocationSample(DemoRoute.LATITUDE, DemoRoute.LONGITUDE, context.elapsed, 0.0, 90f)
                val oldValidSnapshot = context.copy(samples=listOf(sample.copy(elapsedMillis=context.elapsed-30000), sample))
                assertEquals(ReportResult.Failed("LOCATION_UNAVAILABLE"),
                    controller.report("pothole", Category.WARNING, oldValidSnapshot))
            } finally { controller.stop() }
        }
    }
}
