package com.jinkim.hyundaiAndGretel.notes.storage

import com.jinkim.hyundaiAndGretel.notes.pickup.RouteContext
import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote

// 리뷰용: 도로에 심어진 쪽지 원본 테이블이다. 수신자별 이력은 이 행에 추가하지 않고 ReceiptEntity에 따로 기록한다.
@Entity(tableName = "road_notes", indices = [Index("h3CellId"), Index(value = ["scenarioId", "h3CellId"])])
internal data class RoadNoteEntity(
    @PrimaryKey val noteId: String,
    val authorId: String,
    val content: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val anchorOffsetMillis: Long,
    val createdAtUtcMillis: Long,
    val expiresAtUtcMillis: Long,
    val h3CellId: String? = null,
    val routeId: String? = null,
    val direction: Int? = null,
    val progressMeters: Double? = null,
    val scenarioId: String? = null,
) {
    // 리뷰용: DB 전용 표현을 기능에서 사용하는 RoadNote로 변환한다. 조회 처리자는 이 변환 후 저장 데이터의 유효성을 검증한다.
    fun toNote() = RoadNote(
        noteId = noteId, authorId = authorId, content = content,
        category = NoteCategory.valueOf(category), latitude = latitude, longitude = longitude,
        anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
        expiresAtUtcMillis = expiresAtUtcMillis,
        route = routeContext(), scenarioId = scenarioId,
    )

    private fun routeContext(): RouteContext? {
        if (routeId == null && direction == null && progressMeters == null) return null
        return RouteContext(requireNotNull(routeId), requireNotNull(direction), requireNotNull(progressMeters))
    }
}

internal fun RoadNote.toEntity(h3CellId: String) = RoadNoteEntity(
    noteId = noteId, authorId = authorId, content = content,
    category = category.name, latitude = latitude, longitude = longitude,
    anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
    expiresAtUtcMillis = expiresAtUtcMillis, h3CellId = h3CellId,
    routeId = route?.routeId, direction = route?.direction, progressMeters = route?.progressMeters, scenarioId = scenarioId,
)
