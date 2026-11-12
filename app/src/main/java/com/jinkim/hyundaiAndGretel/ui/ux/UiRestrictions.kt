package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.drivingstate.CarUxRestrictions as Flags
import com.jinkim.hyundaiAndGretel.notes.pickup.NoteReceiptProcessor
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey

data class UiRestrictions(
    val connected: Boolean = false,
    val optimized: Boolean = false,
    val flags: Int = 0,
    val stringLimit: Int = Int.MAX_VALUE,
    val itemLimit: Int = Int.MAX_VALUE,
    val depthLimit: Int = Int.MAX_VALUE,
) {
    private fun has(flag: Int) = optimized && flags and flag != 0
    // 리뷰용: 역할·모드·출발점·초기화는 규제 연결 및 설정 허용이 필요하다. 정차 속도만으로 허용 여부를 계산하지 않는다.
    val canSetup get() = connected && !has(Flags.UX_RESTRICTIONS_NO_SETUP)
    val canShowBody get() = !has(Flags.UX_RESTRICTIONS_NO_TEXT_MESSAGE)
    // 리뷰용: 현재 합성 그래프는 영상 제한과 같은 조건으로 숨긴다. 위치 수집 자체의 중지를 의미하지 않는다.
    val canShowPlot get() = !has(Flags.UX_RESTRICTIONS_NO_VIDEO)
    val maxString get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_STRING_LENGTH)) stringLimit.coerceAtLeast(0) else Int.MAX_VALUE
    val maxItems get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_CONTENT)) itemLimit.coerceAtLeast(0) else Int.MAX_VALUE
    val maxDepth get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_CONTENT)) depthLimit.coerceAtLeast(0) else Int.MAX_VALUE
    fun allowsContent(key: String, seen: Set<String>, depth: Int = 1) = depth <= maxDepth && (key in seen || seen.size < maxItems)
    fun fits(text: String) = text.codePointCount(0, text.length) <= maxString
    fun label(text: String): String = text.substring(0, text.offsetByCodePoints(0, minOf(text.codePointCount(0, text.length), maxString)))
}

/** 확인 요청 수락 시에는 비동기로 계산된 화면 상태 대신 서비스의 최신 규제를 직접 확인한다. */
fun NoteReceiptProcessor.confirmVisibleReceipt(
    key: ReceiptKey,
    screenResumed: Boolean,
    policy: UiRestrictions,
    visitedContent: Set<String>,
) {
    val pending = inbox.value.pending ?: return
    val id = "receipt:${key.recipientId}/${key.noteId}"
    if (policy.canShowBody && policy.fits(pending.note.content) && policy.allowsContent(id, visitedContent)) {
        confirm(key, screenResumed)
    }
}
