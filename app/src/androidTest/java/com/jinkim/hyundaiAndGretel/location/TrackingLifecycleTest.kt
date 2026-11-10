package com.jinkim.hyundaiAndGretel.location

import android.app.NotificationManager
import android.os.Build
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
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
        val connected = CompletableDeferred<LocationTrackingService>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete((binder as LocationTrackingService.LocalBinder).service)
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var bound = false
        try {
            bound = context.bindService(Intent(context, LocationTrackingService::class.java), connection, Context.BIND_AUTO_CREATE)
            assertTrue(bound)
            val service = withTimeout(10_000) { connected.await() }
            scenario.onActivity {
                ContextCompat.startForegroundService(it,
                    Intent(it, LocationTrackingService::class.java).setAction(LocationTrackingService.ACTION_START))
            }
            withTimeout(10_000) {
                while (service.tracking.status.value !in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING)) delay(50)
            }
            val original = service.tracking
            scenario.moveToState(Lifecycle.State.CREATED)
            delay(200)
            assertNotEquals(TrackingStatus.STOPPED, original.status.value)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            assertSame(original, service.tracking)
            assertTrue(original.status.value in setOf(TrackingStatus.WAITING, TrackingStatus.RECEIVING))
            val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id == 1 }
            notification.notification.actions.single().actionIntent.send()
            withTimeout(5_000) {
                while (original.status.value != TrackingStatus.STOPPED) delay(50)
            }
            assertEquals(TrackingStatus.STOPPED, original.status.value)
        } finally {
            withContext(Dispatchers.Main) {
                if (connected.isCompleted) connected.await().stopTracking()
            }
            if (bound) context.unbindService(connection)
            scenario.close()
        }
    }
}
