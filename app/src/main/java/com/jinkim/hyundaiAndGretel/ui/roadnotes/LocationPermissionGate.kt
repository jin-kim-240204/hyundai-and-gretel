package com.jinkim.hyundaiAndGretel.ui.roadnotes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

private val locationPermissions = listOf("pleos.car.permission.FUSED_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION)

/** 권한이 없으면 안내 화면을 보여준다. 권한 허용만으로 수집을 자동 시작하지 않는다. */
@Composable
internal fun LocationPermissionGate(policy: UiRestrictions, onStopTracking: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun hasPermissions() = locationPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(hasPermissions()) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermissions()
        denied = !granted
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasPermissions()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (granted) {
        content()
        return
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PolicyText(stringResource(R.string.location_permission_required), policy)
            Button(
                onClick = {
                    val optional = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
                    launcher.launch((locationPermissions + optional).toTypedArray())
                },
                enabled = policy.canSetup,
                modifier = Modifier.heightIn(min = 56.dp),
            ) { PolicyText("위치 권한 허용", policy) }
            if (denied) {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) },
                    enabled = policy.canSetup,
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { PolicyText(stringResource(R.string.location_permission_settings), policy) }
            }
            Button(onClick = onStopTracking, modifier = Modifier.heightIn(min = 56.dp)) {
                PolicyText(stringResource(R.string.location_stop), policy)
            }
            if (!policy.canSetup) PolicyText("차량 규제 연결 및 설정 허용 상태에서 권한을 설정해주세요", policy)
        }
    }
}
