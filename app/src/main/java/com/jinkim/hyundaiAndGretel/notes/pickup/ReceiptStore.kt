package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.spatial.distanceMeters
import kotlinx.coroutines.flow.Flow

/** 현재 등록과 위치를 수락하는 시점에 메인 실행 문맥에서 입력을 고정한다. */
data class ReceiptRequest(val noteId: String, val recipient: SyntheticSession, val position: LocationFix, val scenarioId: String? = null)
data class InboxItem(
    val note: RoadNote,
    val recipientId: String,
    val receivedAtUtcMillis: Long,
    val distanceMeters: Double,
    val displayedAtUtcMillis: Long?,
)
enum class ReceiveResult { RECEIVED, ALREADY_RECEIVED, INELIGIBLE, STORAGE_FAILURE }

/** 사용자별 수신·확인 저장 계약. 구체 트랜잭션과 JOIN은 RoomReceiptStore가 담당한다. */
interface ReceiptStore {
    fun observeInbox(recipientId: String): Flow<List<InboxItem>>
    suspend fun receive(request: ReceiptRequest): ReceiveResult
    suspend fun confirm(noteId: String, recipientId: String)
}

/** 공간 조건만 판단한다. 세션과 위치 수신 시각의 최신성은 메인 실행 문맥의 수락 시점에 확인한다. */
fun eligibleDistance(note: RoadNote, setup: SyntheticSession, position: LocationFix, nowUtc: Long): Double? {
    if (note.authorId == setup.actorId || !note.isActiveAt(nowUtc)) return null
    val route = note.route ?: return null
    if (!SyntheticRoute.matches(route, note.latitude, note.longitude)) return null
    val current = SyntheticRoute.project(position.latitude, position.longitude, setup) ?: return null
    val ahead = SyntheticRoute.ahead(route, current) ?: return null
    if (ahead !in 0.0..150.0) return null
    return distanceMeters(position.latitude, position.longitude, note.latitude, note.longitude).takeIf { it <= 150.0 }
}
