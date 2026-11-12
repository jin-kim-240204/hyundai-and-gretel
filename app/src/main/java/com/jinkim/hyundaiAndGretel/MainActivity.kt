package com.jinkim.hyundaiAndGretel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.demo.DemoPanel
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesScreen
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RoadNotesViewModel by viewModels()
    private enum class PendingStart { NONE, WAITING_FOR_PERMISSION, PERMISSION_RESULT_READY }
    private var pendingStart = PendingStart.NONE
    private var permissionMessage by mutableStateOf("")
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (BuildConfig.DEBUG) Log.d("RoadLocation", "PERMISSION_RESULT pending=${pendingStart != PendingStart.NONE} state=${lifecycle.currentState}")
        if (pendingStart != PendingStart.NONE) {
            pendingStart = PendingStart.PERMISSION_RESULT_READY
            completePermissionRequest()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasRequiredLocationPermissions()) permissionMessage = ""
                completePermissionRequest()
            }
        })
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val startCommandFailed by viewModel.startCommandFailed.collectAsStateWithLifecycle()
            val demo by viewModel.demoRuntime.collectAsStateWithLifecycle()
            DisposableEffect(uiState.trackingStatus.isCollecting) {
                if (uiState.trackingStatus.isCollecting && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            RoadNotesScreen(
                state = uiState,
                startCommandFailed = startCommandFailed,
                permissionMessage = permissionMessage,
                onStart = ::requestStart,
                startAllowed = demo?.enabled != true || (uiState.restrictions.canSetup && demo?.transitioning != true),
                onContentShown = viewModel::markPresented,
                debugTools = { DemoPanel(viewModel, onStart = ::requestStart, onStop = viewModel::stop) },
                onConfirmReceipt = { key ->
                    viewModel.confirmReceipt(key, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                },
                onStop = {
                    pendingStart = PendingStart.NONE
                    viewModel.stop()
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                },
            )
        }
    }

    private fun requestStart() {
        if (pendingStart != PendingStart.NONE) return
        if (hasRequiredLocationPermissions()) {
            permissionMessage = ""
            viewModel.start()
        } else {
            pendingStart = PendingStart.WAITING_FOR_PERMISSION
            val permissions = requiredPermissions + if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
            permissionRequest.launch(permissions.toTypedArray())
        }
    }

    private fun completePermissionRequest() {
        if (pendingStart != PendingStart.PERMISSION_RESULT_READY || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        pendingStart = PendingStart.NONE
        if (hasRequiredLocationPermissions()) {
            permissionMessage = ""
            viewModel.start()
        } else {
            permissionMessage = getString(R.string.location_permission_required)
        }
    }

    private fun hasRequiredLocationPermissions() = requiredPermissions.all(::granted)

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onStart() {
        super.onStart()
        if (viewModel.uiState.value.trackingStatus.isCollecting) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (BuildConfig.DEBUG && pendingStart != PendingStart.NONE) Log.d("RoadLocation", "PERMISSION_START_CANCELLED onStop")
        pendingStart = PendingStart.NONE
        super.onStop()
    }

    companion object {
        private val requiredPermissions = listOf("pleos.car.permission.FUSED_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
