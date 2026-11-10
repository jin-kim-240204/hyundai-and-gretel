package com.jinkim.hyundaiAndGretel.ui.location

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jinkim.hyundaiAndGretel.BuildConfig
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.location.LocationTrackingService
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(private val app: Application) : AndroidViewModel(app), ServiceConnection {
    private val mutableStatus = MutableStateFlow(TrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private val mutableStartCommandFailed = MutableStateFlow(false)
    val startCommandFailed = mutableStartCommandFailed.asStateFlow()
    private var observation: Job? = null
    private val bound = app.bindService(Intent(app, LocationTrackingService::class.java), this, Context.BIND_AUTO_CREATE)

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val connected = (binder as LocationTrackingService.LocalBinder).status
        observation?.cancel()
        observation = viewModelScope.launch { connected.collect { mutableStatus.value = it } }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        observation?.cancel()
        mutableStatus.value = TrackingStatus.STOPPED
    }

    fun start() {
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
        observation?.cancel()
        if (bound) app.unbindService(this)
    }
}
