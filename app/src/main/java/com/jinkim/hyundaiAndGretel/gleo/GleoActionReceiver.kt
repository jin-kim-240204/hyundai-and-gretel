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

    /** Gleo 요청을 받아 비동기로 처리하고, 응답 전송 시도 후 Broadcast 처리를 종료한다. */
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
        // onReceive가 끝나도 처리를 이어가며, 성공·실패·취소 경로 모두 finally에서 수신 처리를 종료한다.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = processRequest(raw, receivedAt)
                sendCallback(context, response, probeId, startedAt)
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * 입력을 파싱한 뒤 debug는 미리보기, release는 저장 처리를 실행한다.
     * 내부 제한 시간 초과는 실패 응답으로 바꾸고, 그 외 코루틴 취소는 호출자에게 전파한다.
     */
    private suspend fun processRequest(raw: Result<String?>, receivedAt: Instant): GleoResponse {
        return try {
            // 협력적 취소를 요청하는 내부 제한이다. 블로킹 파일 I/O의 즉시 중단까지 보장하지는 않는다.
            withTimeout(PROCESS_TIMEOUT_MS.milliseconds) {
                val action = if (raw.isSuccess) parseGleoAction(raw.getOrNull()) else invalidGleoJson()
                if (BuildConfig.DEBUG) previewGleoAction(action) else processor.process(action, receivedAt)
            }
        } catch (_: TimeoutCancellationException) {
            GleoResponse(GleoStatus.FAIL, "The road report could not be processed in time.", reason = "TIMEOUT")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The report could not be prepared.", reason = "INVALID_INPUT")
        }
    }

    /**
     * 처리 결과를 JSON으로 만들어 지정한 패키지에 보내고 전송 시도 결과를 기록한다.
     * sendBroadcast 호출 성공은 Gleo의 실제 수신이나 음성 안내 완료를 뜻하지 않는다.
     */
    private fun sendCallback(context: Context, response: GleoResponse, probeId: Long, startedAt: Long) {
        val callback = response.toCallbackJson()
        // debug는 Gleo 테스트 앱으로 응답하고, 외부 수신을 막은 release는 앱 내부로만 응답한다.
        val destination = if (BuildConfig.DEBUG) "ai.umos.ambientai" else context.packageName
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

    /** debug 요청만 진단 버퍼에 기록하고, release에서는 기록하지 않고 -1을 반환한다. */
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
