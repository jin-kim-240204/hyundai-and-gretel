package com.jinkim.hyundaiAndGretel.notes.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// 리뷰용: 쪽지 ID와 수신자 ID의 복합 기본키로 사용자별 중복 수신을 막는다. 원본 삭제 시 연결 이력도 함께 삭제한다.
@Entity(
    tableName = "note_receipts",
    primaryKeys = ["noteId", "recipientId"],
    foreignKeys = [ForeignKey(entity = RoadNoteEntity::class, parentColumns = ["noteId"],
        childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["recipientId", "displayedAtUtcMillis", "receivedAtUtcMillis", "noteId"])],
)
internal data class ReceiptEntity(
    val noteId: String,
    val recipientId: String,
    val receivedAtUtcMillis: Long,
    val distanceMeters: Double,
    // 리뷰용: 현재 계약에서는 화면 노출 시각이 아니라 명시 확인이 저장된 시각이다. null이면 확인 대기다.
    val displayedAtUtcMillis: Long? = null,
)
