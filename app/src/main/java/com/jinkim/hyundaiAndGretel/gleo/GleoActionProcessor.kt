package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import kotlinx.coroutines.CancellationException
import java.time.Instant

// Successful callbacks include the validated report.
enum class GleoStatus { SUCCESS, FAIL }

data class GleoResponse(
    val status: GleoStatus,
    val message: String,
    val reason: String? = null,
    val report: RoadNoteInput? = null,
)

class GleoActionProcessor(private val store: DraftStore) {
    suspend fun process(action: ParsedGleoAction, receivedAt: Instant): GleoResponse {
        if (action is ParsedGleoAction.Rejected) return action.response
        action as ParsedGleoAction.Valid
        return try {
            if (!store.hasConsent()) {
                return GleoResponse(
                    GleoStatus.FAIL,
                    "Enable local report storage in the app before creating a draft.",
                    reason = "CONSENT_REQUIRED",
                )
            }
            store.save(action.input, receivedAt)
            GleoResponse(
                GleoStatus.SUCCESS,
                "The road report was saved locally successfully.",
                report = action.input,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The report draft could not be saved.", reason = "SAVE_FAILED")
        }
    }
}
