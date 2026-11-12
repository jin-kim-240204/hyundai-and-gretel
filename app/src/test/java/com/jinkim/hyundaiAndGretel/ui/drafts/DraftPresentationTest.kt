package com.jinkim.hyundaiAndGretel.ui.drafts

import com.jinkim.hyundaiAndGretel.roadnote.DirectionMode
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftPresentationTest {
    @Test
    fun currentRoadAndVehicleDoNotClaimMeasuredLocationOrDirection() {
        val draft = draft(LocationMode.CURRENT_ROAD, DirectionMode.CURRENT_VEHICLE)
        assertTrue(draft.locationDescription().contains("위치 미확정"))
        assertTrue(draft.directionDescription().contains("방향 미확정"))
        assertTrue(draft.locationStatus().contains("미게시"))
    }

    @Test
    fun deferredAndClarificationKeepLocationUnresolved() {
        val deferred = draft(LocationMode.DEFERRED).copy(state = DraftState.AWAITING_LOCATION)
        assertEquals("미게시 · 위치 미정", deferred.locationStatus())
        assertEquals("위치 표현: 나중에 지정", deferred.locationDescription())
        assertEquals("위치 표현: 확인 필요", draft(LocationMode.NEEDS_CLARIFICATION).locationDescription())
        assertEquals("방향: 미정", deferred.directionDescription())
    }

    @Test
    fun namedPlaceAndUserDirectionRemainUserExpressions() {
        val draft = draft(LocationMode.NAMED_PLACE, DirectionMode.USER_DESCRIPTION)
        val described = draft.copy(input = draft.input.copy(placeQuery = "서울역", directionDescription = "시청 방면"))
        assertEquals("장소 표현: 서울역", described.locationDescription())
        assertEquals("방향 표현: 시청 방면", described.directionDescription())
        assertEquals("장소 표현: 미정", draft.locationDescription())
        assertEquals("방향 표현: 미정", draft.directionDescription())
        val blank = draft.copy(input = draft.input.copy(placeQuery = "  ", directionDescription = "\n"))
        assertEquals(draft.locationDescription(), blank.locationDescription())
        assertEquals(draft.directionDescription(), blank.directionDescription())
        assertFalse(described.locationDescription().contains("좌표"))
    }

    private fun draft(location: LocationMode, direction: DirectionMode = DirectionMode.UNKNOWN) = RoadNoteDraft(
        "draft-id",
        RoadNoteInput("도로 장애물", location, directionMode = direction),
        Instant.EPOCH,
        Instant.EPOCH,
        DraftState.PENDING_LOCATION,
    )
}
