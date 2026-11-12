package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoRuntimeTest {
    private class Store : ScenarioStore {
        val active = MutableStateFlow<String?>("00000000-0000-0000-0000-000000000001")
        val rows = MutableStateFlow<List<RoadNote>>(emptyList())
        var resetGate: CompletableDeferred<Unit>? = null
        var resets = 0
        var failReset = false
        override fun observeActive() = active
        override fun observeNotes(scenarioId: String) = rows
        override suspend fun reset(): String {
            resets++; resetGate?.await()
            if (failReset) error("reset")
            val next = "00000000-0000-0000-0000-000000000002"
            active.value = next
            return next
        }
    }
    private class Host : DemoServiceContract {
        override var busy = false
        var starts = 0; var stops = 0; var finished = 0; var actor = "A"
        var holdFails = false
        override fun startTracking() { starts++ }
        override fun stopTracking() { stops++ }
        override fun changeActor(actor: String) { this.actor = actor }
        override fun currentFix(): LocationFix? = null
        override fun holdStorage() { check(!holdFails) { "승격 실패" } }
        override fun writesFinished() { finished++ }
    }
    private class Input : DemoInputSource {
        val injected = mutableListOf<Double>()
        var closes = 0; var deny = false
        var motionGate: CompletableDeferred<Unit>? = null
        var partialResponse = false; var fragments = 0
        override suspend fun prepare() { if (deny) throw SecurityException("mock 권한 없음") }
        override fun inject(progress: Double) { injected += progress }
        override suspend fun readMotion(): Pair<Double, String> {
            while (partialResponse) { delay(400); fragments++ }
            motionGate?.let { withContext(NonCancellable) { it.await() } }
            return 36.0 to "Drive"
        }
        override fun close() { closes++ }
    }
    private inner class Harness(val scope: TestScope) {
        val store = Store(); val host = Host(); val input = Input()
        val ux = MutableStateFlow(UiRestrictions(connected = true))
        val runtime = DemoRuntime(scope.backgroundScope, host, store, store.active, ux, { scope.testScheduler.currentTime }, { 1_000 }, input)
        fun fix(progress: Double) = DemoGeometry.fix(progress, scope.testScheduler.currentTime)
        fun ready() {
            runtime.requestStart(); scope.runCurrent()
            assertEquals(DemoPhase.PROBE, runtime.state.value.phase)
            assertFalse(runtime.accept(fix(610.0))) // 준비점 확인 전에는 캐시된 목표 좌표만으로 시작할 수 없다.
            assertFalse(runtime.accept(fix(runtime.state.value.probe!!)))
            scope.runCurrent()
            assertEquals(DemoPhase.START, runtime.state.value.phase)
            assertTrue(runtime.accept(fix(610.0)))
            scope.runCurrent()
            assertEquals(DemoPhase.READY, runtime.state.value.phase)
        }
    }

    @Test fun onlyOrderedSdkProbeAndTargetOpenGateAndConsoleDoesNotDirectlyChangeCurrent() = runTest {
        val h = Harness(this); h.ready()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(620.0, h.input.injected.last(), 1e-8)
        assertEquals(610.0, h.runtime.state.value.current!!, 1e-6) // 주입 성공만으로 SDK가 실제 좌표를 수신한 것은 아니다.
        assertTrue(h.runtime.accept(h.fix(620.0)))
        assertEquals(620.0, h.runtime.state.value.current!!, 1e-6)
    }
    @Test fun setupAndAcceptedWritesBlockChangesWhileStopRemainsPossible() = runTest {
        val h = Harness(this)
        h.host.busy = true
        h.runtime.role("B"); h.runtime.reset(true); h.runtime.requestStart(); runCurrent()
        assertEquals(0, h.host.starts); assertEquals(0, h.store.resets); assertEquals("A", h.host.actor)
        h.host.busy = false; h.ux.value = UiRestrictions()
        h.runtime.requestStart(); runCurrent(); assertEquals(0, h.host.starts)
        h.runtime.stop(); runCurrent(); assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
    }
    @Test fun changingRestrictionsCancelsUnacceptedGateAndNeverAutomaticallyResumes() = runTest {
        val h = Harness(this)
        h.runtime.requestStart(); runCurrent()
        h.ux.value = UiRestrictions(); runCurrent()
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
        assertFalse(h.runtime.accept(h.fix(610.0)))
        h.ux.value = UiRestrictions(connected = true); runCurrent()
        assertEquals(1, h.host.starts)
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
    }
    @Test fun delayedCancelledHttpCannotInjectAndCleanupCompletesBeforeAnotherStart() = runTest {
        val h = Harness(this); h.ready()
        val gate = CompletableDeferred<Unit>(); h.input.motionGate = gate
        advanceTimeBy(1_000); runCurrent()
        val before = h.input.injected.toList()
        h.runtime.stop(); h.runtime.stop(); runCurrent()
        h.runtime.requestStart(); runCurrent()
        assertEquals(1, h.host.starts)
        gate.complete(Unit); runCurrent()
        assertEquals(before, h.input.injected)
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
        h.runtime.requestStart(); runCurrent()
        assertEquals(2, h.host.starts)
    }
    @Test fun deniedMockTimeoutAndExplicitExternalModeHaveNoFakeFallback() = runTest {
        val h = Harness(this); h.input.deny = true
        h.runtime.requestStart(); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        assertEquals(0, h.host.starts); assertTrue(h.input.injected.isEmpty())
        h.runtime.mode(DemoMode.EXTERNAL_ADB); runCurrent()
        h.runtime.requestStart(); runCurrent()
        assertEquals(1, h.host.starts); assertTrue(h.input.injected.isEmpty())
        advanceTimeBy(15_000); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        assertFalse(h.runtime.accept(h.fix(610.0)))
    }
    @Test fun acceptedResetSurvivesStopAndKeepsRoleChangesLockedUntilDatabaseCompletes() = runTest {
        val h = Harness(this); h.store.resetGate = CompletableDeferred()
        h.runtime.reset(false); runCurrent(); assertEquals(0, h.store.resets)
        h.runtime.reset(true); runCurrent(); assertTrue(h.runtime.hasPendingWrites)
        h.runtime.stop(); h.runtime.role("B"); runCurrent()
        assertEquals("A", h.host.actor)
        h.store.resetGate!!.complete(Unit); runCurrent()
        assertFalse(h.runtime.hasPendingWrites)
        assertEquals(1, h.store.resets)
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
        assertEquals(h.store.active.value, h.runtime.state.value.scenarioId)
    }
    @Test fun resetFailureDoesNotShowSuccessOrChangeScenario() = runTest {
        val h = Harness(this); h.store.failReset = true
        val before = h.store.active.value
        h.runtime.reset(true); runCurrent()
        assertEquals(before, h.store.active.value)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        assertFalse(h.runtime.hasPendingWrites)
    }
    @Test fun gateRejectsNullStaleAndUnarmedTargetAndIncludesFiveMeterBoundary() = runTest {
        val h = Harness(this)
        assertFalse(h.runtime.accept(null))
        val gate = StartGate(635.0, 610.0) { 100L }
        assertFalse(gate.accept(DemoGeometry.fix(635.0, 99)))
        assertEquals(DemoPhase.PROBE, gate.phase)
        assertFalse(gate.accept(DemoGeometry.fix(640.0, 100)))
        assertEquals(DemoPhase.START, gate.phase)
        assertFalse(gate.accept(DemoGeometry.fix(610.0, 100)))
        gate.armTarget()
        assertFalse(gate.accept(DemoGeometry.fix(615.01, 100)))
        assertTrue(gate.accept(DemoGeometry.fix(615.0, 100)))
    }
    @Test fun readyCollectionSurvivesNoSetupButSourceFailureRejectsLateCallbacks() = runTest {
        val h = Harness(this); h.ready()
        h.ux.value = UiRestrictions(connected = true, optimized = true, flags = android.car.drivingstate.CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP)
        runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        h.runtime.onSourceFailure(); runCurrent()
        assertFalse(h.runtime.accept(h.fix(620.0)))
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
    }
    @Test fun failedStoragePromotionNeverAcceptsReset() = runTest {
        val h = Harness(this); h.host.holdFails = true
        h.runtime.reset(true); runCurrent()
        assertEquals(0, h.store.resets)
        assertFalse(h.runtime.hasPendingWrites)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
    }
    @Test fun inputDeadlineIncludesTickStopsSdkBeforeBlockedReadFinishesAndRejectsLateInjection() = runTest {
        val h = Harness(this); h.ready()
        val gate = CompletableDeferred<Unit>(); h.input.motionGate = gate
        val before = h.input.injected.toList()
        val stops = h.host.stops
        advanceTimeBy(2_000); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        advanceTimeBy(1); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        assertTrue(h.host.stops > stops) // 입출력 완료를 기다리지 않고 SDK를 중지해야 한다.
        assertFalse(h.runtime.accept(h.fix(620.0)))
        h.runtime.requestStart(); runCurrent()
        assertEquals(1, h.host.starts) // 이전 입력 읽기와 위치 제공자 정리가 끝나기 전에는 새 입력을 시작하지 않는다.
        gate.complete(Unit); runCurrent()
        assertEquals(before, h.input.injected)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        h.input.motionGate = null
        h.ready() // 정리 후에도 명시적으로 다시 시작해야 한다.
        advanceTimeBy(1_000); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
    }
    @Test fun onlyWholeSuccessfulResponseRenewsDeadlineAndOldDeadlineCannotFailNewSession() = runTest {
        val h = Harness(this); h.ready()
        advanceTimeBy(1_000); runCurrent() // 첫 전체 응답이 1000밀리초 시점에 입력 기한을 갱신한다.
        val gate = CompletableDeferred<Unit>(); h.input.motionGate = gate
        advanceTimeBy(2_000); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        advanceTimeBy(1); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        gate.complete(Unit); runCurrent()
        h.input.motionGate = null
        h.ready()
        advanceTimeBy(500); runCurrent()
        h.runtime.stop(); runCurrent()
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
        h.ready()
        advanceTimeBy(1_501); runCurrent() // 이전 세션의 만료 시각을 넘겨도 새 세션에 영향이 없는지 확인한다.
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
    }
    @Test fun partialResponseProgressDoesNotRenewWholeInputDeadline() = runTest {
        val h = Harness(this); h.ready(); h.input.partialResponse = true
        advanceTimeBy(2_001); runCurrent()
        assertTrue(h.input.fragments > 0)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        val fragments = h.input.fragments
        val injections = h.input.injected.toList()
        advanceTimeBy(4_000); runCurrent()
        assertEquals(fragments, h.input.fragments)
        assertEquals(injections, h.input.injected)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
    }
    @Test fun sdkGateDoesNotInventANewerConsoleValidationTime() = runTest {
        val h = Harness(this)
        val blocked = CompletableDeferred<Unit>(); h.input.motionGate = blocked
        h.runtime.requestStart(); runCurrent() // 입력 준비 단계가 0밀리초 시점에 콘솔을 확인했다.
        h.runtime.accept(h.fix(h.runtime.state.value.probe!!)); runCurrent()
        advanceTimeBy(1_500); runCurrent()
        h.runtime.accept(h.fix(610.0)); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        advanceTimeBy(500); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        advanceTimeBy(1); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        blocked.complete(Unit); runCurrent()
    }
    @Test fun gatePointsRepeatWithFreshConsoleReadsUntilRealSdkConfirmsThenDrivingContinues() = runTest {
        val h = Harness(this)
        h.runtime.requestStart(); runCurrent()
        val probe = h.runtime.state.value.probe!!
        advanceTimeBy(3_000); runCurrent()
        assertEquals(DemoPhase.PROBE, h.runtime.state.value.phase)
        assertTrue(h.input.injected.size >= 4)
        assertTrue(h.input.injected.all { it == probe })
        assertNull(h.runtime.state.value.current)
        h.runtime.accept(h.fix(probe)); runCurrent()
        advanceTimeBy(3_000); runCurrent()
        assertEquals(DemoPhase.START, h.runtime.state.value.phase)
        assertEquals(610.0, h.input.injected.last(), 0.0)
        assertTrue(h.runtime.accept(h.fix(610.0))); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(DemoPhase.READY, h.runtime.state.value.phase)
        assertEquals(620.0, h.input.injected.last(), 1e-6)
        h.runtime.stop(); runCurrent()
        val count = h.input.injected.size
        advanceTimeBy(5_000); runCurrent()
        assertEquals(count, h.input.injected.size)
        assertEquals(DemoPhase.STOPPED, h.runtime.state.value.phase)
    }
    @Test fun blockedConsoleDuringProbeStopsSdkAtDeadlineWithoutLateInjection() = runTest {
        val h = Harness(this)
        val blocked = CompletableDeferred<Unit>(); h.input.motionGate = blocked
        h.runtime.requestStart(); runCurrent()
        val before = h.input.injected.toList()
        advanceTimeBy(2_001); runCurrent()
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
        assertTrue(h.host.stops >= 2)
        blocked.complete(Unit); runCurrent()
        advanceTimeBy(16_000); runCurrent()
        assertEquals(before, h.input.injected)
        assertEquals(DemoPhase.ERROR, h.runtime.state.value.phase)
    }
    @Test fun geometryKeepsFirstAnchorMarginAndRejectsInvalidTargetsAndInput() {
        for (anchor in listOf(605.0, 610.0, 615.0)) {
            val fix = DemoGeometry.fix(anchor, 0)
            val route = SyntheticRoute.project(fix.latitude, fix.longitude, SyntheticSession())!!
            val note = RoadNote("00000000-0000-0000-0000-000000000001","A","note",NoteCategory.WARNING,
                fix.latitude,fix.longitude,0,0,600_000,route)
            assertEquals(anchor - 600, DemoGeometry.start("B", listOf(note), 1).first, 1e-6)
            try { DemoGeometry.start("B", listOf(note.copy(route = route.copy(progressMeters = 596.0))), 1); fail() } catch (_: IllegalArgumentException) { }
        }
        assertEquals(10.0, consoleDelta(36.0,"Drive",1_000),0.0)
        assertEquals(0.0, consoleDelta(36.0,"Park",1_000),0.0)
        for (action in listOf<() -> Unit>({consoleDelta(Double.NaN,"Drive",1_000)}, {consoleDelta(36.0,"Reverse",1_000)}, {consoleDelta(36.0,"?",1_000)}, {consoleDelta(36.0,"Drive",2_001)})) {
            try { action(); fail() } catch (_: Exception) { }
        }
    }
}
