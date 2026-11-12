package com.jinkim.hyundaiAndGretel.gleo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** 화면 생명주기에 맞춰 진단 StateFlow를 수집하고 기록 지우기 이벤트를 연결한다. */
@Composable
fun GleoProbeRoute() {
    val events by GleoProbe.events.collectAsStateWithLifecycle()
    GleoProbeScreen(events, GleoProbe::clear)
}

/** 전달받은 수신·처리·응답 기록을 표시하고 지우기 클릭을 상위로 전달한다. */
@Composable
fun GleoProbeScreen(events: List<GleoProbeEvent>, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gleo 음성 연동 테스트", style = MaterialTheme.typography.headlineSmall)
                    Text("함수 선택 → 수신 JSON → 검증 → Callback을 확인합니다.")
                    Text("저장 동의 없이 테스트하며 디스크 저장·게시를 하지 않습니다.")
                    Text("대기 함수: CREATE_ROAD_NOTE · 로그 태그: GleoProbe")
                    Text("수신점은 디버그 테스트용입니다. 표시된 요청의 발신자는 인증되지 않았습니다.")
                    Button(onClick = onClear) { Text("화면 기록 지우기") }
                    if (events.isEmpty()) Text("Gleo에게 도로 제보를 요청해 주세요. 아직 수신한 Action이 없습니다.")
                }
            }
            items(events, key = { it.id }) { event ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("#${event.id} · ${event.receivedAt}")
                        Text("함수: ${event.action}")
                        Text("상태: ${event.result} · ${event.elapsedMs ?: "-"} ms")
                        Text("Intent 키·타입\n${event.extras}")
                        Text("수신 JSON\n${event.parameters}")
                        Text("Callback JSON\n${event.callback}")
                    }
                }
            }
        }
    }
}
