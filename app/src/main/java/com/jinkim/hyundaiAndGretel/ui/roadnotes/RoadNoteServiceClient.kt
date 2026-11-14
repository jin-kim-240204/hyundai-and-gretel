package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import com.jinkim.hyundaiAndGretel.service.RoadNoteServiceContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RoadNoteServiceClient(context: Context, scope: CoroutineScope) : ServiceConnection, AutoCloseable {
    private val context = context.applicationContext
    private val connection = RoadNoteServiceConnection(scope)
    private val mutableService = MutableStateFlow<RoadNoteServiceContract?>(null)
    val service = mutableService.asStateFlow()
    val state = connection.state
    private val mutableStartCommandFailed = MutableStateFlow(false)
    val startCommandFailed = mutableStartCommandFailed.asStateFlow()
    val canStartTracking get() = connection.canStartTracking
    private var closed = false
    private val bound = this.context.bindService(Intent(this.context, RoadNoteService::class.java), this, Context.BIND_AUTO_CREATE)

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        if (closed) return
        val service = binder as RoadNoteServiceContract
        connection.connect(service)
        mutableService.value = service
    }

    override fun onServiceDisconnected(name: ComponentName) {
        connection.connect(null)
        mutableService.value = null
    }

    fun startTracking() {
        if (!state.value.canStart) return
        if (!canStartTracking) {
            mutableStartCommandFailed.value = true
            return
        }
        mutableStartCommandFailed.value = false
        try {
            ContextCompat.startForegroundService(context,
                Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_START))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            mutableStartCommandFailed.value = true
            if (BuildConfig.DEBUG) Log.d("RoadLocation", "START_COMMAND_ERROR ${error.javaClass.simpleName}")
        }
    }

    fun stopTracking(pauseAutomatic: Boolean = true) {
        context.startService(Intent(context, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP)
            .putExtra(RoadNoteService.EXTRA_PAUSE_AUTOMATIC, pauseAutomatic))
    }

    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visited: Set<String>) {
        connection.confirmReceipt(key, screenResumed, visited)
    }

    fun submitRoadNote(content: String, category: NoteCategory) {
        connection.submitRoadNote(content, category)
    }

    override fun close() {
        if (closed) return
        closed = true
        try { if (bound) context.unbindService(this) }
        finally {
            connection.connect(null)
            mutableService.value = null
        }
    }
}
