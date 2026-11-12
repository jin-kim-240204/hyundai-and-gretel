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
    // 리뷰용: 화면 진입점: 상태와 업무 명령은 뷰모델에 맡기고, Activity는 권한 팝업과 화면 수명을 관리한다.
    private val viewModel: RoadNotesViewModel by viewModels()
    private enum class PendingStart { NONE, WAITING_FOR_PERMISSION, PERMISSION_RESULT_READY }
    private var pendingStart = PendingStart.NONE
    private var permissionMessage by mutableStateOf("")
    // 리뷰용: 권한 응답이 돌아와도 화면이 RESUMED일 때만 대기 중인 시작 요청을 이어간다.
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
        // 리뷰용: StateFlow를 Compose 상태로 구독해 화면을 그린다. React에서 상태 변경에 따라 다시 렌더링하는 흐름과 비슷하다.
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val startCommandFailed by viewModel.startCommandFailed.collectAsStateWithLifecycle()
            val demo by viewModel.demoRuntime.collectAsStateWithLifecycle()
            // 리뷰용: 수집 중 화면이 보일 때만 화면 켜짐을 유지한다. 주행 허용 선언이나 차량 규제를 대신하는 설정은 아니다.
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

    // 리뷰용: 사용자 시작 요청: 필수 권한이 있으면 서비스 시작으로, 없으면 권한 요청으로 분기한다.
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

    // 리뷰용: 화면을 떠나면 화면 켜짐과 아직 실행하지 않은 권한 후속 요청만 정리한다. 서비스 수집을 직접 중지하지는 않는다.
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
