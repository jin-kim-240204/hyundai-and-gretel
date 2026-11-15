package com.jinkim.hyundaiAndGretel.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.service.RoadNoteService
import com.jinkim.hyundaiAndGretel.service.RoadNoteServiceContract
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DemoViewModel(private val service: StateFlow<RoadNoteServiceContract?>) : ViewModel() {
    val runtime = service.map { (it as? RoadNoteService.LocalBinder)?.demoRuntime }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectActor(actor: String) { runtime.value?.role(actor) }
    fun selectMode(mode: DemoMode) { runtime.value?.mode(mode) }
    fun resetScenario() { runtime.value?.reset(true) }
    fun bufferSnapshot() = (service.value as? RoadNoteService.LocalBinder)?.bufferSnapshot()
}
