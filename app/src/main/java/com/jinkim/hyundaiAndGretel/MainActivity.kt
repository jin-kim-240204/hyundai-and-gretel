package com.jinkim.hyundaiAndGretel

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jinkim.hyundaiAndGretel.ui.notes.NotesScreen
import com.jinkim.hyundaiAndGretel.ui.notes.NotesViewModel
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val controller get() = (application as RoadNoteApplication).controller
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) lifecycleScope.launch { controller.startDemo() } else controller.stop("위치 권한이 필요합니다")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == NotesViewModel::class.java)
                @Suppress("UNCHECKED_CAST")
                return NotesViewModel(controller) as T
            }
        })[NotesViewModel::class.java]
        lifecycleScope.launch {
            controller.state.collect { state ->
                if (state.running && state.keepScreenOn && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        setContent { HyundaiAndGretelTheme { NotesScreen(model) {
            if (checkSelfPermission("pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED) model.start()
            else permission.launch("pleos.car.permission.FUSED_LOCATION")
        } } }
    }
    override fun onStart() {
        super.onStart()
        if (checkSelfPermission("pleos.car.permission.FUSED_LOCATION") != PackageManager.PERMISSION_GRANTED && controller.state.value.running)
            controller.stop("위치 권한이 필요합니다")
        if (controller.state.value.running && controller.state.value.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onStop() {
        if (!isChangingConfigurations) controller.stop("앱 비가시: 위치 수집 중지")
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }
}
