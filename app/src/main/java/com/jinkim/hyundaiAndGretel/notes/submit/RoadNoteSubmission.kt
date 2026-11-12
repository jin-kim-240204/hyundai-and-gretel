package com.jinkim.hyundaiAndGretel.notes.submit

import com.jinkim.hyundaiAndGretel.location.AnchorSelection
import com.jinkim.hyundaiAndGretel.notes.CreateNoteResult
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.RoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.pickup.SyntheticRoute
import com.jinkim.hyundaiAndGretel.notes.pickup.SyntheticSession
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data class Saved(val note: RoadNote) : SubmissionState
    data class Failed(val reason: SubmissionFailure) : SubmissionState
}

enum class SubmissionFailure { LOCATION_UNAVAILABLE, INVALID_TIME, INVALID_INPUT, DUPLICATE, STORAGE, CANCELLED }

class RoadNoteSubmission(
    private val scope: CoroutineScope,
    private val store: RoadNoteStore,
    private val captureAnchor: () -> AnchorSelection,
    private val utcMillis: () -> Long,
    private val onFinished: () -> Unit,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onUnexpectedError: (Exception) -> Unit = {},
    private val setup: () -> SyntheticSession = { SyntheticSession() },
    private val scenarioId: () -> String? = { null },
) {
    private val mutableState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val state = mutableState.asStateFlow()
    val isSubmitting get() = state.value == SubmissionState.Submitting

    fun clearResult() { if (!isSubmitting) mutableState.value = SubmissionState.Idle }

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
        val context = setup()

        val note = RoadNote(
            noteId = newId(), authorId = context.actorId, content = content, category = category,
            latitude = anchor.sample.latitude, longitude = anchor.sample.longitude,
            anchorOffsetMillis = anchor.offsetMillis, createdAtUtcMillis = createdAt,
            expiresAtUtcMillis = createdAt + RoadNote.TTL_MILLIS,
            route = SyntheticRoute.project(anchor.sample.latitude, anchor.sample.longitude, context),
            scenarioId = scenarioId(),
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
            } catch (error: Exception) {
                mutableState.value = SubmissionState.Failed(SubmissionFailure.STORAGE)
                onUnexpectedError(error)

            } finally {
                onFinished()
            }
        }
    }
}
