package com.jinkim.hyundaiAndGretel.notes.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote

@Entity(tableName = "road_notes")
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
    val isSynthetic: Boolean,
) {
    fun toNote() = RoadNote(
        noteId = noteId, authorId = authorId, content = content,
        category = NoteCategory.valueOf(category), latitude = latitude, longitude = longitude,
        anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
        expiresAtUtcMillis = expiresAtUtcMillis, isSynthetic = isSynthetic,
    )
}

internal fun RoadNote.toEntity() = RoadNoteEntity(
    noteId = noteId, authorId = authorId, content = content,
    category = category.name, latitude = latitude, longitude = longitude,
    anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
    expiresAtUtcMillis = expiresAtUtcMillis, isSynthetic = isSynthetic,
)
