package com.jinkim.hyundaiAndGretel.location

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.MainActivity
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class TrackingLifecycleTest {
    @Test fun activityStopAndRecreationKeepTheSameTrackingSession() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (permission in listOf("pleos.car.permission.FUSED_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
        }
        val connected = CompletableDeferred<IBinder>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete(binder)
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var bound = false
        try {
            bound = context.bindService(Intent(context, RoadNoteService::class.java), connection, Context.BIND_AUTO_CREATE)
            assertTrue(bound)
            val binder = withTimeout(10_000) { connected.await() }
            val status = (binder as RoadNoteService.LocalBinder).status
            val registrationsBefore = registeredLogs(instrumentation)
            scenario.onActivity {
                ContextCompat.startForegroundService(it,
                    Intent(it, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_START))
            }
            withTimeout(10_000) {
                while (status.value !in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING)) delay(50)
            }
            if (InstrumentationRegistry.getArguments().getString("bufferWindow") == "true") {
                withTimeout(15_000) {
                    while ("BUFFER_APPEND" !in registeredLogs(instrumentation).removePrefix(registrationsBefore)) delay(100)
                }
                delay(15_000)
                scenario.moveToState(Lifecycle.State.CREATED)
                val stoppedAt = SystemClock.elapsedRealtime()
                delay(20_000)
                assertBufferAdvanced(stoppedAt, registeredLogs(instrumentation), "CREATED")
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.recreate()
                val recreatedAt = SystemClock.elapsedRealtime()
                delay(15_000)
                assertBufferAdvanced(recreatedAt, registeredLogs(instrumentation), "RECREATED")
                val lines = registeredLogs(instrumentation).removePrefix(registrationsBefore)
                val windows = Regex("BUFFER_APPEND size=(\\d+) oldest=(\\d+) latest=(\\d+)").findAll(lines).toList()
                assertTrue("Expected real SDK buffer updates", windows.size >= 2)
                val first = windows.first().groupValues
                val last = windows.last().groupValues
                assertTrue("Retention window must advance", last[2].toLong() > first[2].toLong())
                assertTrue("Recent history survives Activity stop/recreation", last[3].toLong() - last[2].toLong() in 30_000..45_000)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            delay(200)
            assertNotEquals(TrackingStatus.STOPPED, status.value)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            withTimeout(5_000) {
                var screenConnected = false
                while (!screenConnected) {
                    scenario.onActivity {
                        val screenStatus = ViewModelProvider(it)[RoadNotesViewModel::class.java].status.value
                        screenConnected = screenStatus in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING)
                    }
                    if (!screenConnected) delay(50)
                }
            }
            val registrationsAfter = registeredLogs(instrumentation).removePrefix(registrationsBefore)
            assertEquals("Recreation must not register another SDK listener", 1,
                registrationsAfter.lineSequence().count { " REGISTERED" in it })
            val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id == 1 }
            notification.notification.actions.single().actionIntent.send()
            withTimeout(5_000) {
                while (status.value != TrackingStatus.STOPPED) delay(50)
            }
            assertEquals(TrackingStatus.STOPPED, status.value)
            scenario.onActivity {
                ContextCompat.startForegroundService(it,
                    Intent(it, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_START))
            }
            withTimeout(10_000) {
                while (status.value !in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING)) delay(50)
            }
            context.startService(Intent(context, RoadNoteService::class.java).setAction("unknown-test-command"))
            withTimeout(5_000) { while (status.value != TrackingStatus.STOPPED) delay(50) }
        } finally {
            context.startService(Intent(context, RoadNoteService::class.java)
                .setAction(RoadNoteService.ACTION_STOP))
            if (bound) context.unbindService(connection)
            scenario.close()
        }
    }

    @Test fun startCommandFailureSurvivesInitialServiceStatus() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connected = CompletableDeferred<IBinder>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) { connected.complete(binder) }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val bound = context.bindService(Intent(context, RoadNoteService::class.java), connection, Context.BIND_AUTO_CREATE)
        assertTrue(bound)
        val store = ViewModelStore()
        try {
            val binder = withTimeout(5_000) { connected.await() }
            withContext(Dispatchers.Main) {
                val app = object : Application() {
                    init { attachBaseContext(context) }
                    override fun getApplicationContext(): Context = this
                    override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int) = false
                    override fun startForegroundService(intent: Intent): ComponentName? = throw SecurityException("test")
                }
                val model = RoadNotesViewModel(app)
                store.put("test", model)
                model.startTracking()
                assertTrue(model.startCommandFailed.value)
                model.serviceClient.onServiceConnected(ComponentName(context, RoadNoteService::class.java), binder)
            }
            // 뷰모델의 관찰 작업이 서비스 초기 상태를 받을 때까지 진행한다.
            withContext(Dispatchers.Main) {
                val model = store.get("test") as RoadNotesViewModel
                assertEquals((binder as RoadNoteService.LocalBinder).status.value, model.status.value)
                assertTrue(model.startCommandFailed.value)
            }
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            context.unbindService(connection)
        }
    }

    private fun assertBufferAdvanced(boundaryElapsedMillis: Long, logs: String, phase: String) {
        val latest = latestBufferTime(logs)
        assertTrue("$phase must receive a new SDK sample", latest > boundaryElapsedMillis)
        Log.i("RoadLocation", "TEST_BUFFER_WINDOW phase=$phase boundary=$boundaryElapsedMillis latest=$latest")
    }

    private fun latestBufferTime(logs: String): Long {
        val latest = Regex("BUFFER_APPEND .*latest=(\\d+)").findAll(logs).lastOrNull()
            ?: error("No SDK buffer sample in logs")
        return latest.groupValues[1].toLong()
    }

    private fun registeredLogs(instrumentation: android.app.Instrumentation): String =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("logcat -d -v brief -s RoadLocation:D *:S")
        ).bufferedReader().use { it.readText() }

}
