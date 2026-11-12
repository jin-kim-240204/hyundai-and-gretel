package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.spatial.*
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoadNotePickupFlowTest {
    @Test fun submissionAnchorToCurrentCandidateToApproachToExplicitConfirmation() = runTest {
        val originals = MutableStateFlow<List<RoadNote>>(emptyList())
        val received = MutableStateFlow<List<InboxItem>>(emptyList())
        val notes = object : RoadNoteStore, NearbyNoteStore {
            override suspend fun create(note: RoadNote): CreateNoteResult {
                originals.value += note
                return CreateNoteResult.Created(note)
            }
            override suspend fun findById(noteId: String) = originals.value.find { it.noteId == noteId }
            override fun observeNearby(cells: Set<String>) = originals
        }
        val receiptStore = object : ReceiptStore {
            override fun observeInbox(recipientId: String) = received
            override suspend fun receive(request: ReceiptRequest): ReceiveResult {
                val note = notes.findById(request.noteId) ?: return ReceiveResult.INELIGIBLE
                val distance = eligibleDistance(note, request.recipient, request.position, 1_000) ?: return ReceiveResult.INELIGIBLE
                if (received.value.any { it.note.noteId == note.noteId && it.recipientId == request.recipient.actorId })
                    return ReceiveResult.ALREADY_RECEIVED
                received.value += InboxItem(note, request.recipient.actorId, 1_000, distance, null)
                return ReceiveResult.RECEIVED
            }
            override suspend fun confirm(noteId: String, recipientId: String) {
                received.value = received.value.map { if (it.note.noteId == noteId && it.recipientId == recipientId) it.copy(displayedAtUtcMillis = 2_000) else it }
            }
        }
        val submissions = RoadNoteSubmission(backgroundScope, notes, {
            AnchorSelection.Selected(LocationAnchor(LocationFix(37.5, 127.03, 0), 30_000))
        }, { 1_000 }, {})
        submissions.submit("구덩이", NoteCategory.WARNING)
        runCurrent()
        val note = (submissions.state.value as SubmissionState.Saved).note
        assertEquals(30_000L, note.anchorOffsetMillis)
        assertNotNull(note.route)
        val positions = MutableStateFlow<TrackedPosition?>(TrackedPosition(1, LocationFix(37.5, 127.024, 0)))
        val index = object : SpatialIndex {
            override fun cell(latitude: Double, longitude: Double) = "fixture"
            override fun cover(latitude: Double, longitude: Double) = setOf("fixture")
        }
        val candidates = NearbyNoteCandidates(backgroundScope, positions, index, notes, { 0 }, { 1_000 })
        val receipts = NoteReceiptProcessor(backgroundScope, receiptStore, MutableStateFlow("B"), {})
        var registeredCallback: ((String) -> Unit)? = null
        var enterCallback: ((String) -> Unit)? = null
        var removes = 0
        val source = object : ProximitySource {
            override fun addProximity(latitude: Double, longitude: Double, onRegistered: (String) -> Unit,
                                      onEnter: (String) -> Unit, onError: (Exception) -> Unit) {
                registeredCallback = onRegistered
                enterCallback = onEnter
            }
            override fun removeProximity(alertId: String) { removes++ }
        }
        val pickup = ProximityPickup(backgroundScope, positions, candidates.state, candidates::currentCandidates,
            receipts, { it === source }, { 0 }, { 1_000 })
        pickup.start(1, source, SyntheticSession("B")); runCurrent()
        assertNull(registeredCallback) // More than 400m away.
        positions.value = TrackedPosition(1, LocationFix(37.5, 127.0265, 0)); runCurrent()
        registeredCallback!!("note"); runCurrent()
        assertTrue(received.value.isEmpty()) // Registered outside the receipt radius.
        positions.value = TrackedPosition(1, LocationFix(37.5, 127.0285, 0)); runCurrent()
        assertEquals(1, received.value.size) // Current-position inside supplement, without ENTER.
        enterCallback!!("note"); runCurrent()
        assertEquals(1, received.value.size)
        assertEquals(1, removes)
        assertNull(received.value.single().displayedAtUtcMillis)
        receipts.confirm(ReceiptKey(note.noteId, "B"), true); runCurrent()
        assertEquals(2_000L, received.value.single().displayedAtUtcMillis)
        assertNull(receipts.inbox.value.pending)
        assertEquals(1, originals.value.size)
    }
}
