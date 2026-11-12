package com.jinkim.hyundaiAndGretel.service

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import kotlinx.coroutines.flow.StateFlow

/** 화면에 필요한 현재 서비스 상태와 제보·수신 확인 진입점만 공개하는 계약이다. */
// 리뷰용: 인터페이스: 화면은 이 계약을 보고 호출한다. 실제 구현은 RoadNoteService.LocalBinder에 있다.
interface RoadNoteServiceContract {
    val canStartTracking: Boolean get() = true
    val uiRestrictions: StateFlow<com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions>
    val status: StateFlow<TrackingStatus>
    val submissionState: StateFlow<SubmissionState>
    val inboxState: StateFlow<InboxState>
    val pickupState: StateFlow<PickupState>
    fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>)
    fun submitRoadNote(content: String, category: NoteCategory)
}
