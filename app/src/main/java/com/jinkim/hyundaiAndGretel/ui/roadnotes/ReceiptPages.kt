package com.jinkim.hyundaiAndGretel.ui.roadnotes

import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

internal val UiRestrictions.allowsReceiptPaging: Boolean
    get() = connected && !optimized && flags == 0

internal const val RECEIPT_LINES = 3
private val grapheme = Regex("\\X")

internal fun fittingReceiptPrefix(
    remaining: String,
    lineEnds: List<Int>,
    fits: (String) -> Boolean,
): Int? {
    val boundaries = grapheme.findAll(remaining).map { it.range.last + 1 }.toList()
    // An isolated prefix can wrap differently, especially when it ends in a newline.
    val limit = lineEnds.maxOrNull() ?: return null
    return boundaries.asReversed().firstOrNull { end ->
        end <= limit && end < remaining.length && fits(remaining.substring(0, end))
    }
}

internal data class ReceiptPages(
    val input: ReceiptLayoutInput,
    private val starts: List<Int> = listOf(0),
    val end: Int? = null,
    val fits: Boolean? = null,
    val bodySeen: Boolean = false,
) {
    val start: Int get() = starts.last()
    val text: String get() = input.message.substring(start, end ?: input.message.length)
    val hasPrevious: Boolean get() = input.restrictions.allowsReceiptPaging && starts.size > 1
    val isPaged: Boolean get() = start > 0 || end != null
    val hasNextPage: Boolean get() = end != null && end < input.message.length
    val canGoNext: Boolean get() = input.restrictions.allowsReceiptPaging &&
        fits == true && bodySeen && hasNextPage

    fun measured(wholePageFits: Boolean, prefixLength: Int? = null): ReceiptPages {
        if (!wholePageFits && end == null && input.restrictions.allowsReceiptPaging &&
            prefixLength != null && prefixLength in 1 until text.length
        ) {
            return copy(end = start + prefixLength, fits = null)
        }
        return copy(fits = wholePageFits, bodySeen = bodySeen && wholePageFits)
    }

    fun next(): ReceiptPages {
        val nextStart = end ?: return this
        return if (canGoNext) ReceiptPages(input, starts + nextStart) else this
    }

    fun previous(): ReceiptPages =
        if (hasPrevious) ReceiptPages(input, starts.dropLast(1), end = start) else this

    fun canConfirm(current: ReceiptLayoutInput, confirming: Boolean): Boolean =
        input == current && !confirming && fits == true && (end ?: input.message.length) == input.message.length &&
            (!isPaged || (current.restrictions.allowsReceiptPaging && bodySeen))
}
