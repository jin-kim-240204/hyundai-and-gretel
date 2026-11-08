package com.jinnayeong.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinnayeong.wakeup.seat.SeatScreen
import com.jinnayeong.wakeup.seat.SeatViewModel
import com.jinnayeong.wakeup.seat.PleosSeatSession
import com.jinnayeong.wakeup.ui.theme.HyundaiAndGretelTheme

class MainActivity : ComponentActivity() {
    private val seatModel by viewModels<SeatViewModel> {
        val context = applicationContext
        viewModelFactory { initializer { SeatViewModel { PleosSeatSession(context) } } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifecycleStartEffect(seatModel) {
                seatModel.start()
                onStopOrDispose { seatModel.stop() }
            }
            val state by seatModel.uiState.collectAsStateWithLifecycle()
            HyundaiAndGretelTheme { SeatScreen(state, seatModel::move) }
        }
    }
}
