package com.jinkim.hyundaiAndGretel.ui.location

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.demo.DemoRuntime
import kotlinx.coroutines.flow.combine
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.location.LocationTrackingService
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationViewModel(private val app: Application) : AndroidViewModel(app), ServiceConnection {
    private val connection = LocationConnection(viewModelScope)
    private val visited = MutableStateFlow<Set<String>>(emptySet())
    val uiState = combine(connection.state, visited) { state, seen -> state.copy(visitedContent = seen) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocationUiState())
    private val mutableDemoRuntime = MutableStateFlow<DemoRuntime?>(null)
    val demoRuntime = mutableDemoRuntime.asStateFlow()
    private var currentBinder: LocationTrackingService.LocalBinder? = null
    fun markPresented(key: String) { visited.value += key }
    fun bufferSummary(): String = currentBinder?.bufferSummary() ?: "버퍼 연결 대기"
    val status = uiState.map { it.trackingStatus }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrackingStatus.STOPPED)
    private val mutableStartCommandFailed = MutableStateFlow(false)
    val startCommandFailed = mutableStartCommandFailed.asStateFlow()
    private val bound = app.bindService(Intent(app, LocationTrackingService::class.java), this, Context.BIND_AUTO_CREATE)

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        currentBinder = binder as LocationTrackingService.LocalBinder
        connection.connect(binder)
        mutableDemoRuntime.value = binder.demoRuntime
    }

    override fun onServiceDisconnected(name: ComponentName) {
        connection.connect(null)
        currentBinder = null; mutableDemoRuntime.value = null
    }

    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean) {
        connection.confirmReceipt(key, screenResumed, visited.value.toSet())
    }

    fun submitTestNote(content: String, category: NoteCategory) {
        connection.submitTestNote(content, category)
    }

    fun start() {
        if (!uiState.value.canStart) return
        if (demoRuntime.value?.enabled == true && !uiState.value.restrictions.canSetup) {
            mutableStartCommandFailed.value = true; return
        }
        mutableStartCommandFailed.value = false
        try {
            ContextCompat.startForegroundService(app,
                Intent(app, LocationTrackingService::class.java).setAction(LocationTrackingService.ACTION_START))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            mutableStartCommandFailed.value = true
            if (BuildConfig.DEBUG) Log.d("RoadLocation", "START_COMMAND_ERROR ${error.javaClass.simpleName}")
        }
    }

    fun stop() {
        app.startService(Intent(app, LocationTrackingService::class.java).setAction(LocationTrackingService.ACTION_STOP))
    }

    override fun onCleared() {
        if (bound) app.unbindService(this)
    }
}
