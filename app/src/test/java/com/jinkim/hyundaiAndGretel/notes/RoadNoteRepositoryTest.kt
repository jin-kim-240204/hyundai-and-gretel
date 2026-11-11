package com.jinkim.hyundaiAndGretel.notes

import com.jinkim.hyundaiAndGretel.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoadNoteRepositoryTest {
    private fun anchor(offset: Long = 10_000) = AnchorSelection.Selected(
        LocationAnchor(LocationFix(37.5, 127.0, 100_000 - offset), 100_000),
    )
    private class Store(val action: suspend (RoadNote) -> CreateNoteResult) : RoadNoteStore {
        val inputs = mutableListOf<RoadNote>()
        override suspend fun create(note: RoadNote): CreateNoteResult {
            inputs += note
            return action(note)
        }
        override suspend fun findById(noteId: String): RoadNote? = null
    }

    @Test fun freezesAnchorTimeAndOneRequestBeforeAsyncWrite() = runTest {
        val release = CompletableDeferred<Unit>()
        val store = Store { release.await(); CreateNoteResult.Created(it) }
        var selection: AnchorSelection = anchor()
        var utc = 1_000L
        var finished = 0
        val repo = RoadNoteRepository(this, store, { selection }, { utc }, { finished++ })
        repo.submit("구덩이", NoteCategory.WARNING)
        repo.submit("연타", NoteCategory.INFORMATION)
        assertEquals(SubmissionState.Submitting, repo.state.value)
        selection = AnchorSelection.Unavailable.EMPTY
        utc = 99_000
        runCurrent()
        assertEquals(1, store.inputs.size)
        val original = store.inputs.single()
        assertEquals(10_000, original.anchorOffsetMillis)
        assertEquals(1_000, original.createdAtUtcMillis)
        assertEquals(601_000, original.expiresAtUtcMillis)
        assertEquals(37.5, original.latitude, 0.0)
        // Simulates tracking stop/reset after acceptance; it must not cancel the write.
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(SubmissionState.Saved(original), repo.state.value)
        assertEquals(1, finished)
    }

    @Test fun noAnchorOrOverflowNeverReachesStorage() = runTest {
        val store = Store { error("Must not write") }
        var selection: AnchorSelection = AnchorSelection.Unavailable.STALE
        val repo = RoadNoteRepository(this, store, { selection }, { Long.MAX_VALUE }, {})
        repo.submit("구덩이", NoteCategory.WARNING)
        assertEquals(SubmissionState.Failed(SubmissionFailure.LOCATION_UNAVAILABLE), repo.state.value)
        selection = anchor()
        repo.submit("구덩이", NoteCategory.WARNING)
        assertEquals(SubmissionState.Failed(SubmissionFailure.INVALID_TIME), repo.state.value)
        advanceUntilIdle()
        assertTrue(store.inputs.isEmpty())
    }

    @Test fun failureIsTerminalAndNewExplicitSubmissionGetsNewId() = runTest {
        val store = Store { CreateNoteResult.StorageFailure }
        val repo = RoadNoteRepository(this, store, { anchor(0) }, { 1_000 }, {})
        repo.submit("구덩이", NoteCategory.WARNING)
        advanceUntilIdle()
        assertEquals(SubmissionState.Failed(SubmissionFailure.STORAGE), repo.state.value)
        assertEquals(1, store.inputs.size)
        repo.submit("구덩이", NoteCategory.WARNING)
        advanceUntilIdle()
        assertEquals(2, store.inputs.size)
        assertNotEquals(store.inputs[0].noteId, store.inputs[1].noteId)
        assertEquals(0, store.inputs[0].anchorOffsetMillis)
    }

    @Test fun rejectionAndThrownFailureAlwaysFinishWithoutSaved() = runTest {
        for (result in listOf(CreateNoteResult.Duplicate, CreateNoteResult.Invalid(InvalidNote.CONTENT))) {
            var finished = false
            val repo = RoadNoteRepository(this, Store { result }, { anchor() }, { 1_000 }, { finished = true })
            repo.submit("구덩이", NoteCategory.WARNING)
            advanceUntilIdle()
            assertTrue(repo.state.value is SubmissionState.Failed)
            assertTrue(finished)
        }
        var finished = false
        val repo = RoadNoteRepository(this, Store { throw IllegalStateException("test") },
            { anchor() }, { 1_000 }, { finished = true })
        repo.submit("구덩이", NoteCategory.WARNING)
        advanceUntilIdle()
        assertEquals(SubmissionState.Failed(SubmissionFailure.STORAGE), repo.state.value)
        assertTrue(finished)
    }

    @Test fun cancellationPropagatesAndReleasesSubmissionState() = runTest {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))
        var finished = false
        val repo = RoadNoteRepository(scope, Store { awaitCancellation() }, { anchor() }, { 1_000 }, { finished = true })
        repo.submit("구덩이", NoteCategory.WARNING)
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertEquals(SubmissionState.Failed(SubmissionFailure.CANCELLED), repo.state.value)
        assertTrue(finished)
    }
}
