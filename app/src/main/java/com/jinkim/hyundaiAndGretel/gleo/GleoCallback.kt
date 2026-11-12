package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * 처리 결과를 Gleo Callback 규격의 JSON 문자열로 변환한다.
 * 성공 데이터에도 published=false를 넣어 수신/로컬 저장과 실제 게시를 구분한다.
 */
fun GleoResponse.toCallbackJson(): String {
    val data = JSONObject().apply {
        val completedReport = report
        if (status == GleoStatus.SUCCESS && completedReport != null) {
            putReport(completedReport)
            put("published", false)
        }
        reason?.let { value -> put("reason", value) }
    }
    return JSONObject().apply {
        put("functionName", CREATE_ROAD_NOTE)
        put("status", status.name.lowercase(Locale.ROOT))
        put("message", message)
        put("mode", "voice")
        // Gleo 규격상 data는 배열 자체가 아니라 JSON 객체 배열을 직렬화한 문자열이다.
        put("data", JSONArray().put(data).toString())
    }.toString()
}

/** 응답 객체에 공개 입력 계약의 내용·분류·위치 표현만 넣는다. */
private fun JSONObject.putReport(report: RoadNoteInput) {
    put("content", report.content)
    report.category?.let { category -> put("category", category) }
    put("locationMode", report.locationMode.name)
}
