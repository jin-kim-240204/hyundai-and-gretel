package com.jinkim.hyundaiAndGretel.ui.roadnotes

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.RoadNoteServiceContract
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** 메인 실행 문맥에서 서비스 연결을 전환한다. JVM 테스트에서도 같은 연결 처리를 사용한다. */
class RoadNoteServiceConnection(scope: CoroutineScope) {
    private val current = MutableStateFlow<RoadNoteServiceContract?>(null)

    // 리뷰용: 현재 연결의 상태 흐름만 구독한다. 연결이 바뀌면 이전 서비스 관찰을 취소하고 새 상태로 화면을 구성한다.
    // TODO(코루틴 업데이트 검토): flatMapLatest의 실험 상태·이전 구독 취소·buffer(0) 동작을 재확인한다.
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<RoadNotesUiState> = current.flatMapLatest { session ->
        if (session == null) flowOf(RoadNotesUiState())
        else combine(session.status, session.submissionState, session.inboxState, session.pickupState) { status, submission, inbox, pickup ->
            RoadNotesUiState(status, submission, inbox, pickup)
        }.combine(session.uiRestrictions) { state, policy -> state.copy(restrictions = policy) }
    }.buffer(0).stateIn(scope, SharingStarted.Eagerly, RoadNotesUiState())

    val canStartTracking get() = current.value?.canStartTracking ?: true

    fun connect(session: RoadNoteServiceContract?) {
        current.value = session
    }

    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>) {
        current.value?.confirmReceipt(key, screenResumed, visitedContent)
    }

    // 리뷰용: 명령은 화면에 마지막으로 보인 복사본이 아니라 현재 연결된 서비스의 최신 상태를 확인해 전달한다.
    fun submitRoadNote(content: String, category: NoteCategory) {
        val session = current.value ?: return
        val latest = RoadNotesUiState(session.status.value, session.submissionState.value)
        if (latest.canSubmit) session.submitRoadNote(content, category)
    }
}
