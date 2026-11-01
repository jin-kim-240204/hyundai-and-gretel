package com.jinnayeong.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jinnayeong.wakeup.ui.theme.WAKEUPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WAKEUPTheme { WakeUpScreen() }
        }
    }
}

@Composable
private fun WakeUpScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("WAKE UP", style = MaterialTheme.typography.headlineLarge)
            Text("쾌적한 휴식과 기상을 도와주는 서비스")
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun WakeUpPreview() {
    WAKEUPTheme { WakeUpScreen() }
}
