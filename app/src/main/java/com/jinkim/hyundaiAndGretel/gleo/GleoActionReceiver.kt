package com.jinkim.hyundaiAndGretel.gleo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.jinkim.hyundaiAndGretel.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class GleoActionReceiver : BroadcastReceiver() {
    @Inject lateinit var processor: GleoActionProcessor

    override fun onReceive(context: Context, intent: Intent) {
        val receivedAt = Instant.now()
        val startedAt = SystemClock.elapsedRealtime()
        val raw = runCatching { intent.getStringExtra("ACTION_PARAM") }
        val probeId = recordRequest(intent, raw, receivedAt)
        if (intent.action != GLEO_INTENT_ACTION) {
            GleoProbe.complete(probeId, "지원하지 않는 함수 · 무처리", "없음", 0)
            return
        }

        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = processRequest(raw, startedAt)
                sendCallback(context, response, probeId, startedAt)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun processRequest(raw: Result<String?>, receivedElapsedMillis: Long): GleoResponse {
        return try {

            withTimeout(PROCESS_TIMEOUT_MS.milliseconds) {
                val action = if (raw.isSuccess) parseGleoAction(raw.getOrNull()) else invalidGleoJson()
                processor.process(action, receivedElapsedMillis)
            }
        } catch (_: TimeoutCancellationException) {
            GleoResponse(GleoStatus.FAIL, "The save result could not be confirmed in time. Please check the app before retrying.", reason = "TIMEOUT")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The road report could not be processed.", reason = "INVALID_INPUT")
        }
    }

    private fun sendCallback(context: Context, response: GleoResponse, probeId: Long, startedAt: Long) {
        val callback = response.toCallbackJson()

        val destination = "ai.umos.ambientai"
        val intent = Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")
            .setPackage(destination)
            .putExtra("packageName", context.packageName)
            .putExtra("response", callback)
        val result = try {
            context.sendBroadcast(intent)
            "${response.status} · Callback 전송 요청 → $destination (수신 완료 미확인)"
        } catch (_: Exception) {
            "Callback 전송 실패"
        }
        GleoProbe.complete(probeId, result, callback, SystemClock.elapsedRealtime() - startedAt)
    }

    private fun recordRequest(intent: Intent, raw: Result<String?>, receivedAt: Instant): Long {
        if (!BuildConfig.DEBUG) return -1
        return GleoProbe.begin(
            action = intent.action.orEmpty(),
            parameters = raw.getOrNull() ?: "ACTION_PARAM 없음/잘못된 타입",
            extras = describeExtras(intent),
            receivedAt = receivedAt,
        )
    }

    private companion object {
        const val PROCESS_TIMEOUT_MS = 5_000L
    }
}
