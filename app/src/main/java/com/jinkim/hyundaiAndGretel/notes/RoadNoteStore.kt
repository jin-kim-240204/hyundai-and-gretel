package com.jinkim.hyundaiAndGretel.notes

sealed interface CreateNoteResult {
    data class Created(val note: RoadNote) : CreateNoteResult
    data class Invalid(val reason: InvalidNote) : CreateNoteResult
    data object Duplicate : CreateNoteResult
    data object StorageFailure : CreateNoteResult
}

/** Creation never returns an existing note as success. Cancellation propagates. */
interface RoadNoteStore {
    suspend fun create(note: RoadNote): CreateNoteResult
    /** Includes expired originals; storage failures propagate to the caller. */
    suspend fun findById(noteId: String): RoadNote?
}
