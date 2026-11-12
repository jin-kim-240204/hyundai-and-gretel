package com.jinkim.hyundaiAndGretel.gleo

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

fun GleoResponse.toCallbackJson(): String {
    val data = JSONObject().apply {
        val completedReport = report
        if (status == GleoStatus.SUCCESS && completedReport != null) {
            putReport(completedReport)
            put("storedLocally", true)
            noteId?.let { put("noteId", it) }
        }
        reason?.let { value -> put("reason", value) }
    }
    return JSONObject().apply {
        put("functionName", CREATE_ROAD_NOTE)
        put("status", status.name.lowercase(Locale.ROOT))
        put("message", message)
        put("mode", "voice")

        put("data", JSONArray().put(data).toString())
    }.toString()
}

private fun JSONObject.putReport(report: GleoRoadNoteInput) {
    put("content", report.content)
    put("category", report.category.name)
    put("locationMode", "CURRENT_ROAD")
}
