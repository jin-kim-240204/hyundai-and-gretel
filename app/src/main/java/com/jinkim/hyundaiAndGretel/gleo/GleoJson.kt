package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

fun parseGleoAction(raw: String?): ParsedGleoAction {
    if (raw == null) return validateGleoParameters(emptyMap())
    if (raw.length > 8192 || raw.toByteArray(Charsets.UTF_8).size > 8192) return invalidJson()
    return try {
        val tokener = JSONTokener(raw)
        val objectValue = tokener.nextValue()
        if (objectValue !is JSONObject || tokener.nextClean() != '\u0000') return invalidJson()
        val values = objectValue.keys().asSequence().associateWith { objectValue.get(it) }
        validateGleoParameters(values)
    } catch (_: JSONException) {
        invalidJson()
    }
}

fun GleoResponse.toCallbackJson(): String {
    val data = JSONObject().apply {
        if (status == GleoStatus.SUCCESS) report?.let {
            putReport(it)
            put("published", false)
        }
        reason?.let { put("reason", it) }
    }
    return JSONObject().apply {
        put("functionName", CREATE_ROAD_NOTE)
        put("status", status.name.lowercase(Locale.ROOT))
        put("message", message)
        put("mode", "voice")
        put("data", JSONArray().put(data).toString())
    }.toString()
}

private fun invalidJson() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"),
)

private fun JSONObject.putReport(report: RoadNoteInput) {
    put("content", report.content)
    report.category?.let { put("category", it) }
    put("locationMode", report.locationMode.name)
}
