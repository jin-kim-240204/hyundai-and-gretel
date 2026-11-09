package com.jinkim.hyundaiAndGretel.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.demo.DemoState
import com.jinkim.hyundaiAndGretel.notes.Actor

@Composable
fun DebugPanel(state: DemoState, onActor: (Actor) -> Unit, onReset: () -> Unit, onReport: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = !open }, modifier = Modifier.heightIn(min = 56.dp)) { Text(if (open) "디버그 닫기" else "디버그") }
    if (open) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("시연 도구 · 합성 위치만 사용")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Actor.entries.forEach { actor ->
                        Button(onClick = { onActor(actor) }, enabled = actor != state.actor) { Text("사용자 ${actor.name}") }
                    }
                }
                Text("버퍼 ${state.bufferSeconds}초 · 주변 쪽지 ${state.noteCount} · 후보 ${state.candidateCount}")
                Text("접근 등록 ${state.registeredCount} · 초과 ${state.overflowCount} · 최근 ${state.lastEvent}")
                state.nearbyNotes.take(5).forEach { note ->
                    Text("쪽지 위치 ${note.progressMeters.toInt()}m · 수신 ${note.offsetMillis / 1000}초 전 기준")
                }
                Text("H3 res9: ${state.h3Cell}")
                state.location?.let { Text("좌표 ${it.latitude}, ${it.longitude} · 경로 ${it.progressMeters.toInt()}m") }
                Button(onClick = { onReport("도로에 구덩이가 있어요") }, enabled = state.canReport) { Text("구덩이 테스트 제보") }
                OutlinedButton(onClick = { confirmReset = true }) { Text("테스트 데이터 초기화") }
            }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("시연 데이터를 삭제할까요?") }, text = { Text("쪽지와 수신 이력을 삭제하고 위치 기록을 비웁니다.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("초기화") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } })
}
