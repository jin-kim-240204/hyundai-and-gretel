package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

/** 주변 원본 관찰 계약. 구현체는 RoomRoadNoteStore이며 후보 판정은 NearbyNoteCandidates가 담당한다. */
interface NearbyNoteStore {
    /** Spatial rows, including legacy data; time validity is checked on application and consumption. */
    fun observeNearby(cells: Set<String>): Flow<List<RoadNote>>
}
