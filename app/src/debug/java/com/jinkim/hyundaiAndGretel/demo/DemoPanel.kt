package com.jinkim.hyundaiAndGretel.demo

import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.*
import kotlinx.coroutines.delay

private fun RoadNotesViewModel.demoRole(actor: String) { demoRuntime.value?.role(actor) }
private fun RoadNotesViewModel.demoMode(mode: DemoMode) { demoRuntime.value?.mode(mode) }
private fun RoadNotesViewModel.resetDemo(confirmed: Boolean) { demoRuntime.value?.reset(confirmed) }

/** An app-window overlay only. Closing never stops input or consumes a receipt. */
@Composable
fun DemoPanel(viewModel: RoadNotesViewModel, onStart: () -> Unit, onStop: () -> Unit) {
    val runtime by viewModel.demoRuntime.collectAsStateWithLifecycle()
    val tools = runtime ?: return
    val demo by tools.state.collectAsStateWithLifecycle()
    val notes by tools.notes.collectAsStateWithLifecycle()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val policy = ui.restrictions
    val sdkPermission = ContextCompat.checkSelfPermission(LocalContext.current, "pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }
    BackHandler(open) { open = false; confirmReset = false }
    if (open) LaunchedEffect(tools) { while (true) { delay(1_000); tick++ } }
    // Reserve within this composition too: SideEffect accounting runs after all rows render.
    var reserved = ui.visitedContent
    ui.inbox.pending?.let { pending ->
        val key = "receipt:${pending.recipientId}/${pending.note.noteId}"
        if (policy.canShowBody && policy.fits(pending.note.content) && policy.allowsContent(key, reserved)) reserved += key
    }
    fun content(key: String, depth: Int = 2): Boolean {
        val id = "debug:$key"
        if (!policy.allowsContent(id, reserved, depth)) return false
        reserved += id
        return true
    }
    val settingsVisible = content("settings")
    val allowed = policy.canSetup && !demo.transitioning && !tools.writesBusy && ui.submission != SubmissionState.Submitting && ui.inbox.confirming == null
    Box(Modifier.fillMaxSize()) {
        if (!open) {
            Button(onClick = { open = true }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).heightIn(min = 56.dp)) {
                PolicyText("시연 도구", policy)
            }
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { })
            Surface(Modifier.align(Alignment.CenterEnd).widthIn(max = 480.dp).fillMaxHeight(), tonalElevation = 8.dp) {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PolicyText("시연 도구 · 합성 데이터", policy, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { open = false; confirmReset = false }, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("닫기", policy) }
                        Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("중지 / 취소", policy) }
                    }
                    PolicyText("역할 ${demo.actor} · ${demo.mode} · ${demo.phase}", policy)
                    PolicyText(demo.inputStatus, policy)
                    demo.message?.let { PolicyText(it, policy) }
                    if (!policy.canSetup) PolicyText("규제 미연결 또는 설정 제한 · 중지/닫기는 가능합니다", policy)
                    if (settingsVisible) {
                        SideEffect { viewModel.markPresented("debug:settings") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.demoRole("A"); confirmReset = false }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("역할 A", policy) }
                            Button(onClick = { viewModel.demoRole("B"); confirmReset = false }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("역할 B", policy) }
                        }
                        Button(onClick = { viewModel.demoMode(DemoMode.WEB_CONSOLE) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("웹콘솔 GPS", policy) }
                        Button(onClick = { viewModel.demoMode(DemoMode.EXTERNAL_ADB) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("외부 ADB 입력", policy) }
                        Button(onClick = onStart, enabled = allowed && ui.canStart, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("선택 역할 시작 위치 설정", policy) }
                        if (demo.actor == "A") {
                            Button(onClick = { viewModel.submitTestNote("도로에 구덩이가 있어요", NoteCategory.WARNING) },
                                enabled = tools.canSubmit && ui.canSubmit, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("구덩이 테스트 제보", policy) }
                        }
                        Button(onClick = { confirmReset = true }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("새 시연 초기화", policy) }
                        if (confirmReset) {
                            PolicyText("현재 시연의 원본과 연결 이력만 삭제합니다. 다른 시연/legacy는 보존합니다.", policy)
                            Button(onClick = { viewModel.resetDemo(true); confirmReset = false }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("초기화 확인", policy) }
                            Button(onClick = { confirmReset = false }, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("취소", policy) }
                        }
                        Button(onClick = { details = !details }, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(if (details) "상세 접기" else "상세 진단", policy) }
                    } else PolicyText("플랫폼 탐색 제한으로 시연 설정/상세 표시를 보류합니다", policy)
                    if (details) {
                        val rows = listOf(
                            "scenario" to "시나리오 ${demo.scenarioId ?: "없음"}",
                            "source" to "SDK ${ui.trackingStatus} · SDK 권한 ${if (sdkPermission) "허용" else "미허용"} · 쓰기 busy ${tools.writesBusy}",
                            "target" to "대상 ${demo.targetNoteId ?: "없음"} · 합성 축 거리 ${notes.firstOrNull { it.noteId == demo.targetNoteId }?.route?.progressMeters?.let { target -> demo.current?.let { kotlin.math.abs(target - it) } } ?: "미확인"}m",
                            "buffer" to viewModel.bufferSummary(),
                            "position" to "목표 ${demo.target}m / 준비점 ${demo.probe}m / SDK ${demo.current}m",
                            "pickup" to "후보/등록 상태 ${ui.pickup}",
                            "count" to "시연 원본 ${notes.size} · 시연 수신 ${ui.inbox.items.count { it.note.scenarioId == demo.scenarioId && demo.scenarioId != null }} / 전체 PENDING ${ui.inbox.items.count { it.displayedAtUtcMillis == null }}",
                            "clock" to "SDK 수신 경과 ${demo.receivedAtElapsed?.let { SystemClock.elapsedRealtime() - it } ?: "미확인"}ms · 진단 ${tick}s",
                            "offset" to "실제 저장 offset ${(ui.submission as? SubmissionState.Saved)?.note?.anchorOffsetMillis ?: "없음"}ms",
                        )
                        rows.forEach { (key, text) ->
                            if (content(key)) { SideEffect { viewModel.markPresented("debug:$key") }; PolicyText(text, policy) }
                        }
                    }
                    if (policy.canShowPlot && content("plot")) {
                        SideEffect { viewModel.markPresented("debug:plot") }
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
                }
            }
        }
    }
}
