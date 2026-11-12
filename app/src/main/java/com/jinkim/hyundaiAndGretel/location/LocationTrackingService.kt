package com.jinkim.hyundaiAndGretel.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.MainActivity
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.notes.spatial.H3SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.spatial.NearbyCandidates
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.storage.RoomReceiptStore
import kotlinx.coroutines.flow.MutableStateFlow
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNoteRepository
import com.jinkim.hyundaiAndGretel.notes.storage.RoomRoadNoteStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class LocationTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tracking = LocationTracking(
        scope = scope,
        createSource = { PleosLocationSource(this) },
        log = { message ->
            if (BuildConfig.DEBUG) Log.d("RoadLocation", message)
        },
        onFailure = { stopReceiving(); finishWhenStorageIsIdle() },
        clock = SystemClock::elapsedRealtime,
    )
    private val spatial = H3SpatialIndex()
    private val noteStore by lazy {
        RoomRoadNoteStore(this, spatial) { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "H3_CREATE_ERROR ${error.javaClass.simpleName}")
        }
    }
    private val candidates by lazy {
        NearbyCandidates(scope, tracking.currentPosition, spatial, noteStore,
            elapsedMillis = SystemClock::elapsedRealtime, utcMillis = System::currentTimeMillis,
            onFailure = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            })
    }
    private val setup = SyntheticSession() // 06 will supply explicit synthetic role/route configuration.
    private val recipient = MutableStateFlow(setup.actorId)
    private val receipts by lazy {
        ReceiptRepository(scope, RoomReceiptStore(this, onError = { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "RECEIPT_INSERT ${error.javaClass.simpleName}")
        }), recipient, ::finishWhenStorageIsIdle,
            onWriteAccepted = ::holdStorageForeground)
    }
    private val pickupDelegate = lazy {
        ProximityPickup(scope, tracking.currentPosition, candidates.state, candidates::currentCandidates, receipts,
            tracking::ownsSource, SystemClock::elapsedRealtime, System::currentTimeMillis,
            onError = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            })
    }
    private val pickup by pickupDelegate

    private val submissions: RoadNoteRepository by lazy {
        RoadNoteRepository(
            scope = scope,
            store = noteStore,
            captureAnchor = tracking::selectAnchor,
            utcMillis = System::currentTimeMillis,
            onFinished = ::finishWhenStorageIsIdle,
            setup = { setup },
            onUnexpectedError = { error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "STORE_UNEXPECTED_ERROR ${error.javaClass.simpleName}")
            },
        )
    }
    private val isTracking get() = tracking.status.value.isCollecting
    private val binder = LocalBinder()

    inner class LocalBinder : Binder(), LocationSession {
        override val status get() = tracking.status
        override val submissionState get() = submissions.state
        override val inboxState get() = receipts.inbox
        override val pickupState get() = pickup.state
        override fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean) = receipts.confirm(key, screenResumed)

        override fun submitTestNote(content: String, category: NoteCategory) {
            if (BuildConfig.DEBUG) submissions.submit(content, category)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            ACTION_STORAGE_HOLD -> finishWhenStorageIsIdle()
            // Preserve the existing stop behavior for null or unknown commands.
            else -> stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (isTracking || submissions.isSubmitting || receipts.hasPendingWrites) return
        try {
            val notification = buildTrackingNotification()
            val serviceType = foregroundServiceType(collecting = true, sdkInt = Build.VERSION.SDK_INT)
            ServiceCompat.startForeground(this, 1, notification, serviceType)
            candidates
            tracking.start()
            tracking.proximitySource?.let { pickup.start(tracking.sessionId, it, setup) }
        } catch (cancelled: CancellationException) {
            tracking.stop()
            finishWhenStorageIsIdle()
            throw cancelled
        } catch (error: Exception) {
            tracking.fail(error, operation = "FOREGROUND_START")
        }
    }

    private fun stopReceiving() {
        if (pickupDelegate.isInitialized()) pickup.stop()
    }

    private fun stopTracking() {
        try {
            stopReceiving()
        } finally {
            try { tracking.stop() }
            finally { finishWhenStorageIsIdle() }
        }
    }

    private fun finishWhenStorageIsIdle() {
        if (submissions.isSubmitting || receipts.hasPendingWrites) {
            if (isTracking) {
                getSystemService(NotificationManager::class.java).notify(1, buildTrackingNotification())
            } else {
                try { promoteStorageForeground() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    // Already accepted writes still finish even if the type transition fails.
                    if (BuildConfig.DEBUG) Log.d("RoadNote", "STORAGE_FOREGROUND ${error.javaClass.simpleName}")
                }
            }
        } else if (shouldStopTrackingService(tracking.status.value, submissions.state.value, receipts.hasPendingWrites)) {
            stopForegroundAndRequestServiceStop()
        }
    }

    private fun holdStorageForeground() {
        if (isTracking) return
        // A bound-only service must become started before an accepted ACK outlives unbinding.
        promoteStorageForeground()
        try {
            startService(Intent(this, LocationTrackingService::class.java).setAction(ACTION_STORAGE_HOLD))
        } catch (error: RuntimeException) {
            if (!submissions.isSubmitting && !receipts.hasPendingWrites) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
            throw error
        }
    }

    private fun promoteStorageForeground() {
        ServiceCompat.startForeground(this, 1, buildTrackingNotification(savingOnly = true),
            foregroundServiceType(collecting = false, sdkInt = Build.VERSION.SDK_INT))
    }

    private fun stopForegroundAndRequestServiceStop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTrackingNotification(savingOnly: Boolean = false): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.location_channel_name), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, LocationTrackingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(if (savingOnly) R.string.note_saving_title else R.string.location_notification_title))
            .setContentText(getString(if (savingOnly) R.string.note_saving_text else R.string.location_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, getString(R.string.location_notification_stop), stop)
            .build()
    }

    override fun onDestroy() {
        try {
            stopReceiving()
        } finally {
            try { tracking.stop() }
            finally {
                scope.cancel()
                super.onDestroy()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.jinkim.hyundaiAndGretel.START_LOCATION"
        const val ACTION_STOP = "com.jinkim.hyundaiAndGretel.STOP_LOCATION"
        private const val ACTION_STORAGE_HOLD = "com.jinkim.hyundaiAndGretel.HOLD_NOTE_WRITES"
        private const val CHANNEL = "location_tracking"
    }
}
