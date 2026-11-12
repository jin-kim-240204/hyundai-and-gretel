package com.jinkim.hyundaiAndGretel.notes

sealed interface CreateNoteResult {
    data class Created(val note: RoadNote) : CreateNoteResult
    data class Invalid(val reason: InvalidNote) : CreateNoteResult
    data object Duplicate : CreateNoteResult
    data object StorageFailure : CreateNoteResult
}

/** 이미 존재하는 쪽지는 신규 저장 성공으로 반환하지 않는다. 코루틴 취소는 호출자에게 전파한다. */
/** 쪽지 원본 저장 계약. 구체 저장 구현은 RoomRoadNoteStore. */
// 리뷰용: 저장 계약 인터페이스다. 제보 처리자는 Room을 직접 모르며, 실제 저장은 RoomRoadNoteStore가 수행한다.
interface RoadNoteStore {
    suspend fun create(note: RoadNote): CreateNoteResult
    /** 만료된 원본도 조회에 포함한다. 저장소 오류는 호출자에게 전파한다. */
    suspend fun findById(noteId: String): RoadNote?
}
