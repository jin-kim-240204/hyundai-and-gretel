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
import java.util.Locale

/** SDK·AudioTrack의 공개 API만 사용한다. 서비스 Main 수명에서 생성/중지한다. */
class PleosNoteSpeaker(
    context: Context,
    private val log: (String) -> Unit = {},
) : NoteSpeaker {
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
            log("TTS_CREATE")
            val playback = try {
                Playback(TextToSpeech(context, Mode.ON_DEVICE))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                throw SpeechFailure("TTS_CREATE", error.javaClass.simpleName)
            }
            active = playback
            val listener = object : EventListener {
                override fun onReady() { log("TTS_READY") }
                override fun onStart() { log("TTS_STARTED") }
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onDone() {
                    if (playback.finished.complete(Unit)) log("TTS_DONE")
                }
                override fun onStop() { fail(playback, "TTS_PLAYBACK", "STOPPED") }
                override fun onError(errorType: ErrorType) { fail(playback, "TTS_PLAYBACK", errorType.name) }
            }
            var stage = "TTS_INITIALIZE"
            try {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    log(stage)
                    playback.tts.initialize(object : OnInitializeListener {
                        override fun onSuccess() { playback.initialized.complete(Unit) }
                        override fun onFailure(errorType: ErrorType) { fail(playback, "TTS_INITIALIZE", errorType.name) }
                    })
                    playback.initialized.await()
                    if (active !== playback) throw CancellationException("Speech stopped")
                    log("TTS_INITIALIZED")
                    stage = "TTS_CONFIGURE"
                    playback.tts.addEventListener(listener)
                    playback.tts.setAudioAttributes(attributes)
                    playback.tts.setLocale(Locale.KOREA)
                    if (!canStart()) {
                        log("TTS_SKIPPED_BEFORE_FOCUS")
                        return@withTimeout
                    }
                    stage = "FOCUS_REQUEST"
                    acquireFocus(playback)
                    log("FOCUS_GRANTED")
                    stage = "PCM_PREROLL"
                    log(stage)
                    playSilence(playback)
                    if (playback.finished.isCompleted) playback.finished.await()
                    log("PCM_PREROLL_DONE")
                    if (!canStart()) {
                        log("TTS_SKIPPED_BEFORE_REQUEST")
                        return@withTimeout
                    }
                    stage = "TTS_REQUEST"
                    log(stage)
                    playback.tts.speak(text, true)
                    stage = "TTS_PLAYBACK"
                    playback.finished.await()
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw SpeechFailure(stage, "TIMEOUT")
            } catch (cancelled: CancellationException) {
                log("$stage CANCELLED")
                throw cancelled
            } catch (failure: SpeechFailure) {
                throw failure
            } catch (error: Exception) {
                throw SpeechFailure(stage, error.javaClass.simpleName)
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
                    fail(playback, "FOCUS", "LOST")
                    stop()
                }
            }, mainHandler)
            .build()
        playback.focus = request
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            throw SpeechFailure("FOCUS_REQUEST", "DENIED")
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
        try {
            val ended = CompletableDeferred<Unit>()
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) { ended.complete(Unit) }
                override fun onPeriodicNotification(track: AudioTrack) = Unit
            }, mainHandler)
            val written = track.write(ShortArray(frames), 0, frames)
            if (written != frames) throw SpeechFailure("PCM_WRITE", written.toString())
            val marker = track.setNotificationMarkerPosition(frames)
            if (marker != AudioTrack.SUCCESS) throw SpeechFailure("PCM_MARKER", marker.toString())
            track.play()
            withTimeout(SILENCE_TIMEOUT_MS) {
                select<Unit> {
                    ended.onAwait { }
                    playback.finished.onAwait { }
                }
            }
        } finally {
            playback.silence = null
            cleanup { track.setPlaybackPositionUpdateListener(null) }
            cleanup { track.stop() }
            cleanup { track.release() }
        }
    }

    private fun fail(playback: Playback, stage: String, code: String) {
        val error = SpeechFailure(stage, code)
        playback.initialized.completeExceptionally(error)
        playback.finished.completeExceptionally(error)
    }

    override fun stop() {
        val playback = active ?: return
        active = null
        fail(playback, "TTS_PLAYBACK", "STOPPED")
        cleanup { playback.silence?.pause() }
        cleanup { playback.tts.stop() }
        playback.focus?.let { request -> cleanup { audio.abandonAudioFocusRequest(request) } }
        playback.focus = null
    }

    private fun dispose(playback: Playback, listener: EventListener) {
        cleanup { playback.tts.stop() }
        cleanup { playback.tts.removeEventListener(listener) }
        cleanup { playback.tts.release() }
        playback.focus?.let { request -> cleanup { audio.abandonAudioFocusRequest(request) } }
    }

    private fun cleanup(operation: () -> Unit) {
        try { operation() }
        catch (error: Exception) { log("CLEANUP ${error.javaClass.simpleName}") }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val PRE_ROLL_MS = 160
        const val SILENCE_TIMEOUT_MS = 2_000L
        const val REQUEST_TIMEOUT_MS = 35_000L
    }
}
