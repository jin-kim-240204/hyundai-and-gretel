package com.jinkim.hyundaiAndGretel.demo

interface DemoInput {
    suspend fun prepare()
    fun inject(progress: Double)
    suspend fun readMotion(): Pair<Double, String>
    fun close()
}
