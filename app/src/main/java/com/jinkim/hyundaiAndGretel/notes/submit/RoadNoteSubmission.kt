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

/** 서비스가 소유한다. 제보 요청, 기준 위치 선택과 상태 변경은 메인 실행 문맥에서 처리한다. */
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

    // 리뷰용: 제보 처리 진입점: 중복 요청을 막고 현재 버퍼에서 기준 위치를 먼저 고정한다.
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
        // 리뷰용: 고정한 좌표·작성자·시나리오·UTC 만료시각으로 저장할 쪽지 원본을 구성한다.
        val note = RoadNote(
            noteId = newId(), authorId = context.actorId, content = content, category = category,
            latitude = anchor.sample.latitude, longitude = anchor.sample.longitude,
            anchorOffsetMillis = anchor.offsetMillis, createdAtUtcMillis = createdAt,
            expiresAtUtcMillis = createdAt + RoadNote.TTL_MILLIS,
            route = SyntheticRoute.project(anchor.sample.latitude, anchor.sample.longitude, context),
            scenarioId = scenarioId(),
        )
        mutableState.value = SubmissionState.Submitting
        // 리뷰용: 서비스 수명의 작업으로 저장을 요청한다. 실제 저장 계약의 결과를 화면용 제보 상태로 변환한다.
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
            // 리뷰용: 성공·오류·취소 모두 서비스에 쓰기 종료를 알린다. 화면 이탈만으로 수락한 저장을 끊지 않는다.
            } finally {
                onFinished()
            }
        }
    }
}
