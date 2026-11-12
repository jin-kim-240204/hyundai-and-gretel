package com.jinkim.hyundaiAndGretel.location

import com.jinkim.hyundaiAndGretel.notes.*
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.service.shouldStopRoadNoteService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SubmissionSdkFailureTest {
    private class Source : LocationSource {
        lateinit var location: (LocationFix?, Long) -> Unit
        lateinit var error: (Exception) -> Unit
        var stops = 0
        override fun start(onLocation: (LocationFix?, Long) -> Unit, onError: (Exception) -> Unit): Boolean {
            location = onLocation; error = onError
            return true
        }
        override fun stop() { stops++ }
    }

    @Test fun sdkErrorStopsCollectionButAcceptedWriteFinishesBeforeServiceStopDecision() = runTest {
        for (succeed in listOf(true, false)) {
            val source = Source()
            val release = CompletableDeferred<Unit>()
            var writes = 0
            var stopRequests = 0
            lateinit var repository: RoadNoteSubmission
            lateinit var tracking: LocationTracking
            val finish = { if (shouldStopRoadNoteService(tracking.status.value, repository.state.value)) stopRequests++; Unit }
            tracking = LocationTracking(this, { source }, {}, finish, { 100_000 })
            val store = object : RoadNoteStore {
                override suspend fun create(note: RoadNote): CreateNoteResult {
                    writes++
                    release.await()
                    return if (succeed) CreateNoteResult.Created(note) else CreateNoteResult.StorageFailure
                }
                override suspend fun findById(noteId: String): RoadNote? = null
            }
            repository = RoadNoteSubmission(this, store, tracking::selectAnchor, { 1_000 }, finish)
            tracking.start()
            source.location(LocationFix(37.5, 127.0, 100_000), 100_000)
            runCurrent()
            repository.submit("구덩이", NoteCategory.WARNING)
            runCurrent()
            source.error(IllegalStateException("fake SDK error"))
            runCurrent()
            assertEquals(TrackingStatus.FAILED, tracking.status.value)
            assertEquals(1, source.stops)
            assertEquals(0, stopRequests)
            assertEquals(SubmissionState.Submitting, repository.state.value)
            repository.submit("오류 후 새 요청", NoteCategory.WARNING)
            runCurrent()
            assertEquals(1, writes)
            release.complete(Unit)
            runCurrent()
            if (succeed) assertTrue(repository.state.value is SubmissionState.Saved)
            else assertEquals(SubmissionState.Failed(SubmissionFailure.STORAGE), repository.state.value)
            assertEquals(1, stopRequests)
            repository.submit("종료 후 새 요청", NoteCategory.WARNING)
            runCurrent()
            assertEquals(SubmissionState.Failed(SubmissionFailure.LOCATION_UNAVAILABLE), repository.state.value)
            assertEquals(1, writes)
        }
    }
}
