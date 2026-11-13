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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteAction
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteHeader
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStateCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStatus
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

private val locationPermissions = listOf("pleos.car.permission.FUSED_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION)

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
    Surface(Modifier.fillMaxSize(), color = PleosTheme.colors.background) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = RoadNoteDimensions.pagePadding, vertical = PleosSpacing.space04),
            verticalArrangement = Arrangement.spacedBy(PleosSpacing.space04),
        ) {
            RoadNoteHeader(policy.label(stringResource(R.string.road_note_title)))
            RoadNoteStateCard(
                title = policy.label(stringResource(R.string.location_permission_title)),
                description = policy.label(stringResource(R.string.location_permission_required)),
            )
            RoadNoteAction(
                text = policy.label(stringResource(R.string.location_permission_allow)),
                onClick = {
                    val optional = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
                    launcher.launch((locationPermissions + optional).toTypedArray())
                },
                enabled = policy.canSetup,
                modifier = Modifier.align(Alignment.End),
            )
            if (denied) {
                RoadNoteAction(
                    text = policy.label(stringResource(R.string.location_permission_settings)),
                    onClick = {
                        val appUri = Uri.parse("package:${context.packageName}")
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri))
                    },
                    enabled = policy.canSetup,
                    modifier = Modifier.align(Alignment.End),
                )
            }
            RoadNoteAction(
                text = policy.label(stringResource(R.string.location_stop)),
                onClick = onStopTracking,
                modifier = Modifier.align(Alignment.End),
            )
            if (!policy.canSetup) RoadNoteStatus(policy.label(stringResource(R.string.location_permission_restricted)))
        }
    }
}
