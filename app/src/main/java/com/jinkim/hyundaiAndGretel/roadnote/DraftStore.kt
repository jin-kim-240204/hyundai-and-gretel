package com.jinkim.hyundaiAndGretel.roadnote

import java.time.Instant

interface DraftStore {
    /** 기기 내 초안 저장에 동의했는지 조회한다. */
    suspend fun hasConsent(): Boolean

    /** 저장 동의를 설정한다. 철회하면 저장된 초안도 모두 삭제한다. */
    suspend fun setConsent(granted: Boolean)

    /** 동의·유효 기간·용량을 확인해 초안을 생성하거나 기존 ID의 초안을 갱신한다. */
    suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft

    /** 만료된 초안을 정리한 뒤 최초 수신 시각이 최신인 순서로 반환한다. */
    suspend fun list(): List<RoadNoteDraft>

    /** 해당 ID의 초안을 삭제하고, 실제 삭제한 항목이 있는지 반환한다. */
    suspend fun delete(id: String): Boolean

    /** 저장 동의는 유지하면서 초안만 모두 삭제한다. */
    suspend fun clear()
}
