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

        state.update { events -> (listOf(event) + events).take(MAX_EVENTS) }
        Log.d("GleoProbe", "[$id] RECEIVED parameterChars=${event.parameters.length}")
        return id
    }

    fun complete(id: Long, result: String, callback: String, elapsedMs: Long) {
        if (!BuildConfig.DEBUG) return
        state.update { events ->
            events.map { event ->
                if (event.id == id) event.copy(result = result, callback = callback, elapsedMs = elapsedMs) else event
            }
        }
        Log.d("GleoProbe", "[$id] COMPLETED elapsedMs=$elapsedMs callbackChars=${callback.length}")
    }

    fun clear() {
        state.value = emptyList()
    }

    private const val MAX_EVENTS = 10
}

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
