package com.jinkim.hyundaiAndGretel.notes

import com.jinkim.hyundaiAndGretel.location.AnchorSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data class Saved(val note: RoadNote) : SubmissionState
    data class Failed(val reason: SubmissionFailure) : SubmissionState
}

enum class SubmissionFailure { LOCATION_UNAVAILABLE, INVALID_TIME, INVALID_INPUT, DUPLICATE, STORAGE, CANCELLED }

/** Owned by the service; commands, anchor capture and state changes run on Main. */
class RoadNoteRepository(
    private val scope: CoroutineScope,
    private val store: RoadNoteStore,
    private val captureAnchor: () -> AnchorSelection,
    private val utcMillis: () -> Long,
    private val onFinished: () -> Unit,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val mutableState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val state = mutableState.asStateFlow()
    val isSubmitting get() = state.value == SubmissionState.Submitting

    fun submit(content: String, category: NoteCategory) {
        if (isSubmitting) return
        val selection = captureAnchor() as? AnchorSelection.Selected
        if (selection == null) {
            mutableState.value = SubmissionState.Failed(SubmissionFailure.LOCATION_UNAVAILABLE)
            return
        }
        val createdAt = utcMillis()
        if (createdAt > Long.MAX_VALUE - RoadNote.TTL_MILLIS) {
            mutableState.value = SubmissionState.Failed(SubmissionFailure.INVALID_TIME)
            return
        }
        val anchor = selection.anchor
        val note = RoadNote(
            noteId = newId(), authorId = "A", content = content, category = category,
            latitude = anchor.sample.latitude, longitude = anchor.sample.longitude,
            anchorOffsetMillis = anchor.offsetMillis, createdAtUtcMillis = createdAt,
            expiresAtUtcMillis = createdAt + RoadNote.TTL_MILLIS,
        )
        mutableState.value = SubmissionState.Submitting
        scope.launch {
            try {
                mutableState.value = when (val result = store.create(note)) {
                    is CreateNoteResult.Created -> SubmissionState.Saved(result.note)
                    is CreateNoteResult.Invalid -> SubmissionState.Failed(SubmissionFailure.INVALID_INPUT)
                    CreateNoteResult.Duplicate -> SubmissionState.Failed(SubmissionFailure.DUPLICATE)
                    CreateNoteResult.StorageFailure -> SubmissionState.Failed(SubmissionFailure.STORAGE)
                }
            } catch (cancelled: CancellationException) {
                mutableState.value = SubmissionState.Failed(SubmissionFailure.CANCELLED)
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = SubmissionState.Failed(SubmissionFailure.STORAGE)
            } finally {
                onFinished()
            }
        }
    }
}
