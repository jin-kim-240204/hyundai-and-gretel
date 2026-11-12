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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.ui.location.LocationViewModel
import com.jinkim.hyundaiAndGretel.ui.location.LocationView

class MainActivity : ComponentActivity() {
    private val viewModel: LocationViewModel by viewModels()
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
            LocationView(
                state = uiState,
                startCommandFailed = startCommandFailed,
                permissionMessage = permissionMessage,
                onStart = ::requestStart,
                onSubmitTestNote = viewModel::submitTestNote,
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

    override fun onStop() {
        if (BuildConfig.DEBUG && pendingStart != PendingStart.NONE) Log.d("RoadLocation", "PERMISSION_START_CANCELLED onStop")
        pendingStart = PendingStart.NONE
        super.onStop()
    }

    companion object {
        private val requiredPermissions = listOf("pleos.car.permission.FUSED_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
