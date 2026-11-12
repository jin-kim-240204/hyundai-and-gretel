package com.jinkim.hyundaiAndGretel.notes

sealed interface CreateNoteResult {
    data class Created(val note: RoadNote) : CreateNoteResult
    data class Invalid(val reason: InvalidNote) : CreateNoteResult
    data object Duplicate : CreateNoteResult
    data object StorageFailure : CreateNoteResult
}

interface RoadNoteStore {
    suspend fun create(note: RoadNote): CreateNoteResult

    suspend fun findById(noteId: String): RoadNote?
}
