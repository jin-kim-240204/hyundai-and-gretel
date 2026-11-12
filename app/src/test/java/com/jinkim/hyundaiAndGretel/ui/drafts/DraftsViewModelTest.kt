package com.jinkim.hyundaiAndGretel.ui.drafts

import androidx.lifecycle.ViewModelStore
import com.jinkim.hyundaiAndGretel.roadnote.DraftRepository
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeStore()
    private val repository = DraftRepository(store)
    private val viewModel = DraftsViewModel(repository)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun refreshShowsLoadingThenStoredDraftsAndRejectsDuplicateRequests() = runTest {
        viewModel.refresh()
        viewModel.refresh()
        assertTrue(viewModel.state.value.busy)

        runCurrent()

        assertEquals(DraftUiState(consentGranted = true, drafts = store.drafts), viewModel.state.value)
        assertEquals(1, store.consentReads)
    }

    @Test fun missingConsentDoesNotReadDraftContents() = runTest {
        store.consent = false
        viewModel.refresh()
        runCurrent()

        assertEquals(DraftUiState(), viewModel.state.value)
        assertEquals(0, store.listReads)
    }

    @Test fun failureKeepsVisibleDraftsAndRetryClearsTheError() = runTest {
        viewModel.refresh()
        runCurrent()
        val loaded = viewModel.state.value
        store.failure = IOException("private details")

        viewModel.refresh()
        runCurrent()
        assertEquals(loaded.copy(hasError = true), viewModel.state.value)

        store.failure = null
        viewModel.refresh()
        assertFalse(viewModel.state.value.hasError)
        runCurrent()
        assertEquals(loaded, viewModel.state.value)
    }

    @Test fun revokingConsentClearsStoredAndVisibleDrafts() = runTest {
        viewModel.refresh()
        runCurrent()
        viewModel.setConsent(false)
        runCurrent()

        assertFalse(store.consent)
        assertTrue(store.drafts.isEmpty())
        assertEquals(DraftUiState(), viewModel.state.value)
    }

    @Test fun deleteReloadsTheRemainingDrafts() = runTest {
        viewModel.delete("draft-1")
        runCurrent()

        assertTrue(store.drafts.isEmpty())
        assertEquals(DraftUiState(consentGranted = true), viewModel.state.value)
    }

    @Test fun repositoryPropagatesCancellationInsteadOfReturningFailure() = runTest {
        store.failure = CancellationException("cancelled")
        try {
            repository.load()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(0, store.listReads)
        }
    }

    @Test fun clearingViewModelCancelsTheRequestAndClearsLoadingWithoutAnError() = runTest {
        val owners = ViewModelStore()
        owners.put("drafts", viewModel)
        store.readGate = CompletableDeferred()
        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.state.value.busy)

        owners.clear()
        runCurrent()

        assertTrue(store.readCancelled)
        assertEquals(DraftUiState(), viewModel.state.value)
    }

    private class FakeStore : DraftStore {
        var consent = true
        var consentReads = 0
        var listReads = 0
        var failure: Exception? = null
        var readGate: CompletableDeferred<Unit>? = null
        var readCancelled = false
        private val now = Instant.parse("2026-11-11T00:00:00Z")
        var drafts = listOf(RoadNoteDraft(
            "draft-1", RoadNoteInput("synthetic report", LocationMode.CURRENT_ROAD),
            now, now, DraftState.PENDING_LOCATION,
        ))

        override suspend fun hasConsent(): Boolean {
            consentReads++
            failure?.let { error -> throw error }
            return consent
        }

        override suspend fun setConsent(granted: Boolean) {
            consent = granted
            if (!granted) drafts = emptyList()
        }

        override suspend fun list(): List<RoadNoteDraft> {
            listReads++
            try {
                readGate?.await()
            } catch (cancelled: CancellationException) {
                readCancelled = true
                throw cancelled
            }
            return drafts
        }

        override suspend fun delete(id: String): Boolean {
            val before = drafts.size
            drafts = drafts.filterNot { draft -> draft.id == id }
            return before != drafts.size
        }

        override suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft =
            error("The screen never creates drafts")

        override suspend fun clear() {
            drafts = emptyList()
        }
    }
}
