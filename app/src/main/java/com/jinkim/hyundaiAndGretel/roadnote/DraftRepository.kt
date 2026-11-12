package com.jinkim.hyundaiAndGretel.roadnote

import kotlinx.coroutines.CancellationException
import javax.inject.Inject

data class DraftSnapshot(
    val consentGranted: Boolean,
    val drafts: List<RoadNoteDraft>,
)

class DraftRepository @Inject constructor(private val store: DraftStore) {
    /** 저장 동의와 표시할 초안 목록을 조회한다. 동의가 없으면 내용은 조회하지 않는다. */
    suspend fun load(): Result<DraftSnapshot> = readAfter { }

    /** 동의를 변경한 뒤 화면에 반영할 최신 동의·목록을 다시 조회한다. */
    suspend fun setConsent(granted: Boolean): Result<DraftSnapshot> = readAfter {
        store.setConsent(granted)
    }

    /** 초안을 삭제한 뒤 화면에 반영할 최신 동의·목록을 다시 조회한다. */
    suspend fun delete(id: String): Result<DraftSnapshot> = readAfter {
        store.delete(id)
    }

    /**
     * 저장소 작업 후 최신 데이터를 읽고 오류를 Result로 전달한다.
     * 변경 작업과 후속 조회는 하나의 트랜잭션이 아니며, 코루틴 취소는 Result 실패로 감추지 않는다.
     */
    private suspend fun readAfter(operation: suspend () -> Unit): Result<DraftSnapshot> {
        return try {
            operation()
            val consent = store.hasConsent()
            val drafts = if (consent) store.list() else emptyList()
            Result.success(DraftSnapshot(consent, drafts))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
