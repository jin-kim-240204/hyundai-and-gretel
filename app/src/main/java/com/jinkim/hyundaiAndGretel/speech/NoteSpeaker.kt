package com.jinkim.hyundaiAndGretel.speech

/** 출력 직전 canStart가 false면 생략한다. 재생한 경우 정상 반환은 완료, 중단·오류는 예외다. */
interface NoteSpeaker {
    suspend fun speak(text: String, canStart: () -> Boolean = { true })
    fun stop()
}
