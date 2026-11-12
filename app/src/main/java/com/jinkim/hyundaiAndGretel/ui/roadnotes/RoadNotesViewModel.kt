package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.demo.DemoRuntime
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoadNotesViewModel(private val app: Application) : AndroidViewModel(app), ServiceConnection {
    // 리뷰용: 뷰모델은 서비스 연결 상태와 화면 표시 이력을 조합한다. 직접 위치 SDK를 생성하거나 SQL을 실행하지 않는다.
    private val connection = RoadNoteServiceConnection(viewModelScope)
    private val visited = MutableStateFlow<Set<String>>(emptySet())
    private val mutableStartCommandFailed = MutableStateFlow(false)
    val startCommandFailed = mutableStartCommandFailed.asStateFlow()
    val uiState = combine(connection.state, visited, mutableStartCommandFailed) { state, seen, failed ->
        state.copy(visitedContent = seen, startCommandFailed = failed)
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RoadNotesUiState())
    private val mutableDemoRuntime = MutableStateFlow<DemoRuntime?>(null)
    val demoRuntime = mutableDemoRuntime.asStateFlow()
    private var currentBinder: RoadNoteService.LocalBinder? = null
    fun markPresented(key: String) { visited.value += key }
    fun bufferSummary(): String = currentBinder?.bufferSummary() ?: "버퍼 연결 대기"
    val status = uiState.map { it.trackingStatus }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrackingStatus.STOPPED)
    val canStartTracking get() = demoRuntime.value?.let { demo ->
        !demo.enabled || (uiState.value.restrictions.canSetup && !demo.transitioning)
    } ?: true
    private val bound = app.bindService(Intent(app, RoadNoteService::class.java), this, Context.BIND_AUTO_CREATE)

    // 리뷰용: 바인더를 받으면 화면 상태 관찰과 디버그 연결을 현재 서비스로 바꾼다.
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        currentBinder = binder as RoadNoteService.LocalBinder
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

    fun submitRoadNote(content: String, category: NoteCategory) {
        connection.submitRoadNote(content, category)
    }

    // 리뷰용: 화면의 시작 요청을 서비스 명령으로 바꾼다. 디버그 시연 설정 제한을 통과해야 새 입력을 시작할 수 있다.
    fun start() {
        if (!uiState.value.canStart) return
        if (demoRuntime.value?.enabled == true && !uiState.value.restrictions.canSetup) {
            mutableStartCommandFailed.value = true; return
        }
        mutableStartCommandFailed.value = false
        try {
            ContextCompat.startForegroundService(app,
                Intent(app, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_START))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RuntimeException) {
            mutableStartCommandFailed.value = true
            if (BuildConfig.DEBUG) Log.d("RoadLocation", "START_COMMAND_ERROR ${error.javaClass.simpleName}")
        }
    }

    fun stop() {
        app.startService(Intent(app, RoadNoteService::class.java).setAction(RoadNoteService.ACTION_STOP))
    }

    // 리뷰용: 뷰모델 종료 시 화면의 바인딩만 해제한다. 서비스가 보관한 수락 쓰기를 여기서 직접 취소하지 않는다.
    override fun onCleared() {
        if (bound) app.unbindService(this)
    }
}
