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
    private class Host : DemoHost {
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
    private class Input : DemoInput {
        val injected = mutableListOf<Double>()
        var closes = 0; var deny = false
        var motionGate: CompletableDeferred<Unit>? = null
        override suspend fun prepare() { if (deny) throw SecurityException("mock 권한 없음") }
        override fun inject(progress: Double) { injected += progress }
        override suspend fun readMotion(): Pair<Double, String> {
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
            assertFalse(runtime.accept(fix(610.0))) // Cached target is not enough before probe.
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
        assertEquals(610.0, h.runtime.state.value.current!!, 1e-6) // Injection is not an SDK callback.
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
        h.ux.value = UiRestrictions(connected = true, optimized = true, flags = 1 shl 5)
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
