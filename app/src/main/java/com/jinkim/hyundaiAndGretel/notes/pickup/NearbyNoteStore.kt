package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

/** 주변 원본 관찰 계약. 구현체는 RoomRoadNoteStore이며 후보 판정은 NearbyNoteCandidates가 담당한다. */
interface NearbyNoteStore {
    /** 소속 없는 기존 데이터를 포함해 공간 범위를 조회한다. 시간 유효성은 결과 반영·사용 시점에 확인한다. */
    fun observeNearby(cells: Set<String>): Flow<List<RoadNote>>
}
