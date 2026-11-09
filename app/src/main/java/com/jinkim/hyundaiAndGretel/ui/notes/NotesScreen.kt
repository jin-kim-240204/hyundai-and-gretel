package com.jinkim.hyundaiAndGretel.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinkim.hyundaiAndGretel.notes.DemoRoute

@Composable
fun NotesScreen(model: NotesViewModel, onStart: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var consent by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("도로 쪽지", style = MaterialTheme.typography.headlineLarge)
            Text(state.status)
            if (!state.running) {
                Text("합성 GPS로 위치를 수집하고 쪽지를 이 기기에 저장하는 시연입니다.")
                Text("실제 위치는 주입하지 마세요. 앱을 벗어나면 수집을 중단합니다.")
                Row {
                    Checkbox(consent, { consent = it })
                    Text("합성 위치 시연에 동의합니다", Modifier.padding(top = 12.dp))
                }
                Button(onClick = onStart, enabled = consent, modifier = Modifier.heightIn(min = 56.dp)) { Text("시연 시작") }
            } else {
                Text("사용자 ${state.actor.name} · 위치 기록 ${state.bufferSeconds}초")
                Text(if (state.canReport) "제보 준비 완료" else "위치 수집 중 · 제보는 약30초 이력 필요")
                Button(onClick = model::stop, modifier = Modifier.heightIn(min = 56.dp)) { Text("시연 중지") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("시연 중 화면 유지", Modifier.padding(top = 12.dp))
                Switch(state.keepScreenOn, model::keepScreenOn)
            }
            val location = state.location
            if (location != null) {
                val markers = state.nearbyNotes.filter { it.expiresAt > System.currentTimeMillis() }.map { it.progressMeters }
                val start = location.progressMeters - 400
                Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                    drawLine(Color.DarkGray, Offset(0f,size.height/2), Offset(size.width,size.height/2),8f)
                    markers.forEach { progress ->
                        val x = ((progress-start)/800 * size.width).toFloat()
                        if (x in 0f..size.width) drawCircle(Color(0xFFB42318), 12f, Offset(x,size.height/2))
                    }
                    drawCircle(Color(0xFF175CD3), 14f, Offset(size.width/2,size.height/2))
                }
                Text("파랑: 현재 위치 · 빨강: 심어진 쪽지 · 시연 경로 ${DemoRoute.ID}")
            }
            if (state.lastResult.isNotEmpty()) Text(state.lastResult)
            var inboxIndex by remember(state.actor) { mutableIntStateOf(0) }
            val item = state.inbox.getOrNull(inboxIndex.coerceAtMost((state.inbox.size - 1).coerceAtLeast(0)))
            if (item != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(if (item.note.category.name == "WARNING") "도로 경고" else "도로 안내", style = MaterialTheme.typography.titleLarge)
                        Text(item.note.content, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                if (state.inbox.size > 1) {
                    TextButton(onClick = { inboxIndex = (inboxIndex + 1) % state.inbox.size }) {
                        Text("다음 쪽지 (${inboxIndex + 1}/${state.inbox.size})")
                    }
                }
                LaunchedEffect(item.note.id, state.actor) { model.displayed(item.note.id) }
            }
            DebugPanel(state, model::actor, model::reset, model::report)
        }
    }
}
