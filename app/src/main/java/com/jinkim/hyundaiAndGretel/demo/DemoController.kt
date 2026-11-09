package com.jinkim.hyundaiAndGretel.demo

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.jinkim.hyundaiAndGretel.data.*
import com.jinkim.hyundaiAndGretel.location.PleosLocationSource
import com.jinkim.hyundaiAndGretel.notes.*
import ai.pleos.playground.location.fused.model.ProximityTransition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** One visible synthetic demo session. No Android UI or ViewModel is retained here. */
class DemoController(
    context: Context, private val notes: RoomNotes, private val receipts: ReceiptStore,
    private val spatial: SpatialIndex, private val scope: CoroutineScope,
) {
    private val preferences = context.getSharedPreferences("road-note-demo", Context.MODE_PRIVATE)
    private var scenario = preferences.getString("scenario", null) ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString("scenario", it).commit())
    }
    private val source = PleosLocationSource(context)
    private val buffer = LocationBuffer()
    private val mutex = Mutex()
    private var generation = 0L
    private var sdkJob: Job? = null
    private var nearbyJob: Job? = null
    private var inboxJob: Job? = null
    private var cells = emptyList<String>()
    private var candidates = emptyList<RoadNote>()
    private var allInbox = emptyList<InboxItem>()
    private val alerts = mutableMapOf<String, String?>()
    private val attemptedAt = mutableMapOf<String, Long>()
    private val insideAlerts = mutableSetOf<String>()
    private var ready = false
    private var lastSdkTime: Long? = null
    private var lastDeliveredSdkTime: Long? = null
    private var clockDelta = System.currentTimeMillis() - SystemClock.elapsedRealtime()
    private val _state = MutableStateFlow(DemoState(keepScreenOn = preferences.getBoolean("keepScreenOn", true)))
    val state = _state.asStateFlow()
    val actor get() = _state.value.actor
    init {
        observeInbox()
        scope.launch {
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                if (kotlin.math.abs(System.currentTimeMillis() - now - clockDelta) > 2_000) {
                    clockDelta = System.currentTimeMillis() - now
                    stop("차량 시각 변경: 시연을 초기화하세요")
                }
                val samples = buffer.snapshot(now)
                _state.update { it.copy(bufferSeconds = samples.takeIf { it.size > 1 }?.let { (it.last().elapsedMillis-it.first().elapsedMillis)/1000 } ?: 0,
                    canReport = it.running && actor == Actor.A && buffer.anchor(now) != null,
                    inbox = allInbox.filter { entry -> entry.note.expiresAt > System.currentTimeMillis() }) }
                if (_state.value.running) reconcile()
            }
        }
    }

    fun start() {
        if (_state.value.running) return
        val token = ++generation
        _state.update { it.copy(running = true, status = "위치 연결 중") }
        sdkJob = scope.launch {
            try {
                source.start({ data -> scope.launch {
                    if (token != generation) return@launch
                    if (data == null) { invalidate("위치를 사용할 수 없음"); return@launch }
                    val progress = DemoRoute.progress(data.latitude, data.longitude)
                    if (progress == null) { invalidate("시연 경로 밖 위치"); return@launch }
                    if (lastSdkTime != null && data.timestampMillis < lastSdkTime!!) {
                        invalidate("위치 시각 역행"); lastSdkTime = data.timestampMillis; return@launch
                    }
                    if (data.timestampMillis == lastDeliveredSdkTime) return@launch
                    lastSdkTime = data.timestampMillis
                    lastDeliveredSdkTime = data.timestampMillis
                    val now = SystemClock.elapsedRealtime()
                    val heading = data.heading.takeIf { it.isFinite() && it >= 0 && it < 360 }
                    val previous = _state.value.location
                    val direction = when {
                        previous == null -> "FORWARD" // Fixed demo fixture orientation, not a road-matching result.
                        progress < previous.progressMeters - 1 -> "REVERSE"
                        progress > previous.progressMeters + 1 -> "FORWARD"
                        else -> previous.direction
                    }
                    val sample = LocationSample(data.latitude, data.longitude, now, progress, heading, direction)
                    buffer.add(sample)
                    _state.update { it.copy(location = sample, h3Cell = spatial.cell(sample.latitude, sample.longitude), status = "위치 수신 중") }
                    val nextCells = spatial.nearby(sample.latitude, sample.longitude)
                    if (nextCells != cells) observeNearby(nextCells)
                    reconcile()
                    // Covers ENTER arriving before a location update, and direction changes inside a radius.
                    insideAlerts.toList().forEach { pickup(it, token) }
                } }, { error -> scope.launch { if (token == generation) invalidate("위치 오류: ${error.javaClass.simpleName}") } })
                if (generation == token) { ready = true; reconcile() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (token == generation) stop("연결 실패: ${error.javaClass.simpleName}") }
        }
    }

    private fun invalidate(message: String) {
        buffer.clear()
        _state.update { it.copy(location = null, canReport = false, bufferSeconds = 0, status = message) }
        clearAlerts()
    }
    fun stop(message: String = "시연 중지") {
        generation++
        sdkJob?.cancel(); nearbyJob?.cancel()
        ready = false; cells = emptyList(); candidates = emptyList(); lastSdkTime = null
        clearAlerts()
        runCatching { source.stop() }.onFailure { Log.w("RoadNotes", "SDK_RELEASE_FAILED", it) }
        invalidate(message)
        _state.update { it.copy(running = false, candidateCount = 0) }
    }
    suspend fun switchActor(next: Actor) = mutex.withLock {
        if (next == actor) return@withLock
        val running = _state.value.running
        stop()
        _state.update { it.copy(actor = next, inbox = emptyList(), lastResult = "") }
        observeInbox()
        if (running) start()
    }
    suspend fun reset() = mutex.withLock {
        stop()
        val previous = scenario
        val next = UUID.randomUUID().toString()
        check(preferences.edit().putString("scenario", next).commit()) { "RESET_FAILED" }
        scenario = next
        _state.value = DemoState(keepScreenOn = preferences.getBoolean("keepScreenOn", true))
        allInbox = emptyList()
        observeInbox()
        notes.deleteScenario(previous)
        notes.clean(scenario, System.currentTimeMillis())
    }
    private fun observeInbox() {
        inboxJob?.cancel()
        val tokenScenario = scenario
        val recipient = actor
        inboxJob = scope.launch {
            receipts.observe(tokenScenario, recipient).catch { _state.update { it.copy(status = "수신함 조회 실패") } }.collect {
                allInbox = it
                _state.update { s -> s.copy(inbox = it.filter { entry -> entry.note.expiresAt > System.currentTimeMillis() }) }
                reconcile()
            }
        }
    }
    private fun observeNearby(next: List<String>) {
        cells = next
        nearbyJob?.cancel()
        val token = generation
        nearbyJob = scope.launch {
            notes.observeNearby(scenario, cells).catch { _state.update { it.copy(status = "주변 쪽지 조회 실패") } }.collect { rows ->
                if (generation != token) return@collect
                candidates = rows
                _state.update { it.copy(noteCount = rows.size, nearbyNotes = rows) }
                reconcile()
            }
        }
    }
    private fun clearAlerts() {
        alerts.values.filterNotNull().forEach { runCatching { source.remove(it) } }
        alerts.clear(); attemptedAt.clear(); insideAlerts.clear()
        _state.update { it.copy(registeredCount = 0) }
    }
    private fun reconcile() {
        if (!ready || !_state.value.running) return
        val now = SystemClock.elapsedRealtime()
        val current = _state.value.location?.takeIf { now - it.elapsedMillis in 0..5_000 }
        val received = allInbox.map { it.note.id }.toSet()
        val wanted = if (actor != Actor.B || current == null) emptyList() else candidates.filter {
            it.authorId != actor.id && it.id !in received && it.expiresAt > System.currentTimeMillis() &&
                it.routeId == DemoRoute.ID && it.direction == current.direction &&
                (it.progressMeters - current.progressMeters) * (if (current.direction == "FORWARD") 1 else -1) >= 0
        }.sortedBy { it.progressMeters - current!!.progressMeters }
        // shortcut: cap the small demo registry at 16; expose overflow until the SDK's supported limit is verified.
        val limited = wanted.take(16)
        val ids = limited.map { it.id }.toSet()
        (alerts.keys.toSet() - ids).forEach { id ->
            insideAlerts -= id
            alerts.remove(id)?.let { runCatching { source.remove(it) } }
        }
        _state.update { it.copy(candidateCount = wanted.size, overflowCount = (wanted.size-16).coerceAtLeast(0)) }
        for (note in limited) {
            if (note.id in alerts || now - (attemptedAt[note.id] ?: -10_000) < 5_000) continue
            val token = generation
            alerts[note.id] = null
            attemptedAt[note.id] = now
            try {
                source.register(note.latitude, note.longitude, { alertId -> scope.launch {
                    if (token != generation || note.id !in alerts) { runCatching { source.remove(alertId) }; return@launch }
                    alerts[note.id] = alertId
                    _state.update { it.copy(registeredCount = alerts.values.count { id -> id != null }) }
                    Log.i("RoadNotes", "PROXIMITY_REGISTERED")
                    val currentLocation = _state.value.location
                    if (currentLocation != null && DemoRoute.distance(currentLocation.latitude, currentLocation.longitude, note.latitude, note.longitude) <= 150) {
                        insideAlerts += note.id
                        pickup(note.id, token)
                    }
                } }, { event -> scope.launch {
                    if (token != generation || alerts[note.id] != event.alertId) return@launch
                    _state.update { it.copy(lastEvent = event.transition.name) }
                    Log.i("RoadNotes", "PROXIMITY_${event.transition}")
                    if (event.transition == ProximityTransition.ENTER) {
                        insideAlerts += note.id
                        pickup(note.id, token)
                    } else insideAlerts -= note.id
                } }, { error -> scope.launch {
                    if (token == generation) {
                        alerts.remove(note.id)?.let { runCatching { source.remove(it) } }
                        _state.update { it.copy(status = "접근 등록 실패: ${error.javaClass.simpleName}") }
                    }
                } })
            } catch (error: Exception) {
                alerts.remove(note.id)
                _state.update { it.copy(status = "접근 등록 실패: ${error.javaClass.simpleName}") }
            }
        }
        _state.update { it.copy(registeredCount = alerts.values.count { id -> id != null }) }
    }
    private suspend fun pickup(id: String, token: Long) = mutex.withLock {
        if (token != generation || !_state.value.running || actor != Actor.B) return@withLock
        val current = _state.value.location ?: return@withLock
        try {
            if (receipts.record(id, scenario, actor, current, System.currentTimeMillis(), SystemClock.elapsedRealtime())) {
                _state.update { it.copy(lastResult = "쪽지를 받았어요") }
                Log.i("RoadNotes", "NOTE_RECEIVED")
            }
        } catch (_: Exception) { _state.update { it.copy(status = "수신 저장 실패") } }
    }
    fun capture(): ReportContext = ReportContext(generation, scenario, actor, System.currentTimeMillis(),
        SystemClock.elapsedRealtime(), buffer.snapshot(SystemClock.elapsedRealtime()))

    suspend fun report(content: String, category: Category, context: ReportContext = capture()): ReportResult {
        if (content.trim().length !in 1..500) return ReportResult.Failed("INVALID_INPUT")
        val anchor = selectAnchor(context.samples, context.elapsed) ?: return ReportResult.Failed("LOCATION_BUFFER_NOT_READY")
        return mutex.withLock {
            if (context.generation != generation || context.scenario != scenario || context.actor != actor || actor != Actor.A || !_state.value.running)
                return@withLock ReportResult.Failed("SESSION_NOT_READY")
            try {
                val now = System.currentTimeMillis()
                val note = RoadNote(UUID.randomUUID().toString(), UUID.randomUUID().toString(), scenario, actor.id,
                    content.trim(), category, context.utc, now, now+600_000,
                    anchor.sample.latitude, anchor.sample.longitude, anchor.sample.heading, anchor.offsetMillis,
                    spatial.cell(anchor.sample.latitude, anchor.sample.longitude), anchor.sample.progressMeters, direction = anchor.sample.direction)
                notes.clean(scenario, now)
                val saved = notes.create(note)
                _state.update { it.copy(lastResult = "쪽지를 남겼어요 (약 ${saved.offsetMillis/1000}초 전 위치)") }
                Log.i("RoadNotes", "NOTE_SAVED")
                ReportResult.Saved(saved)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { ReportResult.Failed("SAVE_FAILED") }
        }
    }
    fun keepScreenOn(enabled: Boolean) {
        preferences.edit().putBoolean("keepScreenOn", enabled).apply()
        _state.update { it.copy(keepScreenOn = enabled) }
    }
    fun operationFailed(message: String) { _state.update { it.copy(status = message) } }
    fun displayed(noteId: String) {
        val token = generation; val recipient = actor
        scope.launch { mutex.withLock {
            if (token == generation) runCatching { receipts.markDisplayed(noteId, recipient, System.currentTimeMillis()) }
                .onFailure { _state.update { it.copy(status = "표시 이력 저장 실패") } }
        } }
    }
}

data class DemoState(val actor: Actor = Actor.A, val running: Boolean = false, val location: LocationSample? = null,
    val status: String = "합성 위치 시연 준비", val h3Cell: String = "", val keepScreenOn: Boolean = true, val bufferSeconds: Long = 0, val canReport: Boolean = false,
    val noteCount: Int = 0, val candidateCount: Int = 0, val registeredCount: Int = 0, val overflowCount: Int = 0,
    val nearbyNotes: List<RoadNote> = emptyList(), val inbox: List<InboxItem> = emptyList(), val lastResult: String = "", val lastEvent: String = "")
data class ReportContext(val generation: Long, val scenario: String, val actor: Actor, val utc: Long,
                         val elapsed: Long, val samples: List<LocationSample>)
sealed interface ReportResult {
    data class Saved(val note: RoadNote) : ReportResult
    data class Failed(val reason: String) : ReportResult
}
