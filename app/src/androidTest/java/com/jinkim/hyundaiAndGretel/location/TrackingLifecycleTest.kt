package com.jinkim.hyundaiAndGretel.location

import android.app.NotificationManager
import android.app.Application
import android.os.Build
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.jinkim.hyundaiAndGretel.ui.location.LocationViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.MainActivity
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
        val connected = CompletableDeferred<StateFlow<TrackingStatus>>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete((binder as LocationTrackingService.LocalBinder).status)
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var bound = false
        try {
            bound = context.bindService(Intent(context, LocationTrackingService::class.java), connection, Context.BIND_AUTO_CREATE)
            assertTrue(bound)
            val status = withTimeout(10_000) { connected.await() }
            val registrationsBefore = registeredLogs(instrumentation)
            scenario.onActivity {
                ContextCompat.startForegroundService(it,
                    Intent(it, LocationTrackingService::class.java).setAction(LocationTrackingService.ACTION_START))
            }
            withTimeout(10_000) {
                while (status.value !in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING)) delay(50)
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
                        val screenStatus = ViewModelProvider(it)[LocationViewModel::class.java].status.value
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
        } finally {
            context.startService(Intent(context, LocationTrackingService::class.java)
                .setAction(LocationTrackingService.ACTION_STOP))
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
        val bound = context.bindService(Intent(context, LocationTrackingService::class.java), connection, Context.BIND_AUTO_CREATE)
        assertTrue(bound)
        val store = ViewModelStore()
        try {
            val binder = withTimeout(5_000) { connected.await() }
            withContext(Dispatchers.Main) {
                val app = object : Application() {
                    init { attachBaseContext(context) }
                    override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int) = false
                    override fun startForegroundService(intent: Intent): ComponentName? = throw SecurityException("test")
                }
                val model = LocationViewModel(app)
                store.put("test", model)
                model.start()
                assertTrue(model.startCommandFailed.value)
                model.onServiceConnected(ComponentName(context, LocationTrackingService::class.java), binder)
            }
            // Allow the ViewModel's collector to receive the service's initial state.
            withContext(Dispatchers.Main) {
                val model = store.get("test") as LocationViewModel
                assertEquals((binder as LocationTrackingService.LocalBinder).status.value, model.status.value)
                assertTrue(model.startCommandFailed.value)
            }
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            context.unbindService(connection)
        }
    }

    private fun registeredLogs(instrumentation: android.app.Instrumentation): String =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("logcat -d -v brief -s RoadLocation:D *:S")
        ).bufferedReader().use { it.readText() }

}
