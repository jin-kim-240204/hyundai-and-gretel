package com.jinkim.hyundaiAndGretel.notes.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    val displayedAtUtcMillis: Long? = null,
)
