package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.jinkim.hyundaiAndGretel.notes.CreateNoteResult
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.RoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.validationError

class RoomRoadNoteStore internal constructor(private val database: NoteDatabase) : RoadNoteStore {
    constructor(context: Context) : this(NoteDatabase.getInstance(context))

    override suspend fun create(note: RoadNote): CreateNoteResult {
        val normalized = note.copy(content = note.content.trim())
        normalized.validationError()?.let { return CreateNoteResult.Invalid(it) }
        return try {
            database.notes().insert(normalized.toEntity())
            CreateNoteResult.Created(normalized)
        } catch (_: SQLiteConstraintException) {
            classifyConstraint(normalized.noteId)
        } catch (_: SQLiteException) {
            CreateNoteResult.StorageFailure
        }
    }

    // After a constraint error, an existing ID means Duplicate; revisit if adding other constraints.
    private suspend fun classifyConstraint(noteId: String): CreateNoteResult = try {
        if (database.notes().findById(noteId) != null) CreateNoteResult.Duplicate
        else CreateNoteResult.StorageFailure
    } catch (_: SQLiteException) {
        CreateNoteResult.StorageFailure
    }

    override suspend fun findById(noteId: String): RoadNote? = database.notes().findById(noteId)?.toNote()
}
