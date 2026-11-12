package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoadNotesViewModel(app: Application) : AndroidViewModel(app) {
    internal val serviceClient = RoadNoteServiceClient(app, viewModelScope)
    private val visited = MutableStateFlow<Set<String>>(emptySet())
    val startCommandFailed = serviceClient.startCommandFailed
    val uiState = combine(serviceClient.state, visited, startCommandFailed) { state, seen, failed ->
        state.copy(visitedContent = seen, startCommandFailed = failed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RoadNotesUiState())
    val status = uiState.map { it.trackingStatus }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrackingStatus.STOPPED)
    val canStartTracking get() = serviceClient.canStartTracking

    fun markPresented(key: String) { visited.value += key }
    fun startTracking() = serviceClient.startTracking()
    fun stopTracking() = serviceClient.stopTracking()
    fun submitRoadNote(content: String, category: NoteCategory) = serviceClient.submitRoadNote(content, category)
    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean) {
        serviceClient.confirmReceipt(key, screenResumed, visited.value.toSet())
    }

    override fun onCleared() {
        serviceClient.close()
    }
}
