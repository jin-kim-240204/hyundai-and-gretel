package com.jinkim.hyundaiAndGretel.location

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.MainActivity
import com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class RejectedTrackingStartTest {
    @Test fun missingScenarioRejectsStartWithoutKillingBoundService() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("기존 시연 데이터는 보존한다", NoteDatabase.getInstance(context).scenarios().active() == null)
        for (permission in listOf("pleos.car.permission.FUSED_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        }
        val connected = CompletableDeferred<RoadNoteService.LocalBinder>()
        var disconnected = false
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete(binder as RoadNoteService.LocalBinder)
            }
            override fun onServiceDisconnected(name: ComponentName) { disconnected = true }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val bound = context.bindService(Intent(context, RoadNoteService::class.java), connection, Context.BIND_AUTO_CREATE)
        try {
            assertTrue(bound)
            val binder = withTimeout(5_000) { connected.await() }
            withTimeout(5_000) { while (!binder.uiRestrictions.value.canSetup) delay(50) }
            repeat(1) {
                scenario.onActivity {
                    ContextCompat.startForegroundService(it, Intent(it, RoadNoteService::class.java)
                        .setAction(RoadNoteService.ACTION_START))
                }

                delay(45_000)
                assertFalse(disconnected)
                assertEquals(TrackingStatus.STOPPED, binder.status.value)
                assertEquals("먼저 새 시연을 초기화해주세요", binder.demoRuntime.state.value.message)
                assertNull(NoteDatabase.getInstance(context).scenarios().active())
            }
        } finally {
            context.startService(Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP))
            if (bound) context.unbindService(connection)
            scenario.close()
        }
    }
}
