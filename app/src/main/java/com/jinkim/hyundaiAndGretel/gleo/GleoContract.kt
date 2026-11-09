package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.notes.Category

const val GLEO_ACTION = "com.jinkim.hyundaiAndGretel.CREATE_ROAD_NOTE"
data class ReportInput(val content: String, val category: Category)
fun validateReport(values: Map<String, Any?>): ReportInput? {
    val content = values["content"] as? String ?: return null
    val location = values["locationMode"] as? String ?: return null
    val category = values["category"] as? String ?: return null
    if (content.length > 500 || content.isBlank() || location != "CURRENT_ROAD") return null
    return Category.entries.firstOrNull { it.name == category }?.let { ReportInput(content.trim(), it) }
}
