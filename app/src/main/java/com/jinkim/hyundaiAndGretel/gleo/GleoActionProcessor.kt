package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import kotlinx.coroutines.CancellationException
import java.time.Instant

// Only identifiers and processing state cross the Callback boundary.
enum class GleoStatus { SUCCESS, ASKING, FAIL }

data class GleoResponse(
    val status: GleoStatus,
    val message: String,
    val draftId: String? = null,
    val draftState: DraftState? = null,
    val missingField: String? = null,
    val reason: String? = null,
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
            // shortcut: Gleo timestamp provenance is unverified; enable only after a measured metadata contract exists.
            val draft = store.save(action.input.copy(utteranceRecordedAt = null), receivedAt)
            val needsLocation = action.input.locationMode == LocationMode.NEEDS_CLARIFICATION ||
                action.missingField == "placeQuery"
            val needsDirection = action.missingField == "directionDescription"
            when {
                action.input.locationMode == LocationMode.DEFERRED -> GleoResponse(
                    GleoStatus.SUCCESS,
                    "The report draft was saved for later placement in the app; it has not been published.",
                    draft.id, draft.state,
                )
                needsLocation || needsDirection -> GleoResponse(
                    GleoStatus.ASKING,
                    if (needsLocation) {
                        "The report draft was saved; which place or landmark was it near?"
                    } else {
                        "The report draft was saved; which travel direction was affected?"
                    },
                    draft.id, draft.state,
                    missingField = if (needsLocation) "placeQuery" else "directionDescription",
                )
                else -> GleoResponse(
                    GleoStatus.SUCCESS,
                    "The report draft was saved; location processing is pending and it has not been published.",
                    draft.id, draft.state,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            GleoResponse(GleoStatus.FAIL, "The existing draft could not be updated.", reason = "INVALID_DRAFT")
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The report draft could not be saved.", reason = "SAVE_FAILED")
        }
    }
}
