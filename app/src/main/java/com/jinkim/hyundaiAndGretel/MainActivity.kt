package com.jinkim.hyundaiAndGretel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.ui.location.LocationViewModel
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LocationViewModel by viewModels()
    private var pendingStart = false
    private var permissionResultReady = false
    private var permissionMessage by mutableStateOf("")
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (BuildConfig.DEBUG) Log.d("RoadLocation", "PERMISSION_RESULT pending=$pendingStart state=${lifecycle.currentState}")
        if (pendingStart) {
            permissionResultReady = true
            completePermissionRequest()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (requiredPermissions.all(::granted)) permissionMessage = ""
                completePermissionRequest()
            }
        })
        setContent {
            val status by viewModel.status.collectAsStateWithLifecycle()
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
                        if (permissionMessage.isNotEmpty()) Text(permissionMessage)
                        Button(onClick = ::requestStart, enabled = !collecting && status != TrackingStatus.UNSUPPORTED,
                            modifier = Modifier.heightIn(min = 56.dp)) { Text("위치 수집 시작") }
                        Button(onClick = { pendingStart = false; permissionResultReady = false; viewModel.stop() }, modifier = Modifier.heightIn(min = 56.dp)) {
                            Text("위치 수집 중지")
                        }
                        if (permissionMessage.isNotEmpty()) Button(onClick = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                        }, modifier = Modifier.heightIn(min = 56.dp)) { Text("앱 권한 설정") }
                    }
                }
            }
        }
    }

    private fun requestStart() {
        if (pendingStart) return
        if (requiredPermissions.all(::granted)) {
            permissionMessage = ""
            viewModel.start()
        } else {
            pendingStart = true
            permissionResultReady = false
            val permissions = requiredPermissions + if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
            permissionRequest.launch(permissions.toTypedArray())
        }
    }

    private fun completePermissionRequest() {
        if (!pendingStart || !permissionResultReady || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        pendingStart = false
        permissionResultReady = false
        if (requiredPermissions.all(::granted)) {
            permissionMessage = ""
            viewModel.start()
        } else {
            permissionMessage = "위치 권한이 필요합니다. 거절한 경우 앱 설정에서 허용한 뒤 다시 시작해주세요."
        }
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onStop() {
        if (BuildConfig.DEBUG && pendingStart) Log.d("RoadLocation", "PERMISSION_START_CANCELLED onStop")
        pendingStart = false
        permissionResultReady = false
        super.onStop()
    }

    companion object {
        private val requiredPermissions = listOf("pleos.car.permission.FUSED_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
