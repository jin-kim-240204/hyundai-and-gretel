package com.jinkim.hyundaiAndGretel.notes.storage

import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote

@Entity(tableName = "road_notes", indices = [Index("h3CellId")])
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
) {
    fun toNote() = RoadNote(
        noteId = noteId, authorId = authorId, content = content,
        category = NoteCategory.valueOf(category), latitude = latitude, longitude = longitude,
        anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
        expiresAtUtcMillis = expiresAtUtcMillis,
    )
}

internal fun RoadNote.toEntity(h3CellId: String) = RoadNoteEntity(
    noteId = noteId, authorId = authorId, content = content,
    category = category.name, latitude = latitude, longitude = longitude,
    anchorOffsetMillis = anchorOffsetMillis, createdAtUtcMillis = createdAtUtcMillis,
    expiresAtUtcMillis = expiresAtUtcMillis, h3CellId = h3CellId,
)
