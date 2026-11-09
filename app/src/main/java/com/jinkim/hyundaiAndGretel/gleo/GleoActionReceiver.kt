package com.jinkim.hyundaiAndGretel.gleo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.RoadNoteApplication
import com.jinkim.hyundaiAndGretel.demo.ReportResult
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class GleoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GLEO_ACTION) return
        val controller = (context.applicationContext as RoadNoteApplication).controller
        val receipt = controller.capture()
        val input = runCatching { decode(intent.getStringExtra("ACTION_PARAM")) }.getOrNull()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                val result = try {
                    withTimeout(5_000) {
                        if (input == null) ReportResult.Failed("INVALID_INPUT")
                        else controller.report(input.content, input.category, receipt)
                    }
                } catch (_: TimeoutCancellationException) { ReportResult.Failed("TIMEOUT") }
                catch (_: Exception) { ReportResult.Failed("SAVE_FAILED") }
                val callback = callbackJson(result)
                try {
                    context.sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE")
                        .setPackage(if (BuildConfig.DEBUG) "ai.umos.ambientai" else context.packageName)
                        .putExtra("packageName", context.packageName).putExtra("response", callback))
                    Log.i("RoadNotes", "GLEO_CALLBACK_SENT ${if (result is ReportResult.Saved) "success" else "fail"}")
                } catch (_: Exception) { Log.w("RoadNotes", "GLEO_CALLBACK_SEND_FAILED") }
            } finally { pending.finish() }
        }
    }
}

internal fun decode(raw: String?): ReportInput? {
    if (raw == null || raw.length > 8192 || raw.toByteArray().size > 8192) return null
    return runCatching {
        val tokener = JSONTokener(raw)
        val json = tokener.nextValue() as? JSONObject ?: return null
        if (tokener.nextClean() != '\u0000') return null
        validateReport(json.keys().asSequence().associateWith { json.get(it) })
    }.getOrNull()
}
internal fun callbackJson(result: ReportResult): String {
    val success = result is ReportResult.Saved
    val data = JSONObject()
    when (result) {
        is ReportResult.Saved -> {
            data.put("noteId", result.note.id).put("stored", true).put("published", false)
                .put("scope", "LOCAL_DEMO").put("content", result.note.content)
                .put("category", result.note.category.name).put("locationMode", "CURRENT_ROAD")
                .put("anchorOffsetSeconds", result.note.offsetMillis / 1000.0)
        }
        is ReportResult.Failed -> data.put("reason", result.reason)
    }
    return JSONObject().put("functionName", "CREATE_ROAD_NOTE").put("status", if (success) "success" else "fail")
        .put("message", if (success) "The road note was saved at the vehicle's approximate location from thirty seconds before the report was received."
            else "The road note could not be saved.")
        .put("mode", "voice").put("data", JSONArray().put(data).toString()).toString()
}
