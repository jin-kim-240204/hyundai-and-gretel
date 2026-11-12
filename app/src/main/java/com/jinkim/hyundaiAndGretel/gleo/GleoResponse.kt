package com.jinkim.hyundaiAndGretel.gleo

enum class GleoStatus { SUCCESS, FAIL }

data class GleoResponse(
    val status: GleoStatus,
    val message: String,
    val reason: String? = null,
    val report: GleoRoadNoteInput? = null,
    val noteId: String? = null,
)
