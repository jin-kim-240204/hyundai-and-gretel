package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import kotlinx.coroutines.CancellationException

class GleoActionProcessor(
    private val submitReport: suspend (String, NoteCategory, Long) -> SubmissionState,
) {
    suspend fun process(action: ParsedGleoAction, receivedElapsedMillis: Long): GleoResponse {
        val input = when (action) {
            is ParsedGleoAction.Rejected -> return action.response
            is ParsedGleoAction.Valid -> action.input
        }
        return try {
            when (val result = submitReport(input.content, input.category, receivedElapsedMillis)) {
                is SubmissionState.Saved -> GleoResponse(
                    GleoStatus.SUCCESS, "The road report was saved successfully.",
                    report = input, noteId = result.note.noteId,
                )
                is SubmissionState.Failed -> failure(result.reason)
                else -> GleoResponse(GleoStatus.FAIL, "The save result is not available yet.", reason = "SAVE_PENDING")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The road report could not be saved.", reason = "SAVE_FAILED")
        }
    }

    private fun failure(reason: SubmissionFailure) = GleoResponse(
        GleoStatus.FAIL,
        when (reason) {
            SubmissionFailure.DEMO_REQUIRED -> "Road reports can only be saved during a ready author demo session."
            SubmissionFailure.LOCATION_UNAVAILABLE -> "Start location collection in the app and wait for a valid location before reporting."
            SubmissionFailure.BUSY -> "Another road report is being saved. Please try again after it finishes."
            else -> "The road report could not be saved."
        },
        reason = reason.name,
    )
}
