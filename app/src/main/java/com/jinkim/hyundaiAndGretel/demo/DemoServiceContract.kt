package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.LocationFix

interface DemoServiceContract {
    val busy: Boolean
    fun startTracking()
    fun stopTracking()
    fun changeActor(actor: String)
    fun currentFix(): LocationFix?
    fun holdStorage()
    fun writesFinished()
}
