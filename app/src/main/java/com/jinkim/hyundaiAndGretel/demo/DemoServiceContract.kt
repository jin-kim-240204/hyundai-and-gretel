package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.LocationFix

/** SDK와 수락한 쓰기는 기존 서비스가 관리한다. 시연 흐름은 가짜 구현으로도 검증할 수 있다. */
interface DemoServiceContract {
    val busy: Boolean
    fun startTracking()
    fun stopTracking()
    fun changeActor(actor: String)
    fun currentFix(): LocationFix?
    fun holdStorage()
    fun writesFinished()
}
