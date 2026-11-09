package com.jinkim.hyundaiAndGretel.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.demo.DemoController
import com.jinkim.hyundaiAndGretel.notes.Actor
import com.jinkim.hyundaiAndGretel.notes.Category
import kotlinx.coroutines.launch

class NotesViewModel(private val controller: DemoController) : ViewModel() {
    val state = controller.state
    fun keepScreenOn(enabled: Boolean) = controller.keepScreenOn(enabled)
    fun start() = controller.start()
    fun stop() = controller.stop()
    fun actor(actor: Actor) { viewModelScope.launch { controller.switchActor(actor) } }
    fun reset() { viewModelScope.launch { runCatching { controller.reset() }.onFailure { controller.operationFailed("초기화 실패") } } }
    fun report(content: String) { viewModelScope.launch {
        val result = controller.report(content, Category.WARNING)
        if (result is com.jinkim.hyundaiAndGretel.demo.ReportResult.Failed) controller.operationFailed(result.reason)
    } }
    fun displayed(id: String) = controller.displayed(id)
}
