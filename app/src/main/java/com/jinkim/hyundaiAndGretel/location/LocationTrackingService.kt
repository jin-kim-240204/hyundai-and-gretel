package com.jinkim.hyundaiAndGretel.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
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
        onFailure = { finishForeground() },
        clock = SystemClock::elapsedRealtime,
    )
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        val status get() = tracking.status
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            try {
                startTrackingForeground()
                tracking.start()
            } catch (cancelled: CancellationException) {
                finishForeground()
                throw cancelled
            } catch (error: Exception) {
                tracking.fail(error)
            }
        } else {
            stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun stopTracking() {
        try {
            tracking.stop()
        } finally {
            finishForeground()
        }
    }

    private fun finishForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTrackingForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "위치 수집", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, LocationTrackingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("도로 제보 위치 수집 중")
            .setContentText("앱을 벗어나도 위치 수집을 유지합니다.")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, "중지", stop)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, 1, notification, type)
    }

    override fun onDestroy() {
        try {
            tracking.stop()
        } finally {
            scope.cancel()
            super.onDestroy()
        }
    }

    companion object {
        const val ACTION_START = "com.jinkim.hyundaiAndGretel.START_LOCATION"
        const val ACTION_STOP = "com.jinkim.hyundaiAndGretel.STOP_LOCATION"
        private const val CHANNEL = "location_tracking"
    }
}
