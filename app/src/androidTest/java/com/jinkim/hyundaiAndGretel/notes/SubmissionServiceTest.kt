package com.jinkim.hyundaiAndGretel.notes

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.MainActivity
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SubmissionServiceTest {
    @Test fun acceptedSaveOutlivesStopAndLastUnbindOnSuccessAndFailure() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (permission in listOf("pleos.car.permission.FUSED_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        }
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, "android.permission.POST_NOTIFICATIONS",
        )
        val db = NoteDatabase.getInstance(context)
        for (failStorage in listOf(false, true)) {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            val connected = CompletableDeferred<IBinder>()
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) { connected.complete(service) }
                override fun onServiceDisconnected(name: ComponentName) = Unit
            }
            var bound = context.bindService(Intent(context, RoadNoteService::class.java), connection, Context.BIND_AUTO_CREATE)
            val release = CompletableDeferred<Unit>()
            var blocker: Job? = null
            try {
                val binder = withTimeout(10_000) { connected.await() } as RoadNoteService.LocalBinder
                scenario.onActivity {
                    ContextCompat.startForegroundService(it, Intent(it, RoadNoteService::class.java)
                        .setAction(RoadNoteService.ACTION_START))
                }
                withTimeout(30_000) { while (binder.status.value != TrackingStatus.RECEIVING) delay(50) }
                val locked = CompletableDeferred<Unit>()
                blocker = launch(Dispatchers.IO) {
                    db.withTransaction {
                        if (failStorage) db.openHelper.writableDatabase.execSQL(
                            "CREATE TRIGGER reject_submission BEFORE INSERT ON road_notes BEGIN SELECT RAISE(ABORT, 'test'); END",
                        )
                        locked.complete(Unit)
                        release.await()
                    }
                }
                withTimeout(5_000) { locked.await() }
                withContext(Dispatchers.Main) { binder.submitRoadNote("구덩이", NoteCategory.WARNING) }
                assertEquals(SubmissionState.Submitting, binder.submissionState.value)
                context.startService(Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP))
                withTimeout(5_000) { while (binder.status.value != TrackingStatus.STOPPED) delay(50) }
                context.unbindService(connection)
                bound = false
                scenario.close()
                delay(200)
                val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id == 1 }
                assertEquals("제보 저장 중", notification.notification.extras.getString("android.title"))
                assertEquals(SubmissionState.Submitting, binder.submissionState.value)
                release.complete(Unit)
                withTimeout(10_000) { while (binder.submissionState.value == SubmissionState.Submitting) delay(50) }
                blocker.join()
                if (failStorage) {
                    assertEquals(SubmissionState.Failed(SubmissionFailure.STORAGE), binder.submissionState.value)
                } else {
                    val saved = binder.submissionState.value as SubmissionState.Saved
                    assertEquals(saved.note.noteId, db.notes().findById(saved.note.noteId)?.noteId)
                }
                withTimeout(5_000) {
                    while (context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == 1 }) delay(50)
                    @Suppress("DEPRECATION")
                    while (context.getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
                            .any { it.service.className == RoadNoteService::class.java.name }) delay(50)
                }
            } finally {
                release.complete(Unit)
                blocker?.join()
                withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS reject_submission") }
                context.startService(Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP))
                if (bound) context.unbindService(connection)
                scenario.close()
            }
        }
    }
}
