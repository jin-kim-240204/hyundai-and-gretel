package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

const val CREATE_ROAD_NOTE = "CREATE_ROAD_NOTE"
const val GLEO_INTENT_ACTION = "com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE"

data class GleoRoadNoteInput(val content: String, val category: NoteCategory)

sealed interface ParsedGleoAction {
    data class Valid(val input: GleoRoadNoteInput) : ParsedGleoAction
    data class Rejected(val response: GleoResponse) : ParsedGleoAction
}

fun parseGleoAction(raw: String?): ParsedGleoAction {
    if (raw == null) return validateGleoParameters(emptyMap())

    if (raw.length > MAX_ACTION_BYTES || raw.toByteArray(Charsets.UTF_8).size > MAX_ACTION_BYTES) return invalidGleoJson()
    return try {
        val tokener = JSONTokener(raw)
        val objectValue = tokener.nextValue()

        if (objectValue !is JSONObject || tokener.nextClean() != '\u0000') return invalidGleoJson()
        val values = objectValue.keys().asSequence().associateWith { key -> objectValue.get(key) }
        validateGleoParameters(values)
    } catch (_: JSONException) {
        invalidGleoJson()
    }
}

fun validateGleoParameters(values: Map<String, Any?>): ParsedGleoAction {
    val limits = mapOf("content" to 500, "locationMode" to 32, "category" to 32)
    val strings = mutableMapOf<String, String>()
    for ((key, limit) in limits) {
        val value = values[key]
        if (value !is String || value.length > limit || value.isBlank()) return invalidInput()
        strings[key] = value.trim()
    }

    if (strings["locationMode"] != "CURRENT_ROAD") return invalidInput()
    if (strings["category"] !in setOf("WARNING", "INFORMATION")) return invalidInput()

    val input = GleoRoadNoteInput(
        content = strings.getValue("content"),
        category = NoteCategory.valueOf(strings.getValue("category")),
    )
    return ParsedGleoAction.Valid(input)
}

private fun invalidInput() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The road report is incomplete or invalid.", reason = "INVALID_INPUT"),
)

internal fun invalidGleoJson() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"),
)

private const val MAX_ACTION_BYTES = 8192
