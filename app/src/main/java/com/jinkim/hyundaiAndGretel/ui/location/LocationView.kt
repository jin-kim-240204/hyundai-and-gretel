package com.jinkim.hyundaiAndGretel.ui.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

@Composable
fun LocationView(
    status: TrackingStatus,
    startCommandFailed: Boolean,
    permissionMessage: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val collecting = status in setOf(TrackingStatus.STARTING, TrackingStatus.WAITING,
        TrackingStatus.RECEIVING, TrackingStatus.UNAVAILABLE)
    HyundaiAndGretelTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("hyundai-and-gretel", style = MaterialTheme.typography.headlineLarge)
                Text(when (status) {
                    TrackingStatus.STOPPED -> "위치 수집 중지"
                    TrackingStatus.STARTING -> "위치 수집 준비 중"
                    TrackingStatus.WAITING -> "위치 수신 대기 중"
                    TrackingStatus.RECEIVING -> "위치 수집 중"
                    TrackingStatus.UNAVAILABLE -> "위치 사용 불가 · 새 위치 대기 중"
                    TrackingStatus.UNSUPPORTED -> "이 환경은 위치 수집을 지원하지 않습니다"
                    TrackingStatus.FAILED -> "위치 수집을 시작할 수 없습니다. 권한과 위치 설정을 확인해주세요."
                })
                if (startCommandFailed) Text("위치 수집 시작 요청을 전달하지 못했습니다. 권한과 위치 설정을 확인해주세요.")
                if (permissionMessage.isNotEmpty()) Text(permissionMessage)
                Button(onClick = onStart, enabled = !collecting && status != TrackingStatus.UNSUPPORTED,
                    modifier = Modifier.heightIn(min = 56.dp)) { Text("위치 수집 시작") }
                Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("위치 수집 중지")
                }
                if (permissionMessage.isNotEmpty()) Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 56.dp)) { Text("앱 권한 설정") }
            }
        }
    }
}
