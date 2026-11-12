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
    // 이전 저장 파일을 읽고 보존하기 위한 값이며, 현재 Gleo 입력에서 생성하지 않는다.
    val observedAt: Instant? = null,
    val draftId: String? = null,
    val category: String? = null,
)

data class RoadNoteDraft(
    val id: String,
    val input: RoadNoteInput,
    val receivedAt: Instant,
    val updatedAt: Instant,
    val state: DraftState,
)
