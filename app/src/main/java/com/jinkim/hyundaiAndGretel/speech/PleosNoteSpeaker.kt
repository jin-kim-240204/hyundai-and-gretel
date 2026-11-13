package com.jinkim.hyundaiAndGretel.speech

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.selects.select
import java.io.IOException
import java.util.Locale

/** SDK·AudioTrack의 공개 API만 사용한다. 서비스 Main 수명에서 생성/중지한다. */
class PleosNoteSpeaker(context: Context) : NoteSpeaker {
    private val context = context.applicationContext
    private val audio = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var active: Playback? = null

    private class Playback(val tts: TextToSpeech) {
        val initialized = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var silence: AudioTrack? = null
        var focus: AudioFocusRequest? = null
    }

    override suspend fun speak(text: String, canStart: () -> Boolean) = withContext(Dispatchers.Main.immediate) {
        mutex.withLock {
            // 발화 ID가 없는 늦은 콜백이 다음 요청에 섞이지 않도록 SDK 인스턴스를 격리한다.
            val playback = Playback(TextToSpeech(context, Mode.ON_DEVICE))
            active = playback
            val listener = object : EventListener {
                override fun onReady() = Unit
                override fun onStart() = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onDone() { playback.finished.complete(Unit) }
                override fun onStop() { fail(playback, "STOPPED") }
                override fun onError(errorType: ErrorType) { fail(playback, errorType.name) }
            }
            try {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    playback.tts.initialize(object : OnInitializeListener {
                        override fun onSuccess() { playback.initialized.complete(Unit) }
                        override fun onFailure(errorType: ErrorType) { fail(playback, errorType.name) }
                    })
                    playback.initialized.await()
                    if (active !== playback) throw CancellationException("Speech stopped")
                    playback.tts.addEventListener(listener)
                    playback.tts.setAudioAttributes(attributes)
                    playback.tts.setLocale(Locale.KOREA)
                    if (!canStart()) return@withTimeout
                    acquireFocus(playback)
                    playSilence(playback)
                    if (playback.finished.isCompleted) playback.finished.await()
                    if (!canStart()) return@withTimeout
                    playback.tts.speak(text, true)
                    playback.finished.await()
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw IOException("TTS_TIMEOUT")
            } finally {
                if (active === playback) active = null
                dispose(playback, listener)
            }
        }
    }

    private fun acquireFocus(playback: Playback) {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change != AudioManager.AUDIOFOCUS_GAIN && active === playback) {
                    fail(playback, "FOCUS_LOST")
                    stop()
                }
            }, mainHandler)
            .build()
        playback.focus = request
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            throw IOException("FOCUS_DENIED")
        }
    }

    /** 실제 PCM 무음을 먼저 출력한다. 단순 지연을 선행 묵음으로 대신하지 않는다. */
    private suspend fun playSilence(playback: Playback) {
        val frames = SAMPLE_RATE * PRE_ROLL_MS / 1000
        val format = AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()
        val track = AudioTrack.Builder().setAudioAttributes(attributes).setAudioFormat(format)
            .setBufferSizeInBytes(frames * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
        playback.silence = track
        val ended = CompletableDeferred<Unit>()
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) { ended.complete(Unit) }
            override fun onPeriodicNotification(track: AudioTrack) = Unit
        }, mainHandler)
        if (track.write(ShortArray(frames), 0, frames) != frames ||
            track.setNotificationMarkerPosition(frames) != AudioTrack.SUCCESS) throw IOException("SILENCE_OUTPUT")
        track.play()
        try {
            withTimeout(SILENCE_TIMEOUT_MS) {
                select<Unit> {
                    ended.onAwait { }
                    playback.finished.onAwait { }
                }
            }
        }
        finally {
            playback.silence = null
            track.setPlaybackPositionUpdateListener(null)
            try { track.stop() } finally { track.release() }
        }
    }

    private fun fail(playback: Playback, code: String) {
        val error = IOException("TTS_$code")
        playback.initialized.completeExceptionally(error)
        playback.finished.completeExceptionally(error)
    }

    override fun stop() {
        val playback = active ?: return
        active = null
        fail(playback, "STOPPED")
        cleanup { playback.silence?.pause() }
        cleanup { playback.tts.stop() }
        playback.focus?.let { request -> cleanup { audio.abandonAudioFocusRequest(request) } }
        playback.focus = null
    }

    private fun dispose(playback: Playback, listener: EventListener) {
        cleanup { playback.tts.stop() }
        cleanup { playback.tts.removeEventListener(listener) }
        cleanup { playback.tts.release() }
        playback.silence?.let { track -> cleanup { track.release() } }
        playback.focus?.let { request -> cleanup { audio.abandonAudioFocusRequest(request) } }
    }

    private fun cleanup(operation: () -> Unit) {
        try { operation() }
        catch (error: Exception) { Log.w("RoadSpeech", "CLEANUP ${error.javaClass.simpleName}") }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val PRE_ROLL_MS = 160
        const val SILENCE_TIMEOUT_MS = 2_000L
        const val REQUEST_TIMEOUT_MS = 35_000L
    }
}
