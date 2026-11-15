package com.jinkim.hyundaiAndGretel.speech

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Test

/** Pleos Speech Service가 설치된 환경에서만 실행하는 실제 SDK 검사다. */
class PleosNoteSpeakerTest {
    @Test fun audioPowerPolicyConnectsAndCloseDisablesNewSpeech() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val power = CarAudioPower(scope) {}
        try {
            withContext(Dispatchers.Main) {
                power.start(InstrumentationRegistry.getInstrumentation().targetContext)
                withTimeout(5_000) { while (!power.enabled) delay(20) }
                power.close()
                assertFalse(power.enabled)
            }
        } finally {
            withContext(Dispatchers.Main) { power.close() }
            scope.cancel()
        }
    }

    @Test fun onDeviceCompletionAndCancellationReleaseTheNextRequest() = runBlocking {
        val speaker = PleosNoteSpeaker(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            val interrupted = launch { speaker.speak("도로 안내 시험입니다.") }
            delay(100)
            interrupted.cancel()
            interrupted.join()
            speaker.speak("도로 안내입니다. 앞에 공사 구간이 있습니다.")
        } finally {
            withContext(Dispatchers.Main) { speaker.stop() }
        }
    }
}
