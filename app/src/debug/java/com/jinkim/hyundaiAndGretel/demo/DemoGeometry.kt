package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import kotlin.math.abs
import kotlin.math.cos

object DemoGeometry {
    private val scale = 6_371_000.0 * cos(Math.toRadians(37.5))
    val end = scale * Math.toRadians(0.03)
    fun fix(progress: Double, received: Long): LocationFix {
        require(progress.isFinite() && progress in 0.0..end) { "출발점이 경로 밖입니다" }
        return LocationFix(37.5, 127.02 + Math.toDegrees(progress / scale), received)
    }
    fun progress(fix: LocationFix) = SyntheticRoute.project(fix.latitude, fix.longitude, SyntheticSession())?.progressMeters
    fun start(actor: String, notes: List<RoadNote>, now: Long): Pair<Double, RoadNote?> {
        if (actor == "A") return 610.0 to null
        val note = notes.filter { it.authorId == "A" && it.isActiveAt(now) && it.route?.routeId == "R1" && it.route.direction == 1 }
            .sortedWith(compareByDescending<RoadNote> { it.createdAtUtcMillis }.thenBy { it.noteId }).firstOrNull()
            ?: error("출발 기준 쪽지가 없습니다")
        val start = note.route!!.progressMeters - 600
        fix(start, 0) // Never clamp an invalid anchor into the supported route.
        return start to note
    }
    fun probe(target: Double, previous: Double?): Double =
        listOf(target + 25, target - 25, target + 50, target - 50).firstOrNull {
            it in 0.0..end && abs(it - target) >= 25 && (previous == null || abs(it - previous) >= 25)
        } ?: error("준비점을 정할 수 없습니다")
}

enum class DemoMode { WEB_CONSOLE, EXTERNAL_ADB }
enum class DemoPhase { STOPPED, PREPARING, PROBE, START, READY, RESETTING, ERROR }
data class DemoState(
    val actor: String = "A", val mode: DemoMode = DemoMode.WEB_CONSOLE,
    val phase: DemoPhase = DemoPhase.STOPPED, val scenarioId: String? = null,
    val target: Double? = null, val probe: Double? = null, val current: Double? = null,
    val targetNoteId: String? = null, val message: String? = null,
    val receivedAtElapsed: Long? = null, val inputStatus: String = "입력/권한 미확인",
) { val transitioning get() = phase in setOf(DemoPhase.PREPARING, DemoPhase.PROBE, DemoPhase.START, DemoPhase.RESETTING) }

/** Deliberately a two-point cache heuristic, not SDK measurement authentication. */
class StartGate(val probe: Double, val target: Double, private val now: () -> Long) {
    var phase = DemoPhase.PROBE; private set
    private var commandAt = now()
    private var targetArmed = false
    fun armTarget() { targetArmed = true; commandAt = now() }
    fun accept(fix: LocationFix): Boolean {
        if (fix.receivedElapsedMillis < commandAt || fix.receivedElapsedMillis > now()) return false
        if (phase == DemoPhase.START && !targetArmed) return false
        val progress = DemoGeometry.progress(fix) ?: return false
        val expected = if (phase == DemoPhase.PROBE) probe else target
        // Latitude/longitude round trips can exceed an exact boundary by nanometers.
        if (abs(progress - expected) > 5.0 + 1e-6) return false
        if (phase == DemoPhase.PROBE) { phase = DemoPhase.START; commandAt = now(); return false }
        phase = DemoPhase.READY
        return true
    }
}

fun consoleDelta(speedKph: Double, gear: String, elapsedMillis: Long): Double {
    require(speedKph.isFinite() && speedKph in 0.0..240.0) { "잘못된 콘솔 속도" }
    require(elapsedMillis in 0L..2_000L) { "콘솔 입력이 지연됐습니다" }
    return when (gear) {
        "Drive" -> speedKph / 3.6 * elapsedMillis / 1_000
        "Park", "Neutral" -> 0.0
        "Reverse" -> error("이번 시연은 D 방향만 지원합니다")
        else -> error("알 수 없는 콘솔 기어")
    }
}
