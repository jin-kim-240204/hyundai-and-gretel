package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput

const val CREATE_ROAD_NOTE = "CREATE_ROAD_NOTE"
const val GLEO_INTENT_ACTION = "com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE"

sealed interface ParsedGleoAction {
    data class Valid(val input: RoadNoteInput) : ParsedGleoAction
    data class Rejected(val response: GleoResponse) : ParsedGleoAction
}

// JSONObject is decoded at the Android boundary; this function never coerces input types.
fun validateGleoParameters(values: Map<String, Any?>): ParsedGleoAction {
    val limits = mapOf("content" to 500, "locationMode" to 32, "category" to 32)
    val strings = mutableMapOf<String, String>()
    for ((key, limit) in limits) {
        val value = values[key]
        if (value !is String || value.length > limit || value.isBlank()) return invalidInput()
        strings[key] = value.trim()
    }
    if (strings["locationMode"] != "CURRENT_ROAD" ||
        strings["category"] !in setOf("WARNING", "INFORMATION")) return invalidInput()
    return ParsedGleoAction.Valid(RoadNoteInput(
        content = strings.getValue("content"),
        category = strings.getValue("category"),
        locationMode = LocationMode.CURRENT_ROAD,
        directionMode = DirectionMode.CURRENT_VEHICLE,
    ))
}

private fun invalidInput() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The road report is incomplete or invalid.", reason = "INVALID_INPUT"),
)
