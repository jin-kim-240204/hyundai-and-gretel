package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.drivingstate.CarUxRestrictions as Flags
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptKey
import com.jinkim.hyundaiAndGretel.notes.pickup.ReceiptRepository

data class UiRestrictions(
    val connected: Boolean = false,
    val optimized: Boolean = false,
    val flags: Int = 0,
    val stringLimit: Int = Int.MAX_VALUE,
    val itemLimit: Int = Int.MAX_VALUE,
    val depthLimit: Int = Int.MAX_VALUE,
) {
    private fun has(flag: Int) = optimized && flags and flag != 0
    val canSetup get() = connected && !has(Flags.UX_RESTRICTIONS_NO_SETUP)
    val canShowBody get() = !has(Flags.UX_RESTRICTIONS_NO_TEXT_MESSAGE)
    val canShowPlot get() = !has(Flags.UX_RESTRICTIONS_NO_VIDEO)
    val maxString get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_STRING_LENGTH)) stringLimit.coerceAtLeast(0) else Int.MAX_VALUE
    val maxItems get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_CONTENT)) itemLimit.coerceAtLeast(0) else Int.MAX_VALUE
    val maxDepth get() = if (has(Flags.UX_RESTRICTIONS_LIMIT_CONTENT)) depthLimit.coerceAtLeast(0) else Int.MAX_VALUE
    fun allowsContent(key: String, seen: Set<String>, depth: Int = 1) = depth <= maxDepth && (key in seen || seen.size < maxItems)
    fun fits(text: String) = text.codePointCount(0, text.length) <= maxString
    fun label(text: String): String = text.substring(0, text.offsetByCodePoints(0, minOf(text.codePointCount(0, text.length), maxString)))
}

/** Main admission uses the service's raw policy, not the asynchronously derived screen state. */
fun ReceiptRepository.confirmVisibleReceipt(
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
