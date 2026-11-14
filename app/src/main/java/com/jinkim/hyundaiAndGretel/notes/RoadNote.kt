package com.jinkim.hyundaiAndGretel.notes

import com.jinkim.hyundaiAndGretel.notes.pickup.RouteContext
import com.jinkim.hyundaiAndGretel.notes.pickup.SyntheticRoute
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class NoteCategory { WARNING, INFORMATION }

data class RoadNote(
    val noteId: String,
    val authorId: String,
    val content: String,
    val category: NoteCategory,
    val latitude: Double,
    val longitude: Double,
    val anchorOffsetMillis: Long,
    val createdAtUtcMillis: Long,
    val expiresAtUtcMillis: Long,
    val route: RouteContext? = null,
    val scenarioId: String? = null,
    val receiverGuidance: String? = null,
) {
    val receiverMessage: String get() = receiverGuidance ?: content

    fun isActiveAt(nowUtcMillis: Long): Boolean = nowUtcMillis < expiresAtUtcMillis

    companion object {
        val TTL_MILLIS = TimeUnit.SECONDS.toMillis(600)
    }
}

enum class InvalidNote { ID, AUTHOR, CONTENT, RECEIVER_GUIDANCE, COORDINATES, OFFSET, TIME, ROUTE, SCENARIO }

internal fun RoadNote.validationError(): InvalidNote? = when {
    !noteId.isCanonicalUuid() -> InvalidNote.ID
    authorId.isBlank() -> InvalidNote.AUTHOR
    content.codePointCount(0, content.length) !in 1..MAX_CONTENT_CODE_POINTS -> InvalidNote.CONTENT
    receiverGuidance != null && !receiverGuidance.isValidReceiverGuidance() -> InvalidNote.RECEIVER_GUIDANCE
    !latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0 -> InvalidNote.COORDINATES
    anchorOffsetMillis !in 0L..MAX_ANCHOR_OFFSET_MILLIS -> InvalidNote.OFFSET
    createdAtUtcMillis > Long.MAX_VALUE - RoadNote.TTL_MILLIS ||
        expiresAtUtcMillis != createdAtUtcMillis + RoadNote.TTL_MILLIS -> InvalidNote.TIME
    scenarioId != null && !scenarioId.isCanonicalUuid() -> InvalidNote.SCENARIO
    else -> null
}

internal fun RoadNote.hasValidRouteMetadata(): Boolean =
    route == null || SyntheticRoute.matches(route, latitude, longitude)

private fun String.isCanonicalUuid(): Boolean = try {
    UUID.fromString(this).toString() == this
} catch (_: IllegalArgumentException) {
    false
}

internal fun String.isValidReceiverGuidance(): Boolean =
    isNotBlank() && codePointCount(0, length) <= MAX_CONTENT_CODE_POINTS

private const val MAX_CONTENT_CODE_POINTS = 500
private val MAX_ANCHOR_OFFSET_MILLIS = TimeUnit.SECONDS.toMillis(35)
