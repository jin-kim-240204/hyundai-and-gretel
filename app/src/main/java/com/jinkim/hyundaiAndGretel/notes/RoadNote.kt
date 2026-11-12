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
) {
    fun isActiveAt(nowUtcMillis: Long): Boolean = nowUtcMillis < expiresAtUtcMillis

    companion object {
        val TTL_MILLIS = TimeUnit.SECONDS.toMillis(600)
    }
}

enum class InvalidNote { ID, AUTHOR, CONTENT, COORDINATES, OFFSET, TIME, ROUTE, SCENARIO }

/** 화면을 거치지 않는 호출도 포함하여, 저장 직전에 정규화된 입력을 검증한다. */
internal fun RoadNote.validationError(): InvalidNote? = when {
    !noteId.isCanonicalUuid() -> InvalidNote.ID
    authorId.isBlank() -> InvalidNote.AUTHOR
    content.codePointCount(0, content.length) !in 1..MAX_CONTENT_CODE_POINTS -> InvalidNote.CONTENT
    !latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0 -> InvalidNote.COORDINATES
    anchorOffsetMillis !in 0L..MAX_ANCHOR_OFFSET_MILLIS -> InvalidNote.OFFSET
    createdAtUtcMillis > Long.MAX_VALUE - RoadNote.TTL_MILLIS ||
        expiresAtUtcMillis != createdAtUtcMillis + RoadNote.TTL_MILLIS -> InvalidNote.TIME
    route != null && !SyntheticRoute.matches(route, latitude, longitude) -> InvalidNote.ROUTE
    scenarioId != null && !scenarioId.isCanonicalUuid() -> InvalidNote.SCENARIO
    else -> null
}

private fun String.isCanonicalUuid(): Boolean = try {
    UUID.fromString(this).toString() == this
} catch (_: IllegalArgumentException) {
    false
}

private const val MAX_CONTENT_CODE_POINTS = 500
private val MAX_ANCHOR_OFFSET_MILLIS = TimeUnit.SECONDS.toMillis(35)
