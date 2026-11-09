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
import com.jinkim.hyundaiAndGretel.notes.routePlotRange
import com.jinkim.hyundaiAndGretel.notes.Actor
import kotlin.math.roundToInt

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
                Text(when {
                    state.actor == Actor.B -> "주변 쪽지 대기 중 · 접근하면 자동으로 수신합니다"
                    state.canReport -> "제보 준비 완료"
                    else -> "위치 수집 중 · 제보는 약30초 이력 필요"
                })
                Button(onClick = model::stop, modifier = Modifier.heightIn(min = 56.dp)) { Text("시연 중지") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("시연 중 화면 유지", Modifier.padding(top = 12.dp))
                Switch(state.keepScreenOn, model::keepScreenOn)
            }
            val location = state.location
            val markers = state.plotNotes
            if (location != null || markers.isNotEmpty()) {
                val range = routePlotRange(location?.progressMeters, markers.map { it.progressMeters })
                Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                    val inset = 20.dp.toPx()
                    fun x(progress: Double) = inset + range.fraction(progress) * (size.width-2*inset)
                    val roadY = size.height / 2
                    drawLine(Color.DarkGray, Offset(inset, roadY), Offset(size.width-inset, roadY), 4.dp.toPx())
                    markers.forEach { note ->
                        val px = x(note.progressMeters)
                        val noteY = size.height * 0.25f
                        drawLine(Color(0xFFB42318), Offset(px,noteY), Offset(px,roadY), 2.dp.toPx())
                        drawCircle(Color(0xFFB42318), 8.dp.toPx(), Offset(px,noteY))
                    }
                    location?.let {
                        val px = x(it.progressMeters)
                        val carY = size.height * 0.75f
                        drawLine(Color(0xFF175CD3), Offset(px,roadY), Offset(px,carY), 2.dp.toPx())
                        drawCircle(Color(0xFF175CD3), 10.dp.toPx(), Offset(px,carY))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${range.startMeters.roundToInt()}m")
                    Text("${range.endMeters.roundToInt()}m")
                }
                Text("파랑(아래): 현재 ${state.actor.name} · 빨강(위): 심어진 쪽지 · 같은 거리 축")
                Text("현재 위치: ${location?.progressMeters?.roundToInt()?.let { "${it}m" } ?: "SDK 수신 대기"}")
                markers.forEachIndexed { index, note ->
                    Text("쪽지 ${index+1}: ${note.progressMeters.roundToInt()}m" +
                        (location?.let { " · 현재 위치와 ${kotlin.math.abs(note.progressMeters-it.progressMeters).roundToInt()}m 차이" } ?: ""))
                }
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
            DebugPanel(state, model::actor, model::reset, model::report, model::startPosition, model::externalGps)
        }
    }
}
