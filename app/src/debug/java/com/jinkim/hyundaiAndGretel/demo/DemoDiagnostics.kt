package com.jinkim.hyundaiAndGretel.demo

import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.LocationBufferSnapshot
import androidx.core.content.ContextCompat
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.PolicyText
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesUiState
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.delay

internal val demoDiagnosticKeys = listOf("scenario", "source", "target", "buffer", "position", "pickup", "count", "clock", "offset")

@Composable
internal fun DemoDiagnostics(ui: RoadNotesUiState, demo: DemoState, notes: List<RoadNote>, visible: Set<String>, writesBusy: Boolean, bufferSnapshot: () -> LocationBufferSnapshot?) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); tick++ } }
    val sdkPermission = ContextCompat.checkSelfPermission(LocalContext.current, "pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED
    val unknown = stringResource(R.string.debug_value_unknown)
    val none = stringResource(R.string.debug_value_none)
    val snapshot = bufferSnapshot()
    val bufferText = if (snapshot == null) stringResource(R.string.debug_buffer_waiting) else {
        val missing = stringResource(R.string.debug_missing_value)
        stringResource(R.string.debug_buffer_summary, snapshot.sampleCount,
            snapshot.oldestReceivedElapsedMillis?.toString() ?: missing,
            snapshot.latestReceivedElapsedMillis?.toString() ?: missing)
    }
    val targetProgress = notes.firstOrNull { it.noteId == demo.targetNoteId }?.route?.progressMeters
    val distance = targetProgress?.let { target -> demo.current?.let { kotlin.math.abs(target - it) } }
    val permission = stringResource(if (sdkPermission) R.string.debug_permission_allowed else R.string.debug_permission_denied)
    val rows = listOf(
        "scenario" to stringResource(R.string.debug_diagnostic_scenario, demo.scenarioId ?: none),
        "source" to stringResource(R.string.debug_diagnostic_source, ui.trackingStatus, permission, writesBusy),
        "target" to stringResource(R.string.debug_diagnostic_target, demo.targetNoteId ?: none, distance?.toString() ?: unknown),
        "buffer" to bufferText,
        "position" to stringResource(R.string.debug_diagnostic_position, demo.target?.toString() ?: unknown, demo.probe?.toString() ?: unknown, demo.current?.toString() ?: unknown),
        "pickup" to stringResource(R.string.debug_diagnostic_pickup, ui.pickup),
        "count" to stringResource(R.string.debug_diagnostic_count, notes.size,
            ui.inbox.items.count { it.note.scenarioId == demo.scenarioId && demo.scenarioId != null },
            ui.inbox.items.count { it.displayedAtUtcMillis == null }),
        "clock" to stringResource(R.string.debug_diagnostic_clock, demo.receivedAtElapsed?.let { (SystemClock.elapsedRealtime() - it).toString() } ?: unknown, tick),
        "offset" to stringResource(R.string.debug_diagnostic_offset, (ui.submission as? SubmissionState.Saved)?.note?.anchorOffsetMillis?.toString() ?: none),
    )
    rows.filter { it.first in visible }.forEach { (_, text) -> PolicyText(text, ui.restrictions) }
}

@Composable
internal fun DemoRoutePlot(demo: DemoState, notes: List<RoadNote>, policy: UiRestrictions) {
    PolicyText(stringResource(R.string.debug_route_legend), policy)
    val points = notes.mapNotNull { it.route?.progressMeters }
    val current = demo.current
    val end = maxOf(1_500.0, (points + listOfNotNull(current)).maxOrNull()?.plus(100) ?: 1_500.0)
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        fun x(progress: Double) = (progress / end * size.width).toFloat()
        drawLine(Color.Gray, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
        points.forEach { drawCircle(Color.Red, 7.dp.toPx(), Offset(x(it), size.height * 0.3f)) }
        current?.let { drawCircle(Color.Blue, 7.dp.toPx(), Offset(x(it), size.height * 0.7f)) }
    }
}
