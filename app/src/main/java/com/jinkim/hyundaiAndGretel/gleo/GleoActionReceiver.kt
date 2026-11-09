package com.jinkim.hyundaiAndGretel.gleo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.Instant

class GleoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val receivedAt = Instant.now()
        val startedAt = SystemClock.elapsedRealtime()
        val raw = runCatching { intent.getStringExtra("ACTION_PARAM") }
        val probeId = if (BuildConfig.DEBUG) {
            GleoProbe.begin(intent.action.orEmpty(), raw.getOrNull() ?: "ACTION_PARAM 없음/잘못된 타입", describeExtras(intent), receivedAt)
        } else -1
        if (intent.action != GLEO_INTENT_ACTION) {
            GleoProbe.complete(probeId, "지원하지 않는 함수 · 무처리", "없음", 0)
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = try {
                    withTimeout(5_000) {
                        val parsed = if (raw.isFailure) {
                            ParsedGleoAction.Rejected(GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"))
                        } else parseGleoAction(raw.getOrNull())
                        if (BuildConfig.DEBUG) previewGleoAction(parsed)
                        else GleoActionProcessor(LocalDraftStore(context)).process(parsed, receivedAt)
                    }
                } catch (_: TimeoutCancellationException) {
                    GleoResponse(GleoStatus.FAIL, "The road report could not be processed in time.", reason = "TIMEOUT")
                } catch (_: Exception) {
                    GleoResponse(GleoStatus.FAIL, "The report could not be prepared.", reason = "INVALID_INPUT")
                }
                val callback = response.toCallbackJson()
                // Debug-only preview cannot write drafts or control vehicles; release remains internal-only.
                val destination = if (BuildConfig.DEBUG) "ai.umos.ambientai" else context.packageName
                try {
                    context.sendBroadcast(
                        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")
                            .setPackage(destination)
                            .putExtra("packageName", context.packageName)
                            .putExtra("response", callback),
                    )
                    GleoProbe.complete(probeId, "${response.status} · Callback 전송 요청 → $destination (수신 완료 미확인)", callback,
                        SystemClock.elapsedRealtime() - startedAt)
                } catch (_: Exception) {
                    GleoProbe.complete(probeId, "Callback 전송 실패", callback, SystemClock.elapsedRealtime() - startedAt)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

private fun describeExtras(intent: Intent): String = runCatching {
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
