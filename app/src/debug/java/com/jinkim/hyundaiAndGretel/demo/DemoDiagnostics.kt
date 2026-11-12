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
import androidx.core.content.ContextCompat
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.PolicyText
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesUiState
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.delay

internal val demoDiagnosticKeys = listOf("scenario", "source", "target", "buffer", "position", "pickup", "count", "clock", "offset")

@Composable
internal fun DemoDiagnostics(ui: RoadNotesUiState, demo: DemoState, notes: List<RoadNote>, visible: Set<String>, writesBusy: Boolean, bufferSummary: () -> String) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); tick++ } }
    val sdkPermission = ContextCompat.checkSelfPermission(LocalContext.current, "pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED
    val rows = listOf(
        "scenario" to "시나리오 ${demo.scenarioId ?: "없음"}",
        "source" to "SDK ${ui.trackingStatus} · SDK 권한 ${if (sdkPermission) "허용" else "미허용"} · 쓰기 busy $writesBusy",
        "target" to "대상 ${demo.targetNoteId ?: "없음"} · 합성 축 거리 ${notes.firstOrNull { it.noteId == demo.targetNoteId }?.route?.progressMeters?.let { target -> demo.current?.let { kotlin.math.abs(target - it) } } ?: "미확인"}m",
        "buffer" to bufferSummary(),
        "position" to "목표 ${demo.target}m / 준비점 ${demo.probe}m / SDK ${demo.current}m",
        "pickup" to "후보/등록 상태 ${ui.pickup}",
        "count" to "시연 원본 ${notes.size} · 시연 수신 ${ui.inbox.items.count { it.note.scenarioId == demo.scenarioId && demo.scenarioId != null }} / 전체 PENDING ${ui.inbox.items.count { it.displayedAtUtcMillis == null }}",
        "clock" to "SDK 수신 경과 ${demo.receivedAtElapsed?.let { SystemClock.elapsedRealtime() - it } ?: "미확인"}ms · 진단 ${tick}s",
        "offset" to "실제 저장 offset ${(ui.submission as? SubmissionState.Saved)?.note?.anchorOffsetMillis ?: "없음"}ms",
    )
    rows.filter { it.first in visible }.forEach { (_, text) -> PolicyText(text, ui.restrictions) }
}

@Composable
internal fun DemoRoutePlot(demo: DemoState, notes: List<RoadNote>, policy: UiRestrictions) {
    PolicyText("합성 경로 · 실제 지도 아님 · 파랑=SDK현재 / 빨강=쪽지", policy)
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
