package com.jinkim.hyundaiAndGretel.driving

import com.jinkim.hyundaiAndGretel.gleo.GleoProbeEvent
import com.jinkim.hyundaiAndGretel.gleo.GleoStatus

// Zero is the platform's unrestricted baseline; unknown or partial restrictions use the minimal UI.
internal fun useCompactDrivingUi(requiresOptimization: Boolean?, activeRestrictions: Int?): Boolean =
    requiresOptimization != false || activeRestrictions == null || activeRestrictions != 0

internal fun drivingStatus(event: GleoProbeEvent?): String = when {
    event == null -> "Gleo에게 목격한 내용을 말해 주세요."
    event.elapsedMs == null -> "제보 내용을 확인하고 있습니다."
    event.status == GleoStatus.SUCCESS -> "제보 내용을 확인했습니다."
    event.status == GleoStatus.ASKING -> "Gleo의 질문에 답해 주세요."
    else -> "제보를 처리하지 못했습니다."
}
