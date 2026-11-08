package com.jinkim.hyundaiAndGretel.roadnote

import java.time.Instant

enum class LocationMode { CURRENT_ROAD, NAMED_PLACE, NEEDS_CLARIFICATION, DEFERRED }
enum class DirectionMode { CURRENT_VEHICLE, USER_DESCRIPTION, UNKNOWN }
enum class DraftState { PENDING_LOCATION, AWAITING_LOCATION }

data class RoadNoteInput(
    val content: String,
    val locationMode: LocationMode,
    val placeQuery: String? = null,
    val directionMode: DirectionMode = DirectionMode.UNKNOWN,
    val directionDescription: String? = null,
    val utteranceRecordedAt: Instant? = null,
    val draftId: String? = null,
)

data class RoadNoteDraft(
    val id: String,
    val input: RoadNoteInput,
    val receivedAt: Instant,
    val updatedAt: Instant,
    val state: DraftState,
)

interface DraftStore {
    suspend fun hasConsent(): Boolean
    suspend fun setConsent(granted: Boolean)
    suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft
    suspend fun list(): List<RoadNoteDraft>
    suspend fun delete(id: String): Boolean
    suspend fun clear()
}
