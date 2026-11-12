package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import kotlinx.coroutines.CancellationException
import java.time.Instant
import javax.inject.Inject

class GleoActionProcessor @Inject constructor(private val store: DraftStore) {
    /**
     * 검증된 제보만 동의 확인 후 로컬 초안으로 저장하고 성공/실패를 반환한다.
     * 취소는 저장 실패로 감추지 않고 전파하며, 성공해도 게시·공유된 상태는 아니다.
     */
    suspend fun process(action: ParsedGleoAction, receivedAt: Instant): GleoResponse {
        val input = when (action) {
            is ParsedGleoAction.Rejected -> return action.response
            is ParsedGleoAction.Valid -> action.input
        }
        return try {
            if (!store.hasConsent()) {
                return GleoResponse(
                    GleoStatus.FAIL,
                    "Enable local report storage in the app before creating a draft.",
                    reason = "CONSENT_REQUIRED",
                )
            }
            store.save(input, receivedAt)
            GleoResponse(
                GleoStatus.SUCCESS,
                "The road report was saved locally successfully.",
                report = input,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoResponse(GleoStatus.FAIL, "The report draft could not be saved.", reason = "SAVE_FAILED")
        }
    }
}

/** debug에서 저장 없이 검증 결과를 반환한다. 성공에는 PROBE_ONLY를 붙인다. */
fun previewGleoAction(action: ParsedGleoAction): GleoResponse = when (action) {
    is ParsedGleoAction.Rejected -> action.response
    is ParsedGleoAction.Valid -> GleoResponse(
        GleoStatus.SUCCESS,
        "The road report was received successfully.",
        reason = "PROBE_ONLY",
        report = action.input,
    )
}
