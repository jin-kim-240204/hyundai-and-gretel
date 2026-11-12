package com.jinkim.hyundaiAndGretel.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jinkim.hyundaiAndGretel.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.*
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

/** Activity와 같은 뷰모델을 받는다. 시연 동작은 그 서비스의 DemoRuntime이 처리한다. */
@Composable
fun DemoPanel(viewModel: RoadNotesViewModel, onStart: () -> Unit, onStop: () -> Unit) {
    val runtime by viewModel.demoRuntime.collectAsStateWithLifecycle()
    val tools = runtime ?: return
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    BackHandler(open) { open = false }
    Box(Modifier.fillMaxSize()) {
        if (open) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { })
            Surface(Modifier.align(Alignment.CenterEnd).widthIn(max = 480.dp).fillMaxHeight(), tonalElevation = 8.dp) {
                DemoPanelContents(viewModel, tools, ui, onStart, onStop, onClose = { open = false })
            }
        } else {
            Button(onClick = { open = true }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).heightIn(min = 56.dp)) {
                PolicyText("시연 도구", ui.restrictions)
            }
        }
    }
}

@Composable
private fun DemoPanelContents(viewModel: RoadNotesViewModel, tools: DemoRuntime, ui: RoadNotesUiState, onStart: () -> Unit, onStop: () -> Unit, onClose: () -> Unit) {
    val demo by tools.state.collectAsStateWithLifecycle()
    val notes by tools.notes.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf(false) }
    val policy = ui.restrictions
    val visible = visibleDemoContent(ui, details)
    val settingsAllowed = policy.canSetup && !demo.transitioning && !tools.writesBusy &&
        ui.submission != SubmissionState.Submitting && ui.inbox.confirming == null
    SideEffect { visible.forEach { viewModel.markPresented("debug:$it") } }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PolicyText("시연 도구 · 합성 데이터", policy, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClose, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("닫기", policy) }
            Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("중지 / 취소", policy) }
        }
        PolicyText("${if (demo.actor == "A") "제보자 A" else "수신자 B"} · ${demo.mode} · ${demo.phase}", policy)
        PolicyText(demo.inputStatus, policy)
        demo.message?.let { PolicyText(it, policy) }
        if (!policy.canSetup) PolicyText("규제 미연결 또는 설정 제한 · 중지/닫기는 가능합니다", policy)

        if ("settings" in visible) {
            DemoRoleAndInputControls(tools, demo, settingsAllowed, ui.canStart, onStart, policy)
            if (demo.actor == "A") {
                val content = stringResource(R.string.debug_note_fixture)
                Button(
                    onClick = { viewModel.submitRoadNote(content, NoteCategory.WARNING) },
                    enabled = tools.canSubmit && ui.canSubmit,
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { PolicyText(stringResource(R.string.debug_note_submit), policy) }
            }
            DemoResetControl(tools, settingsAllowed, policy)
            Button(onClick = { details = !details }, modifier = Modifier.heightIn(min = 56.dp)) {
                PolicyText(if (details) "상세 접기" else "상세 진단", policy)
            }
        } else PolicyText("플랫폼 탐색 제한으로 시연 설정/상세 표시를 보류합니다", policy)

        if (details) DemoDiagnostics(ui, demo, notes, visible, tools.writesBusy, viewModel::bufferSummary)
        if ("plot" in visible) DemoRoutePlot(demo, notes, policy)
    }
}

@Composable
private fun DemoRoleAndInputControls(tools: DemoRuntime, demo: DemoState, allowed: Boolean, canStart: Boolean, onStart: () -> Unit, policy: UiRestrictions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { tools.role("A") }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("제보자 A", policy) }
        Button(onClick = { tools.role("B") }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("수신자 B", policy) }
    }
    Button(onClick = { tools.mode(DemoMode.WEB_CONSOLE) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("웹콘솔 GPS", policy) }
    Button(onClick = { tools.mode(DemoMode.EXTERNAL_ADB) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("외부 ADB 입력", policy) }
    Button(onClick = onStart, enabled = allowed && canStart, modifier = Modifier.heightIn(min = 56.dp)) {
        PolicyText("${if (demo.actor == "A") "제보자" else "수신자"} 시작 위치 설정", policy)
    }
}

@Composable
private fun DemoResetControl(tools: DemoRuntime, allowed: Boolean, policy: UiRestrictions) {
    val demo by tools.state.collectAsStateWithLifecycle()
    var confirming by remember(demo.actor, demo.mode, demo.phase) { mutableStateOf(false) }
    Button(onClick = { confirming = true }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("새 시연 초기화", policy) }
    if (confirming) {
        PolicyText("현재 시연의 원본과 연결 이력만 삭제합니다. 다른 시연/legacy는 보존합니다.", policy)
        Button(onClick = { tools.reset(true); confirming = false }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("초기화 확인", policy) }
        Button(onClick = { confirming = false }, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText("취소", policy) }
    }
}

/** 같은 화면 구성에서 제품 카드와 디버그 행이 표시 예산을 중복 사용하지 않게 순서대로 계산한다. */
internal fun visibleDemoContent(ui: RoadNotesUiState, details: Boolean): Set<String> {
    val policy = ui.restrictions
    var presented = ui.visitedContent
    ui.inbox.pending?.let { item ->
        val key = "receipt:${item.recipientId}/${item.note.noteId}"
        if (policy.canShowBody && policy.fits(item.note.content) && policy.allowsContent(key, presented)) presented += key
    }
    val sections = buildList {
        add("settings")
        if (details) addAll(demoDiagnosticKeys)
        if (policy.canShowPlot) add("plot")
    }
    return sections.filterTo(linkedSetOf()) { key ->
        val id = "debug:$key"
        policy.allowsContent(id, presented, depth = 2).also { allowed -> if (allowed) presented += id }
    }
}
