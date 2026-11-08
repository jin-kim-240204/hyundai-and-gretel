package com.jinkim.hyundaiAndGretel.gleo

import android.util.Log
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
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Debug observations stay in process memory and disappear when the app process exits. */
data class GleoProbeEvent(
    val id: Long,
    val receivedAt: Instant,
    val action: String,
    val parameters: String,
    val extras: String,
    val result: String = "수신 · 검증 중",
    val callback: String = "아직 없음",
    val elapsedMs: Long? = null,
)

object GleoProbe {
    private val sequence = AtomicLong()
    private val state = MutableStateFlow<List<GleoProbeEvent>>(emptyList())
    val events = state.asStateFlow()

    fun begin(action: String, parameters: String, extras: String, receivedAt: Instant): Long {
        if (!BuildConfig.DEBUG) return -1
        val id = sequence.incrementAndGet()
        val event = GleoProbeEvent(id, receivedAt, action, parameters.take(8192), extras.take(8192))
        state.update { (listOf(event) + it).take(10) }
        log(id, "ACTION", action)
        log(id, "EXTRAS", event.extras)
        log(id, "INPUT_JSON", event.parameters)
        return id
    }

    fun complete(id: Long, result: String, callback: String, elapsedMs: Long) {
        if (!BuildConfig.DEBUG) return
        state.update { items -> items.map { if (it.id == id) it.copy(result = result, callback = callback, elapsedMs = elapsedMs) else it } }
        log(id, "RESULT", "$result elapsedMs=$elapsedMs")
        log(id, "CALLBACK_JSON", callback)
    }

    fun clear() { state.value = emptyList() }

    private fun log(id: Long, stage: String, value: String) {
        value.chunked(2000).forEachIndexed { index, chunk ->
            Log.d("GleoProbe", "[$id][$stage][$index] $chunk")
        }
    }
}

fun previewGleoAction(action: ParsedGleoAction): GleoResponse = when (action) {
    is ParsedGleoAction.Rejected -> action.response
    is ParsedGleoAction.Valid -> when {
        action.missingField != null -> GleoResponse(
            GleoStatus.ASKING, "The report ${action.missingField} is required.", missingField = action.missingField,
        )
        action.input.locationMode == LocationMode.NEEDS_CLARIFICATION -> GleoResponse(
            GleoStatus.ASKING, "Which place or landmark was the road condition near?", missingField = "placeQuery",
        )
        else -> GleoResponse(
            GleoStatus.SUCCESS,
            "The road report parameters were received and validated; nothing was saved or published.",
            reason = "PROBE_ONLY",
        )
    }
}

@Composable
fun GleoProbeScreen(events: List<GleoProbeEvent>, onClear: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gleo 음성 연동 테스트", style = MaterialTheme.typography.headlineSmall)
                    Text("함수 선택 → 수신 JSON → 검증 → Callback을 확인합니다.")
                    Text("저장 동의 없이 테스트하며 디스크 저장·게시를 하지 않습니다.")
                    Text("대기 함수: CREATE_ROAD_NOTE_DRAFT · 로그 태그: GleoProbe")
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
