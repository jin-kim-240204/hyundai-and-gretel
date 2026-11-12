package com.jinkim.hyundaiAndGretel.demo

import android.content.Context
import android.os.SystemClock
import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DemoRuntime(
    private val scope: CoroutineScope,
    private val host: DemoServiceContract,
    private val store: ScenarioStore,
    private val active: StateFlow<String?>,
    private val restrictions: StateFlow<UiRestrictions>,
    private val clock: () -> Long,
    private val utc: () -> Long,
    private val input: DemoInputSource,
) {
    constructor(context: Context, scope: CoroutineScope, host: DemoServiceContract, store: ScenarioStore,
                active: StateFlow<String?>, restrictions: StateFlow<UiRestrictions>) :
        this(scope, host, store, active, restrictions, SystemClock::elapsedRealtime, System::currentTimeMillis, AndroidDemoInputSource(context))

    val enabled = true
    private val mutable = MutableStateFlow(DemoState())
    val state = mutable.asStateFlow()
    private val mutableNotes = MutableStateFlow<List<RoadNote>>(emptyList())
    val notes = mutableNotes.asStateFlow()
    private var owner: Any? = null
    private var work: Job? = null
    private var motion: Job? = null
    private var inputDeadline: Job? = null
    private var cleanup: Job? = null
    private var gate: StartGate? = null
    private var probeSeen: CompletableDeferred<Unit>? = null
    private var startSeen: CompletableDeferred<Unit>? = null
    private var resetWriting = false
    val hasPendingWrites get() = resetWriting
    val writesBusy get() = host.busy || resetWriting
    val transitioning get() = state.value.transitioning || cleanup?.isActive == true
    val canSubmit get() = state.value.phase == DemoPhase.READY && state.value.actor == "A" && active.value != null
    val blocksNewWrites get() = transitioning

    init {
        scope.launch {
            active.collectLatest { id ->
                mutable.value = state.value.copy(scenarioId = id)
                mutableNotes.value = emptyList()
                if (id != null) {
                    val ownContext = coroutineContext
                    try { store.observeNotes(id).collect { rows -> ownContext.ensureActive(); mutableNotes.value = rows } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { ownContext.ensureActive(); mutable.value = state.value.copy(message = "시연 쪽지 조회 실패") }
                }
            }
        }
        scope.launch {
            restrictions.collect { policy ->
                if (!policy.canSetup && state.value.transitioning && !resetWriting) stop("규제로 시연 설정이 취소됐습니다")
            }
        }
    }

    private fun allowed(): Boolean {
        val reason = when {
            !restrictions.value.canSetup -> "규제 미연결 또는 설정 제한으로 조작할 수 없습니다"
            host.busy || hasPendingWrites -> "저장 완료 후 변경할 수 있습니다"
            transitioning -> "위치 전환 또는 정리 중입니다"
            else -> null
        }
        if (reason != null) mutable.value = state.value.copy(message = reason)
        return reason == null
    }
    private fun checkOwner(token: Any, setup: Boolean = true) {
        scope.coroutineContext.ensureActive()
        check(owner === token) { "이전 시연 작업" }
        if (setup) check(restrictions.value.canSetup) { "시연 설정이 제한됐습니다" }
    }
    private suspend fun closeInput() {
        if (inputDeadline !== cleanup) inputDeadline?.cancel()
        motion?.cancelAndJoin(); motion = null
        input.close()
    }

    private fun configure(change: (DemoState) -> DemoState) {
        if (!allowed()) return
        val token = Any(); owner = token
        mutable.value = state.value.copy(phase = DemoPhase.PREPARING, message = null)
        work = scope.launch {
            try {
                try { closeInput() } finally { host.stopTracking() }
                ensureActive(); checkOwner(token)
                val next = change(state.value)
                host.changeActor(next.actor)
                mutable.value = next.copy(phase = DemoPhase.STOPPED, current = null, target = null, probe = null, targetNoteId = null)
                host.writesFinished()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fail(token, error) }
        }
    }
    fun role(actor: String) { require(actor in setOf("A", "B")); configure { it.copy(actor = actor) } }
    fun mode(mode: DemoMode) = configure { it.copy(mode = mode) }

    fun requestStart(): Boolean {
        if (state.value.phase == DemoPhase.READY) return true
        if (!allowed()) return true
        val id = active.value
        if (id == null) { mutable.value = state.value.copy(message = "먼저 새 시연을 초기화해주세요"); return true }
        val token = Any(); owner = token
        val selected = state.value
        val previous = host.currentFix()?.let(DemoGeometry::progress)
        mutable.value = selected.copy(phase = DemoPhase.PREPARING, message = null)
        work = scope.launch {
            try {
                try { closeInput() } finally { host.stopTracking() }
                val rows = store.observeNotes(id).first()
                ensureActive(); checkOwner(token)
                check(active.value == id) { "시나리오가 변경됐습니다" }
                val (target, note) = DemoGeometry.start(selected.actor, rows, utc())
                val probe = DemoGeometry.probe(target, previous)
                var validatedAt = clock()
                if (selected.mode == DemoMode.WEB_CONSOLE) {
                    input.prepare()
                    validatedAt = clock()
                    mutable.value = state.value.copy(inputStatus = "mock 권한/콘솔 응답 확인 · SDK 대기")
                } else mutable.value = state.value.copy(inputStatus = "앱 provider 정리 · 외부 SDK 입력 대기")
                ensureActive(); checkOwner(token)
                check(note == null || note.isActiveAt(utc())) { "대상 쪽지가 만료됐습니다" }
                val nextGate = StartGate(probe, target, clock)
                gate = nextGate; probeSeen = CompletableDeferred(); startSeen = CompletableDeferred()
                mutable.value = state.value.copy(phase = DemoPhase.PROBE, target = target, probe = probe, current = null, targetNoteId = note?.noteId)
                host.startTracking()
                ensureActive(); checkOwner(token)
                if (selected.mode == DemoMode.WEB_CONSOLE) {
                    input.inject(probe)
                    startMotion(token, target, validatedAt)
                }
                withTimeout(15_000) { probeSeen!!.await() }
                ensureActive(); checkOwner(token)
                nextGate.armTarget()
                mutable.value = state.value.copy(phase = DemoPhase.START)
                if (selected.mode == DemoMode.WEB_CONSOLE) input.inject(target)
                withTimeout(15_000) { startSeen!!.await() }
                ensureActive(); checkOwner(token, setup = false)
                mutable.value = state.value.copy(phase = DemoPhase.READY)
            } catch (timeout: TimeoutCancellationException) { fail(token, IllegalStateException("SDK 출발 위치 확인 시간 초과", timeout)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fail(token, error) }
        }
        return true
    }

    fun accept(fix: LocationFix?): Boolean {
        if (fix == null) return state.value.phase == DemoPhase.READY
        val currentGate = gate ?: return false
        val phase = state.value.phase
        if (phase == DemoPhase.READY) { mutable.value = state.value.copy(current = DemoGeometry.progress(fix), receivedAtElapsed = fix.receivedElapsedMillis); return true }
        if (phase !in setOf(DemoPhase.PROBE, DemoPhase.START) || !restrictions.value.canSetup) return false
        val accepted = currentGate.accept(fix)
        if (currentGate.phase == DemoPhase.START) probeSeen?.complete(Unit)
        if (accepted) {
            mutable.value = state.value.copy(phase = DemoPhase.READY, current = DemoGeometry.progress(fix), receivedAtElapsed = fix.receivedElapsedMillis, inputStatus = "실제 SDK 출발점 확인")
            startSeen?.complete(Unit)
        }
        return accepted
    }

    private fun startMotion(token: Any, initial: Double, validatedAt: Long) {
        motion = scope.launch {
            val currentMotion = coroutineContext.job
            var deadline: Job? = null
            var progress = initial
            var previous = clock()
            var lastSuccess = validatedAt
            try {
                while (isActive) {

                    deadline = scope.launch expiry@{
                        delay((lastSuccess + 2_001 - clock()).coerceAtLeast(0))
                        if (owner !== token || motion !== currentMotion) return@expiry
                        val expired = coroutineContext.job
                        cleanup = expired
                        try { fail(token, IllegalStateException("콘솔 입력이 2초 넘게 지연됐습니다")) }
                        finally {
                            if (cleanup === expired) cleanup = null
                            host.writesFinished()
                        }
                    }
                    inputDeadline = deadline
                    delay(1_000)
                    val (speed, gear) = input.readMotion()
                    ensureActive(); checkOwner(token, setup = false)
                    val now = clock()
                    check(now - lastSuccess <= 2_000) { "콘솔 입력이 지연됐습니다" }
                    val delta = consoleDelta(speed, gear, now - previous)

                    val position = when (state.value.phase) {
                        DemoPhase.PROBE -> checkNotNull(state.value.probe)
                        DemoPhase.START -> initial
                        DemoPhase.READY -> { progress += delta; progress }
                        else -> return@launch
                    }
                    DemoGeometry.fix(position, now)
                    lastSuccess = now; previous = now
                    deadline.cancel(); deadline = null
                    input.inject(position)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fail(token, error, fromMotion = true) }
            finally { if (cleanup !== deadline) deadline?.cancel() }
        }
    }

    fun reset(confirmed: Boolean) {
        if (!confirmed || !allowed()) return
        val token = Any(); owner = token
        mutable.value = state.value.copy(phase = DemoPhase.PREPARING, message = null)
        work = scope.launch {
            try {
                try { closeInput() } finally { host.stopTracking() }
                ensureActive(); checkOwner(token)
                host.holdStorage()
                resetWriting = true
                mutable.value = state.value.copy(phase = DemoPhase.RESETTING)
                scope.launch {
                    try {
                        val next = store.reset()
                        withTimeout(15_000) { active.filter { it == next }.first() }
                        if (owner === token) {
                            host.changeActor("A")
                            mutable.value = state.value.copy(actor = "A", phase = DemoPhase.STOPPED, current = null, target = null, probe = null, targetNoteId = null)
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        if (owner === token) mutable.value = state.value.copy(phase = DemoPhase.ERROR, message = "초기화 실패 또는 상태 확인 실패")
                    } finally { resetWriting = false; host.writesFinished() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fail(token, error) }
        }
    }

    fun stop(reason: String? = null) {
        if (resetWriting || cleanup?.isActive == true) return
        val token = Any(); owner = token
        gate = null
        val previousWork = work; previousWork?.cancel()
        inputDeadline?.cancel(); motion?.cancel()
        mutable.value = state.value.copy(phase = DemoPhase.PREPARING, message = reason)
        cleanup = scope.launch {
            try { previousWork?.join(); closeInput() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (owner === token) mutable.value = state.value.copy(phase = DemoPhase.ERROR, message = "GPS 입력 정리 실패"); return@launch }
            finally { host.stopTracking() }
            if (owner === token) {
                mutable.value = state.value.copy(phase = DemoPhase.STOPPED, current = null)
                host.writesFinished()
            }
        }
    }
    private suspend fun fail(token: Any, error: Exception, fromMotion: Boolean = false) {
        if (owner !== token) return
        gate = null
        if (fromMotion || currentCoroutineContext().job === inputDeadline) work?.cancel()
        mutable.value = state.value.copy(phase = DemoPhase.ERROR, message = error.message ?: "시연 입력 실패")
        try { host.stopTracking() }
        finally {
            try {
                if (!fromMotion) closeInput() else input.close()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = state.value.copy(message = "GPS 입력 정리 실패") }
        }
    }
    fun onSourceFailure() {
        owner = null; gate = null
        work?.cancel(); motion?.cancel()
        if (inputDeadline !== cleanup) inputDeadline?.cancel()
        try { input.close() } catch (_: Exception) { }
        mutable.value = state.value.copy(phase = DemoPhase.ERROR, message = "SDK 위치 연결 실패")
    }
    fun close() {
        owner = null; gate = null
        work?.cancel(); motion?.cancel(); inputDeadline?.cancel(); cleanup?.cancel()
        input.close()
    }
}
