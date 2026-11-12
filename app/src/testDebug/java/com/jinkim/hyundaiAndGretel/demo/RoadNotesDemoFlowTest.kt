package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.spatial.*
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.shouldStopRoadNoteService
import com.jinkim.hyundaiAndGretel.ui.ux.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoadNotesDemoFlowTest {
    private fun id(value: Long) = UUID(0, value).toString()
    private inner class Store(private val now: () -> Long) : ScenarioStore, RoadNoteStore, NearbyNoteStore, ReceiptStore {
        val active = MutableStateFlow<String?>(null)
        val legacy = RoadNote(id(1), "C", "이전 기록", NoteCategory.INFORMATION, 37.5, 127.021, 0, 0, 600_000)
        val other = legacy.copy(noteId = id(2), scenarioId = id(99))
        val originals = MutableStateFlow(listOf(legacy, other))
        val received = MutableStateFlow(listOf(InboxItem(legacy, "B", 1, 1.0, null), InboxItem(other, "B", 2, 1.0, 3)))
        var createGate = CompletableDeferred<Unit>().also { it.complete(Unit) }
        var resetGate = CompletableDeferred<Unit>().also { it.complete(Unit) }
        var failCreate = false; var failReset = false
        var creates = 0; var generation = 100L
        override fun observeActive() = active
        override fun observeNotes(scenarioId: String) = originals.map { rows -> rows.filter { it.scenarioId == scenarioId } }
        override fun observeNearby(cells: Set<String>) = combine(originals, active) { rows, selected ->
            rows.filter { selected != null && it.scenarioId == selected }
        }
        override suspend fun create(note: RoadNote): CreateNoteResult {
            creates++; createGate.await()
            note.validationError()?.let { return CreateNoteResult.Invalid(it) }
            if (failCreate) return CreateNoteResult.StorageFailure
            if (originals.value.any { it.noteId == note.noteId }) return CreateNoteResult.Duplicate
            originals.value += note
            return CreateNoteResult.Created(note)
        }
        override suspend fun findById(noteId: String) = originals.value.find { it.noteId == noteId }
        override fun observeInbox(recipientId: String) = received.map { rows -> rows.filter { it.recipientId == recipientId } }
        override suspend fun receive(request: ReceiptRequest): ReceiveResult {
            val note = findById(request.noteId) ?: return ReceiveResult.INELIGIBLE
            if (note.scenarioId != request.scenarioId) return ReceiveResult.INELIGIBLE
            val distance = eligibleDistance(note, request.recipient, request.position, now()) ?: return ReceiveResult.INELIGIBLE
            if (received.value.any { it.note.noteId == note.noteId && it.recipientId == request.recipient.actorId }) return ReceiveResult.ALREADY_RECEIVED
            received.value += InboxItem(note, request.recipient.actorId, now(), distance, null)
            return ReceiveResult.RECEIVED
        }
        override suspend fun confirm(noteId: String, recipientId: String) {
            received.value = received.value.map { item ->
                if (item.note.noteId == noteId && item.recipientId == recipientId && item.displayedAtUtcMillis == null)
                    item.copy(displayedAtUtcMillis = now()) else item
            }
        }
        override suspend fun reset(): String {
            resetGate.await()
            check(!failReset) { "reset" }
            val selected = active.value
            val deleted = originals.value.filter { selected != null && it.scenarioId == selected }.map { it.noteId }.toSet()
            originals.value = originals.value.filter { it.noteId !in deleted }
            received.value = received.value.filter { it.note.noteId !in deleted }
            return id(generation++).also { active.value = it }
        }
    }
    private class Source : LocationSource {
        lateinit var location: (LocationFix?, Long) -> Unit
        lateinit var error: (Exception) -> Unit
        val enters = mutableListOf<() -> Unit>()
        var stops = 0; var registrations = 0
        override fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean {
            location = onLocation; error = onError; return true
        }
        override fun stop() { stops++ }
        override fun addProximity(latitude: Double, longitude: Double, radiusMeters: Float, onRegistered: (String) -> Unit,
                                  onEnter: (String) -> Unit, onError: (Exception) -> Unit) {
            val key = "alert-${++registrations}"
            enters += { onEnter(key) }
            onRegistered(key)
        }
        override fun removeProximity(alertId: String) = Unit
    }
    private class Input : DemoInputSource {
        var last = 0.0
        override suspend fun prepare() = Unit
        override suspend fun readMotion() = 36.0 to "Drive"
        override fun inject(progress: Double) { last = progress }
        override fun close() = Unit
    }
    private inner class Harness(val test: TestScope) {
        fun elapsed() = test.testScheduler.currentTime
        fun utc() = 1_000 + elapsed()
        val store = Store(::utc)
        val input = Input()
        val sources = mutableListOf<Source>()
        var setup = SyntheticSession()
        val actor = MutableStateFlow("A")
        val policy = MutableStateFlow(UiRestrictions(connected = true))
        var stopDecisions = 0
        lateinit var demo: DemoRuntime
        val receipts: NoteReceiptProcessor = NoteReceiptProcessor(test.backgroundScope, store, actor, { finish() })
        val tracking: LocationTracking = LocationTracking(test.backgroundScope, { Source().also { sources += it } }, {}, {
            demo.onSourceFailure(); pickup.stop(); finish()
        }, ::elapsed, { demo.accept(it) })
        val submissions: RoadNoteSubmission = RoadNoteSubmission(test.backgroundScope, store, tracking::selectAnchor, ::utc, { finish() },
            setup = { setup }, scenarioId = { store.active.value })
        val spatial = object : SpatialIndex {
            override fun cell(latitude: Double, longitude: Double) = "fixture"
            override fun cover(latitude: Double, longitude: Double) = setOf("fixture")
        }
        val candidates: NearbyNoteCandidates = NearbyNoteCandidates(test.backgroundScope, tracking.currentPosition, spatial, store, ::elapsed, ::utc)
        val pickup: ProximityPickup = ProximityPickup(test.backgroundScope, tracking.currentPosition, candidates.state, candidates::currentCandidates,
            receipts, tracking::ownsSource, ::elapsed, ::utc, scenarioId = { store.active.value })
        init {
            demo = DemoRuntime(test.backgroundScope, object : DemoServiceContract {
                override val busy get() = submissions.isSubmitting || receipts.hasPendingWrites
                override fun startTracking() {
                    tracking.start()
                    tracking.proximitySource?.let { pickup.start(tracking.sessionId, it, setup) }
                }
                override fun stopTracking() { pickup.stop(); tracking.stop(); finish() }
                override fun changeActor(actor: String) {
                    setup = SyntheticSession(actor); this@Harness.actor.value = actor
                    receipts.refreshObservation(); submissions.clearResult()
                }
                override fun currentFix() = tracking.currentPosition.value?.fix
                override fun holdStorage() = Unit
                override fun writesFinished() { finish() }
            }, store, store.active, policy, ::elapsed, ::utc, input)
        }
        fun finish() {
            if (shouldStopRoadNoteService(tracking.status.value, submissions.state.value, receipts.hasPendingWrites)) stopDecisions++
        }
        fun emit(progress: Double) {
            val fix = DemoGeometry.fix(progress, elapsed())
            sources.last().location(fix, fix.receivedElapsedMillis)
            test.runCurrent()
        }
        fun ready() {
            demo.requestStart(); test.runCurrent()
            assertEquals(DemoPhase.PROBE, demo.state.value.phase)
            assertNull(tracking.currentPosition.value)
            emit(input.last); assertNull(tracking.currentPosition.value)
            emit(input.last)
            assertEquals(DemoPhase.READY, demo.state.value.phase)
            assertEquals(TrackingStatus.RECEIVING, tracking.status.value)
        }
        fun drive(seconds: Int) {
            repeat(seconds) { test.advanceTimeBy(1_000); test.runCurrent(); emit(input.last) }
        }
        fun confirmPending() {
            val pending = receipts.inbox.value.pending!!
            val key = ReceiptKey(pending.note.noteId, pending.recipientId)
            receipts.confirmVisibleReceipt(key, true, policy.value, setOf("receipt:${key.recipientId}/${key.noteId}"))
            test.runCurrent()
        }
    }

    @Test fun repeatedRoleChangesClearMemoryBufferButPreserveStoredNotesAndReceipts() = runTest {
        val h = Harness(this)
        h.demo.reset(true); runCurrent(); h.ready(); h.drive(35)
        h.submissions.submit("구덩이", NoteCategory.WARNING); runCurrent()
        assertTrue(h.submissions.state.value is SubmissionState.Saved)
        val originals = h.store.originals.value.toList()
        val receipts = h.store.received.value.toList()
        val oldSource = h.sources.last()
        assertTrue(h.tracking.bufferSnapshot().sampleCount > 0)
        for (actor in listOf("B", "A", "B")) {
            h.demo.role(actor); runCurrent()
            oldSource.location(DemoGeometry.fix(960.0, h.elapsed()), h.elapsed()); runCurrent()
            assertEquals(0, h.tracking.bufferSnapshot().sampleCount)
            assertNull(h.tracking.currentPosition.value)
            assertEquals(originals, h.store.originals.value)
            assertEquals(receipts, h.store.received.value)
        }
    }

    @Test fun sdkGatedAnchorThroughCandidatePickupExplicitAckAndScopedResetPreservesHistory() = runTest {
        val h = Harness(this)
        h.demo.reset(true); runCurrent(); h.ready(); h.drive(35)
        h.submissions.submit("구덩이", NoteCategory.WARNING); runCurrent()
        val note = (h.submissions.state.value as SubmissionState.Saved).note
        assertEquals(30_000L, note.anchorOffsetMillis)
        assertEquals(660.0, note.route!!.progressMeters, 1e-5)
        val oldSource = h.sources.last()
        h.demo.role("B"); runCurrent()
        oldSource.location(DemoGeometry.fix(960.0, h.elapsed()), h.elapsed()); runCurrent()
        assertNull(h.tracking.currentPosition.value)
        h.ready()
        assertEquals(note.route.progressMeters - 600, h.demo.state.value.target!!, 1e-5)
        val b = h.sources.last()
        val activeScenario = h.store.active.value
        h.policy.value = UiRestrictions(true, true, android.car.drivingstate.CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP)
        runCurrent()
        h.demo.role("A"); h.demo.reset(true); runCurrent()
        assertEquals("B", h.actor.value)
        assertEquals(activeScenario, h.store.active.value)
        assertEquals(DemoPhase.READY, h.demo.state.value.phase)
        assertEquals(1, b.registrations)
        assertEquals(1, b.registrations)
        h.drive(9)
        assertTrue(h.store.received.value.none { it.note.noteId == note.noteId })
        h.drive(2)
        val newRows = { h.store.received.value.filter { it.note.noteId == note.noteId } }
        assertEquals(1, newRows().size)
        b.enters.forEach { it() }; runCurrent()
        assertEquals(1, newRows().size)
        assertNull(newRows().single().displayedAtUtcMillis)
        assertEquals(h.store.legacy.noteId, h.receipts.inbox.value.pending!!.note.noteId)
        h.confirmPending(); h.confirmPending()
        assertNull(h.receipts.inbox.value.pending)
        assertTrue(h.store.originals.value.contains(note))
        h.policy.value = UiRestrictions(connected = true); runCurrent()
        val preserved = h.store.received.value.filter { it.note.noteId != note.noteId }
        val scenario = h.store.active.value
        h.store.failReset = true; h.demo.reset(true); runCurrent()
        assertEquals(DemoPhase.ERROR, h.demo.state.value.phase)
        assertEquals(scenario, h.store.active.value)
        assertTrue(h.store.originals.value.contains(note))
        h.store.failReset = false; h.store.resetGate = CompletableDeferred()
        h.demo.reset(true); runCurrent()
        assertTrue(h.demo.hasPendingWrites)
        h.demo.role("B"); h.demo.stop(); runCurrent()
        h.store.resetGate.complete(Unit); runCurrent()
        assertEquals(DemoPhase.STOPPED, h.demo.state.value.phase)
        assertEquals("A", h.actor.value)
        assertNotEquals(scenario, h.store.active.value)
        assertFalse(h.store.originals.value.contains(note))
        assertEquals(listOf(h.store.legacy, h.store.other), h.store.originals.value)
        assertEquals(preserved, h.store.received.value)
    }

    @Test fun acceptedSubmissionSurvivesSdkFailureAndBlocksResetUntilSuccessOrFailureIsSettled() = runTest {
        for (failure in listOf(false, true)) {
            val h = Harness(this)
            h.demo.reset(true); runCurrent(); h.ready()
            h.store.createGate = CompletableDeferred(); h.store.failCreate = failure
            h.submissions.submit("구덩이", NoteCategory.WARNING); runCurrent()
            val scenario = h.store.active.value
            h.demo.role("B"); h.demo.reset(true); runCurrent()
            assertEquals("A", h.actor.value); assertEquals(scenario, h.store.active.value)
            val old = h.sources.last()
            val before = h.stopDecisions
            old.error(IllegalStateException("SDK")); runCurrent()
            assertEquals(TrackingStatus.FAILED, h.tracking.status.value)
            assertEquals(DemoPhase.ERROR, h.demo.state.value.phase)
            assertEquals(before, h.stopDecisions)
            assertEquals(1, old.stops)
            old.location(DemoGeometry.fix(900.0, h.elapsed()), h.elapsed()); runCurrent()
            assertNull(h.tracking.currentPosition.value)
            h.store.createGate.complete(Unit); runCurrent()
            assertEquals(before + 1, h.stopDecisions)
            if (failure) assertEquals(SubmissionState.Failed(SubmissionFailure.STORAGE), h.submissions.state.value)
            else {
                val saved = (h.submissions.state.value as SubmissionState.Saved).note
                assertEquals("A", saved.authorId); assertEquals(scenario, saved.scenarioId)
            }
            h.submissions.submit("오류 뒤 신규", NoteCategory.WARNING); runCurrent()
            assertEquals(SubmissionState.Failed(SubmissionFailure.LOCATION_UNAVAILABLE), h.submissions.state.value)
            assertEquals(1, h.store.creates)
        }
    }
}
