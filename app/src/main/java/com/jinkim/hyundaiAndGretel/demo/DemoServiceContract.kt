package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.LocationFix

/** Existing service owns SDK/accepted writes; debug orchestration can be exercised with a Fake. */
interface DemoServiceContract {
    val busy: Boolean
    fun startTracking()
    fun stopTracking()
    fun changeActor(actor: String)
    fun currentFix(): LocationFix?
    fun holdStorage()
    fun writesFinished()
}
