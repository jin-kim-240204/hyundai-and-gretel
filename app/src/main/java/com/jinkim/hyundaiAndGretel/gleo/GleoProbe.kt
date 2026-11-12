package com.jinkim.hyundaiAndGretel.gleo

import android.content.Intent
import android.util.Log
import org.json.JSONObject
import com.jinkim.hyundaiAndGretel.BuildConfig
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 프로세스가 종료되면 사라지는 debug 관측 기록이며 디스크 초안과 별개다. */
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

    /** 수신 요청에 번호를 붙여 최근 10건을 메모리에 보관하고, 원문 없는 수신 로그를 남긴다. */
    fun begin(action: String, parameters: String, extras: String, receivedAt: Instant): Long {
        if (!BuildConfig.DEBUG) return -1
        val id = sequence.incrementAndGet()
        val event = GleoProbeEvent(id, receivedAt, action, parameters.take(8192), extras.take(8192))
        // 기존 기록 목록 맨 앞에 새 기록을 붙이고 최근 10건만 남김
        state.update { events -> (listOf(event) + events).take(MAX_EVENTS) }
        Log.d("GleoProbe", "[$id] RECEIVED parameterChars=${event.parameters.length}")
        return id
    }

    /** 같은 요청 번호의 기록에 결과·응답·소요 시간을 반영한다. 이미 지운 기록은 복원하지 않는다. */
    fun complete(id: Long, result: String, callback: String, elapsedMs: Long) {
        if (!BuildConfig.DEBUG) return
        state.update { events ->
            events.map { event ->
                if (event.id == id) event.copy(result = result, callback = callback, elapsedMs = elapsedMs) else event
            }
        }
        Log.d("GleoProbe", "[$id] COMPLETED elapsedMs=$elapsedMs callbackChars=${callback.length}")
    }

    /** 화면에 표시할 진단 기록만 비운다. 디스크 초안과 진행 중인 요청에는 영향을 주지 않는다. */
    fun clear() {
        state.value = emptyList()
    }

    private const val MAX_EVENTS = 10
}

/** 진단 화면에서 Intent의 키·타입·문자열 값을 확인할 수 있도록 크기를 제한해 JSON으로 만든다. */
internal fun describeExtras(intent: Intent): String = runCatching {
    val extras = intent.extras ?: return@runCatching "{}"
    JSONObject().apply {
        extras.keySet().take(32).forEach { key ->
            @Suppress("DEPRECATION")
            val value = extras.get(key)
            put(key.take(100), JSONObject().apply {
                put("type", value?.javaClass?.simpleName ?: "null")
                put("value", if (value is String) value.take(8192) else "값 출력 생략")
            })
        }
    }.toString(2)
}.getOrElse { "Intent extras를 읽지 못함" }
