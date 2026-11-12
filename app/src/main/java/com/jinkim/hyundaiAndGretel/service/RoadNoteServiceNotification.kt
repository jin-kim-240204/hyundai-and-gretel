package com.jinkim.hyundaiAndGretel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jinkim.hyundaiAndGretel.MainActivity
import com.jinkim.hyundaiAndGretel.R

/** 알림 내용만 구성한다. foreground 전환과 서비스 수명은 RoadNoteService가 소유한다. */
internal class RoadNoteServiceNotification(private val context: Context) {
    // 리뷰용: 알림의 내용과 버튼만 만든다. 이 함수 자체가 서비스를 시작하거나 종료하지는 않는다.
    fun build(savingOnly: Boolean = false): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.location_channel_name), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(context, 1,
            Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(if (savingOnly) R.string.note_saving_title else R.string.location_notification_title))
            .setContentText(context.getString(if (savingOnly) R.string.note_saving_text else R.string.location_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.location_notification_stop), stop)
            .build()
    }

    private companion object { const val CHANNEL = "location_tracking" }
}
