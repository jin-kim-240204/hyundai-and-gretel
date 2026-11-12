package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput

// 성공 응답에는 검증된 제보를 포함하며, 성공이 실제 게시 완료를 의미하지는 않는다.
enum class GleoStatus { SUCCESS, FAIL }

data class GleoResponse(
    val status: GleoStatus,
    val message: String,
    val reason: String? = null,
    val report: RoadNoteInput? = null,
)
