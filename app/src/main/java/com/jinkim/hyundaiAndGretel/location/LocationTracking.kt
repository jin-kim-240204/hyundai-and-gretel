package com.jinkim.hyundaiAndGretel.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TrackingStatus {
    STOPPED, STARTING, WAITING, RECEIVING, UNAVAILABLE, UNSUPPORTED, FAILED;

    val isCollecting: Boolean get() = when (this) {
        STARTING, WAITING, RECEIVING, UNAVAILABLE -> true
        STOPPED, UNSUPPORTED, FAILED -> false
    }
}

/** 시작·중지 명령과 콜백 처리는 소유자의 메인 코루틴 실행 문맥에서 순서대로 실행한다. */
class LocationTracking(
    private val scope: CoroutineScope,
    private val createSource: () -> LocationSource,
    private val log: (String) -> Unit,
    private val onFailure: () -> Unit,
    private val clock: () -> Long,
    private val acceptLocation: (LocationFix?) -> Boolean = { true },
) {
    private val mutableStatus = MutableStateFlow(TrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private val mutablePosition = MutableStateFlow<TrackedPosition?>(null)
    val currentPosition = mutablePosition.asStateFlow()
    private var source: LocationSource? = null
    private var generation = 0L
    val sessionId get() = generation
    val proximitySource: ProximitySource? get() = source
    fun ownsSource(owner: ProximitySource): Boolean = source === owner
    private val buffer = LocationBuffer()

    // 리뷰용: 수집 시작 흐름: 중복 시작 방지 → 새 세션 준비 → SDK 소스 생성 → 콜백 등록 → 등록 결과 반영.
    fun start() {
        if (source != null) return
        val session = prepareSession()
        try {
            val current = createSource()
            source = current
            val isSupported = registerCallbacks(current, session)
            handleRegistration(isSupported, session)
        } catch (cancelled: CancellationException) {
            stop()
            throw cancelled
        } catch (error: Exception) {
            fail(error)
        }
    }

    private fun prepareSession(): Long {
        mutablePosition.value = null
        buffer.reset()
        val session = ++generation
        update(TrackingStatus.STARTING)
        return session
    }

    // 리뷰용: SDK 콜백은 나중에 들어온다. 원래 세션 번호를 함께 잡아두고 메인 실행 문맥으로 전달한다.
    private fun registerCallbacks(current: LocationSource, session: Long): Boolean = current.start(
        onLocation = { fix, receivedElapsedMillis ->
            scope.launch { handleLocation(session, fix, receivedElapsedMillis) }
        },
        onError = { error ->
            scope.launch { handleError(session, error) }
        },
    )

    private fun handleRegistration(isSupported: Boolean, session: Long) {
        if (!isSupported) {
            stop()
            update(TrackingStatus.UNSUPPORTED)
            onFailure()
            return
        }
        log("session=$session REGISTERED")
        update(TrackingStatus.WAITING)
    }

    // 리뷰용: 실제 처리 시점에 이전 세션을 거른 뒤, 시연 출발점 확인과 버퍼 정책을 통과한 위치만 현재 위치로 공개한다.
    private fun handleLocation(session: Long, fix: LocationFix?, receivedElapsedMillis: Long) {
        if (generation != session) return
        if (!acceptLocation(fix)) return
        when (buffer.offer(fix = fix, receivedElapsedMillis = receivedElapsedMillis, nowElapsedMillis = clock())) {
            LocationBuffer.Update.IGNORED -> Unit
            LocationBuffer.Update.INVALIDATED -> {
                mutablePosition.value = null
                update(TrackingStatus.UNAVAILABLE)
            }
            LocationBuffer.Update.ACCEPTED -> {
                mutablePosition.value = TrackedPosition(session, checkNotNull(fix))
                update(TrackingStatus.RECEIVING)
                log("session=$session LOCATION lat=${fix?.latitude} lon=${fix?.longitude} elapsed=$receivedElapsedMillis")
                log("session=$session BUFFER_APPEND size=${buffer.size} oldest=${buffer.oldestElapsedMillis} latest=${buffer.latestElapsedMillis}")
            }
        }
    }

    private fun handleError(session: Long, error: Exception) {
        if (generation == session) fail(error)
    }

    // 리뷰용: 세션 번호부터 바꿔 늦은 콜백을 차단하고 메모리 버퍼를 비운 뒤 기존 SDK 소스를 정리한다.
    fun stop() {
        mutablePosition.value = null
        ++generation
        val previous = source
        source = null
        buffer.reset()
        update(TrackingStatus.STOPPED)
        try {
            previous?.stop()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            log("session=$generation CLEANUP_ERROR ${error.javaClass.simpleName}")
        }
    }

    // 리뷰용: 일반 실패는 수집 중지와 실패 상태로 바꾸지만, 코루틴 취소는 정리 후 반드시 다시 던진다.
    fun fail(error: Exception, operation: String = "SDK") {
        if (error is CancellationException) {
            stop()
            throw error
        }
        stop()
        update(TrackingStatus.FAILED)
        log("session=$generation ${operation}_ERROR ${error.javaClass.simpleName}")
        onFailure()
    }

    /** 위치 수집 실행 문맥에서 진단용 표본 수와 수신 시각 범위를 읽는다. */
    fun bufferSnapshot() = LocationBufferSnapshot(buffer.size, buffer.oldestElapsedMillis, buffer.latestElapsedMillis)

    fun selectAnchor(): AnchorSelection = buffer.select(clock())

    private fun update(value: TrackingStatus) {
        if (mutableStatus.value == value) return
        mutableStatus.value = value
        log("session=$generation STATE $value")
    }
}
