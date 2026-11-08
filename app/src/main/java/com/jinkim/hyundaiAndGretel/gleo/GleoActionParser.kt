package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import java.time.Instant

const val CREATE_ROAD_NOTE_DRAFT = "CREATE_ROAD_NOTE_DRAFT"
const val GLEO_INTENT_ACTION = "com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE_DRAFT"

sealed interface ParsedGleoAction {
    data class Valid(val input: RoadNoteInput, val missingField: String? = null) : ParsedGleoAction
    data class Rejected(val response: GleoResponse) : ParsedGleoAction
}

// JSONObject is decoded at the Android boundary; this function never coerces input types.
fun validateGleoParameters(values: Map<String, Any?>, now: Instant): ParsedGleoAction {
    val limits = mapOf(
        "content" to 500, "locationMode" to 32, "placeQuery" to 200,
        "directionMode" to 32, "directionDescription" to 200,
        "utteranceRecordedAt" to 40, "draftId" to 128,
    )
    val strings = mutableMapOf<String, String>()
    for ((key, limit) in limits) {
        if (!values.containsKey(key)) continue
        val value = values[key]
        if (value !is String || value.length > limit) return invalidInput()
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            if (key == "content" || key == "locationMode") continue
            return invalidInput()
        }
        strings[key] = trimmed
    }
    val content = strings["content"] ?: return missingInput("content")
    val location = strings["locationMode"] ?: return missingInput("locationMode")
    val locationMode = LocationMode.entries.find { it.name == location } ?: return invalidInput()
    val directionMode = strings["directionMode"]?.let { value ->
        DirectionMode.entries.find { it.name == value } ?: return invalidInput()
    } ?: if (locationMode == LocationMode.CURRENT_ROAD) {
        DirectionMode.CURRENT_VEHICLE
    } else {
        DirectionMode.UNKNOWN
    }
    if (directionMode == DirectionMode.CURRENT_VEHICLE && locationMode != LocationMode.CURRENT_ROAD) {
        return invalidInput()
    }
    if (strings.containsKey("placeQuery") && locationMode != LocationMode.NAMED_PLACE) return invalidInput()
    if (strings.containsKey("directionDescription") && directionMode != DirectionMode.USER_DESCRIPTION) {
        return invalidInput()
    }
    val recordedAt = strings["utteranceRecordedAt"]?.let {
        val parsed = try {
            Instant.parse(it)
        } catch (_: java.time.format.DateTimeParseException) {
            return invalidInput()
        }
        if (parsed.isAfter(now)) return invalidInput()
        parsed
    }
    val missingField = when {
        locationMode == LocationMode.NAMED_PLACE && !strings.containsKey("placeQuery") -> "placeQuery"
        directionMode == DirectionMode.USER_DESCRIPTION && !strings.containsKey("directionDescription") -> "directionDescription"
        else -> null
    }
    return ParsedGleoAction.Valid(
        RoadNoteInput(
            content = content,
            locationMode = locationMode,
            placeQuery = strings["placeQuery"],
            directionMode = directionMode,
            directionDescription = strings["directionDescription"],
            utteranceRecordedAt = recordedAt,
            draftId = strings["draftId"],
        ),
        missingField,
    )
}

private fun invalidInput() = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.FAIL, "The request format is invalid.", reason = "INVALID_INPUT"),
)

private fun missingInput(field: String) = ParsedGleoAction.Rejected(
    GleoResponse(GleoStatus.ASKING, "The report $field is required.", missingField = field),
)
