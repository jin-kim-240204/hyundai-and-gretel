package com.jinkim.hyundaiAndGretel.gleo

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.util.Locale

fun parseGleoAction(raw: String?, now: Instant): ParsedGleoAction {
    if (raw == null) return validateGleoParameters(emptyMap(), now)
    if (raw.length > 8192 || raw.toByteArray(Charsets.UTF_8).size > 8192) return invalidJson()
    return try {
        val tokener = JSONTokener(raw)
        val objectValue = tokener.nextValue()
        if (objectValue !is JSONObject || tokener.nextClean() != '\u0000') return invalidJson()
        val values = objectValue.keys().asSequence().associateWith { objectValue.get(it) }
        validateGleoParameters(values, now)
    } catch (_: JSONException) {
        invalidJson()
    }
}

fun GleoResponse.toCallbackJson(): String {
    val data = JSONObject().apply {
        draftId?.let { put("draftId", it) }
        draftState?.let { put("draftState", it.name) }
        if (draftId != null) put("published", false)
        missingField?.let { put("missingField", it) }
        reason?.let { put("reason", it) }
    }
    return JSONObject().apply {
        put("functionName", CREATE_ROAD_NOTE_DRAFT)
        put("status", status.name.lowercase(Locale.ROOT))
        put("message", message)
        put("mode", "voice")
        put("data", JSONArray().put(data).toString())
    }.toString()
}

private fun invalidJson() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"),
)
