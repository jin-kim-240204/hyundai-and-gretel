package com.jinkim.hyundaiAndGretel.ui.location

import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.location.LocationSession
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationConnectionTest {
    private class Session(status: TrackingStatus, submission: SubmissionState) : LocationSession {
        override val status = MutableStateFlow(status)
        override val submissionState = MutableStateFlow(submission)
        val calls = mutableListOf<Pair<String, NoteCategory>>()
        override fun submitTestNote(content: String, category: NoteCategory) {
            calls += content to category
            submissionState.value = SubmissionState.Submitting
        }
    }

    @Test fun disconnectAndReplacementRejectQueuedOldStateAndOldClickTarget() = runTest {
        val a = Session(TrackingStatus.RECEIVING, SubmissionState.Saved(note()))
        val b = Session(TrackingStatus.STOPPED, SubmissionState.Idle)
        val connection = LocationConnection(backgroundScope)
        val observed = mutableListOf<LocationUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { connection.state.collect { observed += it } }
        connection.connect(a)
        runCurrent()
        assertEquals(LocationUiState(a.status.value, a.submissionState.value), connection.state.value)
        backgroundScope.launch { yield(); a.status.value = TrackingStatus.FAILED }
        connection.connect(null)
        connection.submitTestNote("차단", NoteCategory.WARNING)
        runCurrent()
        assertEquals(LocationUiState(), connection.state.value)
        connection.connect(b)
        // Even before the UI collector catches up, commands use the current session.
        connection.submitTestNote("차단", NoteCategory.WARNING)
        runCurrent()
        a.submissionState.value = SubmissionState.Failed(SubmissionFailure.STORAGE)
        runCurrent()
        assertEquals(LocationUiState(TrackingStatus.STOPPED, SubmissionState.Idle), connection.state.value)
        assertTrue(observed.all { it in setOf(LocationUiState(),
            LocationUiState(TrackingStatus.RECEIVING, SubmissionState.Saved(note())),
            LocationUiState(TrackingStatus.STOPPED, SubmissionState.Idle)) })
        assertTrue(a.calls.isEmpty())
        assertTrue(b.calls.isEmpty())
        b.status.value = TrackingStatus.RECEIVING
        connection.submitTestNote("도로에 구덩이가 있어요", NoteCategory.WARNING)
        connection.submitTestNote("연타", NoteCategory.INFORMATION)
        runCurrent()
        assertTrue(a.calls.isEmpty())
        assertEquals(listOf("도로에 구덩이가 있어요" to NoteCategory.WARNING), b.calls)
    }

    @Test fun immediateReplacementWithOrWithoutNullUsesOnlyNewSession() = runTest {
        for (disconnectFirst in listOf(false, true)) {
            val a = Session(TrackingStatus.RECEIVING, SubmissionState.Saved(note()))
            val b = Session(TrackingStatus.STOPPED, SubmissionState.Idle)
            val connection = LocationConnection(backgroundScope)
            val observed = mutableListOf<LocationUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                connection.state.collect { observed += it }
            }
            connection.connect(a)
            runCurrent()
            val displayedA = LocationUiState(a.status.value, a.submissionState.value)
            assertEquals(displayedA, connection.state.value)
            observed.clear()
            backgroundScope.launch {
                yield()
                a.status.value = TrackingStatus.FAILED
                a.submissionState.value = SubmissionState.Failed(SubmissionFailure.STORAGE)
            }
            if (disconnectFirst) connection.connect(null)
            connection.connect(b)
            // Do not drain the scheduler between A, optional null, and B.
            assertEquals(displayedA, connection.state.value)
            connection.submitTestNote("차단", NoteCategory.WARNING)
            assertTrue(a.calls.isEmpty())
            assertTrue(b.calls.isEmpty())
            runCurrent()
            val stoppedB = LocationUiState(TrackingStatus.STOPPED, SubmissionState.Idle)
            assertEquals(stoppedB, connection.state.value)
            assertTrue(observed.all { it == stoppedB || it == LocationUiState() })
            // A fast null may be conflated; its emission is not required.
            a.status.value = TrackingStatus.RECEIVING
            a.submissionState.value = SubmissionState.Idle
            runCurrent()
            assertEquals(stoppedB, connection.state.value)
            b.status.value = TrackingStatus.RECEIVING
            connection.submitTestNote("도로에 구덩이가 있어요", NoteCategory.WARNING)
            runCurrent()
            assertTrue(a.calls.isEmpty())
            assertEquals(listOf("도로에 구덩이가 있어요" to NoteCategory.WARNING), b.calls)
            assertEquals(LocationUiState(TrackingStatus.RECEIVING, SubmissionState.Submitting), connection.state.value)
        }
    }

    @Test fun reconnectObservesCurrentResultWithoutResubmittingAndNewServiceStartsIdle() = runTest {
        val session = Session(TrackingStatus.RECEIVING, SubmissionState.Submitting)
        val connection = LocationConnection(backgroundScope)
        connection.connect(session)
        runCurrent()
        assertEquals(SubmissionState.Submitting, connection.state.value.submission)
        connection.connect(null)
        runCurrent()
        session.submissionState.value = SubmissionState.Saved(note())
        connection.connect(session)
        runCurrent()
        assertEquals(SubmissionState.Saved(note()), connection.state.value.submission)
        assertTrue(session.calls.isEmpty())
        connection.connect(Session(TrackingStatus.STOPPED, SubmissionState.Idle))
        runCurrent()
        assertEquals(SubmissionState.Idle, connection.state.value.submission)
    }

    @Test fun buttonMatrixDisablesBothWhileSavingAndOnlyAllowsManualRestartAfterwards() {
        val live = setOf(TrackingStatus.STARTING, TrackingStatus.WAITING, TrackingStatus.RECEIVING, TrackingStatus.UNAVAILABLE)
        val startable = setOf(TrackingStatus.STOPPED, TrackingStatus.FAILED)
        val submissions = listOf(null, SubmissionState.Idle, SubmissionState.Submitting,
            SubmissionState.Saved(note()), SubmissionState.Failed(SubmissionFailure.STORAGE))
        for (status in TrackingStatus.entries) for (submission in submissions) {
            val state = LocationUiState(status, submission)
            assertEquals(status.name, status in live && submission != null && submission != SubmissionState.Submitting, state.canSubmit)
            assertEquals(status.name, status in startable && submission != SubmissionState.Submitting, state.canStart)
        }
        for (status in listOf(TrackingStatus.STOPPED, TrackingStatus.FAILED)) {
            assertFalse(LocationUiState(status, SubmissionState.Submitting).canStart)
            assertTrue(LocationUiState(status, SubmissionState.Saved(note())).canStart)
            assertTrue(LocationUiState(status, SubmissionState.Failed(SubmissionFailure.STORAGE)).canStart)
        }
    }

    @Test fun debugMessagesCoverAllResultsWithoutExposingOriginalText() {
        assertEquals(DebugSubmissionMessage.CONNECTING, null.debugMessage())
        assertEquals(DebugSubmissionMessage.READY, SubmissionState.Idle.debugMessage())
        assertEquals(DebugSubmissionMessage.SAVING, SubmissionState.Submitting.debugMessage())
        assertEquals(DebugSubmissionMessage.SAVED, SubmissionState.Saved(note()).debugMessage())
        val expected = mapOf(
            SubmissionFailure.LOCATION_UNAVAILABLE to DebugSubmissionMessage.LOCATION_UNAVAILABLE,
            SubmissionFailure.INVALID_TIME to DebugSubmissionMessage.INVALID_TIME,
            SubmissionFailure.INVALID_INPUT to DebugSubmissionMessage.INVALID_INPUT,
            SubmissionFailure.DUPLICATE to DebugSubmissionMessage.DUPLICATE,
            SubmissionFailure.STORAGE to DebugSubmissionMessage.STORAGE,
            SubmissionFailure.CANCELLED to DebugSubmissionMessage.CANCELLED,
        )
        assertEquals(SubmissionFailure.entries.toSet(), expected.keys)
        expected.forEach { (failure, message) -> assertEquals(message, SubmissionState.Failed(failure).debugMessage()) }
    }

    private fun note() = RoadNote("00000000-0000-0000-0000-000000000001", "A", "비공개 내용",
        NoteCategory.WARNING, 37.5, 127.0, 0, 1_000, 601_000)
}
