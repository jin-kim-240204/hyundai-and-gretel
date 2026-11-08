package com.jinkim.hyundaiAndGretel.roadnote

import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalDraftStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private var clock = Instant.parse("2026-11-08T00:00:00Z")
    private val file: File get() = File(folder.root, "drafts.bin")
    private fun store() = DraftFileStore(file, { clock })
    private fun input(mode: LocationMode = LocationMode.DEFERRED) = RoadNoteInput("도로 낙하물", mode)

    @Test fun consentRecoveryClearAndRevocation() = runBlocking {
        val store = store()
        assertFalse(store.hasConsent())
        assertThrows(IllegalStateException::class.java) { runBlocking { store.save(input(), clock) } }
        assertFalse(file.exists())
        store.setConsent(true)
        val draft = store.save(input(), clock)
        assertEquals(listOf(draft), store().list())
        assertTrue(store().hasConsent())
        store.clear()
        assertTrue(store.hasConsent())
        assertTrue(store.list().isEmpty())
        store.save(input(), clock)
        store.setConsent(false)
        assertFalse(store().hasConsent())
        assertTrue(store().list().isEmpty())
    }

    @Test fun updatesPreserveOriginalTimesAndResolveAllModes() = runBlocking {
        val store = store()
        store.setConsent(true)
        val original = store.save(input().copy(utteranceRecordedAt = clock.minusSeconds(3)), clock)
        clock = clock.plusSeconds(100)
        LocationMode.entries.forEach { mode ->
            val updated = store.save(input(mode).copy(draftId = original.id,
                utteranceRecordedAt = clock), clock)
            assertEquals(original.id, updated.id)
            assertEquals(original.id, updated.input.draftId)
            assertEquals(original.receivedAt, updated.receivedAt)
            assertEquals(original.input.utteranceRecordedAt, updated.input.utteranceRecordedAt)
            assertEquals(clock, updated.updatedAt)
            assertEquals(if (mode in listOf(LocationMode.CURRENT_ROAD, LocationMode.NAMED_PLACE))
                DraftState.PENDING_LOCATION else DraftState.AWAITING_LOCATION, updated.state)
        }
        val nullTime = store.save(input(), clock)
        assertNull(store.save(input().copy(draftId = nullTime.id,
            utteranceRecordedAt = clock), clock).input.utteranceRecordedAt)
        assertEquals(2, store.list().size)
    }

    @Test fun expirationBoundaryDoesNotExtendOnUpdate(): Unit = runBlocking {
        val store = store()
        store.setConsent(true)
        val original = store.save(input(), clock)
        clock = clock.plus(Duration.ofDays(7)).minusNanos(1)
        assertEquals(1, store.list().size)
        store.save(input().copy(draftId = original.id), clock)
        clock = clock.plusNanos(1)
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            store.save(input().copy(draftId = original.id), clock)
        } }
        assertTrue(store().list().isEmpty())
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            store.save(input(), clock.minus(Duration.ofDays(7)))
        } }
    }

    @Test fun rejectsUnknownIdAndMaintainsSortAndDelete() = runBlocking {
        val store = store()
        store.setConsent(true)
        val first = store.save(input(), clock)
        clock = clock.plusSeconds(1)
        val second = store.save(input(), clock)
        assertEquals(listOf(second, first), store.list())
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            store.save(input().copy(draftId = "missing"), clock)
        } }
        assertTrue(store.delete(first.id))
        assertFalse(store.delete(first.id))
        assertEquals(listOf(second), store().list())
    }

    @Test fun failedReplacementPreservesDataAndConsent() = runBlocking {
        val store = store()
        store.setConsent(true)
        store.save(input(), clock)
        val bytes = file.readBytes()
        val broken = DraftFileStore(file, { clock }) { _, _ -> throw IOException("Injected failure") }
        assertThrows(IOException::class.java) { runBlocking { broken.save(input(), clock) } }
        assertArrayEquals(bytes, file.readBytes())
        assertThrows(IOException::class.java) { runBlocking { broken.setConsent(false) } }
        assertArrayEquals(bytes, file.readBytes())
        assertTrue(store().hasConsent())
        assertEquals(1, store().list().size)
        assertFalse(File(folder.root, "drafts.bin.tmp").exists())
    }

    @Test fun corruptionIsNotSilentlyOverwrittenAndRevocationCanEraseIt() = runBlocking {
        file.writeText("broken")
        val bytes = file.readBytes()
        assertThrows(IOException::class.java) { runBlocking { store().list() } }
        assertThrows(IOException::class.java) { runBlocking { store().save(input(), clock) } }
        assertThrows(IOException::class.java) { runBlocking { store().setConsent(true) } }
        assertArrayEquals(bytes, file.readBytes())
        store().setConsent(false)
        assertFalse(store().hasConsent())
        assertTrue(store().list().isEmpty())
    }

    @Test fun capacityFailsWithoutEvictionAndExpiredEntriesFreeSpace() = runBlocking {
        val store = store()
        store.setConsent(true)
        repeat(100) { store.save(input(), clock) }
        val originals = store.list()
        assertThrows(IllegalStateException::class.java) { runBlocking { store.save(input(), clock) } }
        assertEquals(originals, store.list())
        store.save(input().copy(draftId = originals.first().id), clock)
        assertEquals(100, store.list().size)
        clock = clock.plus(Duration.ofDays(7))
        val newDraft = store.save(input(), clock)
        assertEquals(listOf(newDraft), store.list())
    }

    @Test fun multipleInstancesDoNotLoseConcurrentSavesAndDeletes() = runBlocking {
        store().setConsent(true)
        val saved = coroutineScope {
            (1..50).map { async { store().save(input(), clock) } }.awaitAll()
        }
        assertEquals(saved.toSet(), store().list().toSet())
        coroutineScope {
            val deletes = saved.take(25).map { draft -> async { assertTrue(store().delete(draft.id)) } }
            val additions = (1..25).map { async { store().save(input(), clock) } }
            deletes.awaitAll()
            additions.awaitAll()
        }
        assertEquals(50, store().list().size)
        assertTrue(store().list().none { it.id in saved.take(25).map { draft -> draft.id } })
        coroutineScope {
            val saves = (1..20).map { async {
                try { store().save(input(), clock) } catch (_: IllegalStateException) { null }
            } }
            val revoke = async { store().setConsent(false) }
            saves.awaitAll()
            revoke.await()
        }
        assertFalse(store().hasConsent())
        assertTrue(store().list().isEmpty())
    }

    @Test fun abandonedTemporaryFileIsRemovedWithoutChangingCommittedData() = runBlocking {
        store().setConsent(true)
        val draft = store().save(input(), clock)
        val temp = File(folder.root, "drafts.bin.tmp")
        temp.writeText("uncommitted data")
        assertEquals(listOf(draft), store().list())
        assertFalse(temp.exists())
        assertTrue(file.delete())
        temp.writeText("uncommitted consent")
        assertFalse(store().hasConsent())
        assertFalse(temp.exists())
    }

    @Test fun serializationFailureKeepsExistingFile() = runBlocking {
        store().setConsent(true)
        val bytes = file.readBytes()
        assertThrows(IOException::class.java) { runBlocking {
            store().save(input().copy(content = "a".repeat(70_000)), clock)
        } }
        assertArrayEquals(bytes, file.readBytes())
    }
}
