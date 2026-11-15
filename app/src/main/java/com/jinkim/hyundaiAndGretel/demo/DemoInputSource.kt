package com.jinkim.hyundaiAndGretel.demo

interface DemoInputSource {
    suspend fun prepare()
    fun inject(progress: Double)
    suspend fun readMotion(): Pair<Double, String>
    fun close()
}
