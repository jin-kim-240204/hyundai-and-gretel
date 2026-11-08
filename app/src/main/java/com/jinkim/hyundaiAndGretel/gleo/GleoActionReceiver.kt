package com.jinkim.hyundaiAndGretel.gleo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant

class GleoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CREATE_ROAD_NOTE_DRAFT) return
        val receivedAt = Instant.now()
        val startedAt = SystemClock.elapsedRealtime()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = try {
                    withTimeout(5_000) {
                        val parsed = parseGleoAction(intent.getStringExtra("ACTION_PARAM"), receivedAt)
                        GleoActionProcessor(LocalDraftStore(context)).process(parsed, receivedAt)
                    }
                } catch (_: TimeoutCancellationException) {
                    null
                } catch (_: Exception) {
                    GleoResponse(GleoStatus.FAIL, "The report draft could not be prepared.", reason = "INVALID_INPUT")
                }
                if (response != null && SystemClock.elapsedRealtime() - startedAt < 5_000) {
                    // shortcut: external Gleo trust/destination is unverified; keep callbacks inside this app until confirmed.
                    context.sendBroadcast(
                        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")
                            .setPackage(context.packageName)
                            .putExtra("packageName", context.packageName)
                            .putExtra("response", response.toCallbackJson()),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
