package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

const val CREATE_ROAD_NOTE = "CREATE_ROAD_NOTE"
const val GLEO_INTENT_ACTION = "com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE"

sealed interface ParsedGleoAction {
    data class Valid(val input: RoadNoteInput) : ParsedGleoAction
    data class Rejected(val response: GleoResponse) : ParsedGleoAction
}

/** 수신 문자열의 크기와 JSON 객체 형식을 검사한 뒤 필드 검증으로 넘긴다. */
fun parseGleoAction(raw: String?): ParsedGleoAction {
    if (raw == null) return validateGleoParameters(emptyMap())
    // 문자 수로 과도한 입력을 먼저 거르고, 한글 등 다중 바이트 문자는 UTF-8 크기로 다시 제한한다.
    if (raw.length > MAX_ACTION_BYTES || raw.toByteArray(Charsets.UTF_8).size > MAX_ACTION_BYTES) return invalidGleoJson()
    return try {
        val tokener = JSONTokener(raw)
        val objectValue = tokener.nextValue()
        // 객체 뒤에 다른 JSON이나 문자열이 붙은 요청도 거절한다.
        if (objectValue !is JSONObject || tokener.nextClean() != '\u0000') return invalidGleoJson()
        val values = objectValue.keys().asSequence().associateWith { key -> objectValue.get(key) }
        validateGleoParameters(values)
    } catch (_: JSONException) {
        invalidGleoJson()
    }
}

/**
 * 필수 문자열의 타입·길이·허용값을 검사해 RoadNoteInput을 만든다.
 * 숫자 등을 문자열로 강제 변환하지 않으며, 계약에 없는 키는 실행에 사용하지 않는다.
 */
fun validateGleoParameters(values: Map<String, Any?>): ParsedGleoAction {
    val limits = mapOf("content" to 500, "locationMode" to 32, "category" to 32)
    val strings = mutableMapOf<String, String>()
    for ((key, limit) in limits) {
        val value = values[key]
        if (value !is String || value.length > limit || value.isBlank()) return invalidInput()
        strings[key] = value.trim()
    }
    // 현재 지원하는 위치 표현은 CURRENT_ROAD뿐
    if (strings["locationMode"] != "CURRENT_ROAD") return invalidInput()
    if (strings["category"] !in setOf("WARNING", "INFORMATION")) return invalidInput()

    val input = RoadNoteInput(
        content = strings.getValue("content"),
        category = strings.getValue("category"),
        locationMode = LocationMode.CURRENT_ROAD,
        directionMode = DirectionMode.CURRENT_VEHICLE,
    )
    return ParsedGleoAction.Valid(input)
}

/** 필수값 누락이나 잘못된 필드 값에 대한 실패 결과를 만든다. */
private fun invalidInput() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The road report is incomplete or invalid.", reason = "INVALID_INPUT"),
)

/** 잘못된 JSON 형식 또는 수신 데이터 타입에 대한 실패 결과를 만든다. */
internal fun invalidGleoJson() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"),
)

private const val MAX_ACTION_BYTES = 8192
