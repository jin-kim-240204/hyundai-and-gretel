package com.jinkim.hyundaiAndGretel.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoadNoteSubmissionClient @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun submitReport(content: String, category: NoteCategory, receivedElapsedMillis: Long): SubmissionState =
        withContext(Dispatchers.Main.immediate) {
            val connected = CompletableDeferred<RoadNoteService.LocalBinder>()
            var closed = false
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    if (!closed) connected.complete(binder as RoadNoteService.LocalBinder)
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    connected.completeExceptionally(IllegalStateException("Service disconnected"))
                }
                override fun onNullBinding(name: ComponentName) {
                    connected.completeExceptionally(IllegalStateException("Service binding unavailable"))
                }
                override fun onBindingDied(name: ComponentName) {
                    connected.completeExceptionally(IllegalStateException("Service binding lost"))
                }
            }
            val bound = context.bindService(Intent(context, RoadNoteService::class.java), connection, Context.BIND_AUTO_CREATE)
            try {
                check(bound) { "Service binding failed" }
                connected.await().submitRoadNoteWithResult(content, category, receivedElapsedMillis).await()
            } finally {
                closed = true
                connected.cancel()
                if (bound) context.unbindService(connection)
            }
        }
}
