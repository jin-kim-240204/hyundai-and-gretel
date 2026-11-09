package com.jinkim.hyundaiAndGretel.demo

import android.content.Context

class DebugGps(context: Context) {
    fun setPosition(progress: Double): Unit = error("Debug GPS is unavailable")
    suspend fun speedKph(): Double = error("Debug GPS is unavailable")
    fun stop() = Unit
}
