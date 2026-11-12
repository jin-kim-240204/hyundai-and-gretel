package com.jinkim.hyundaiAndGretel.gleo

import com.jinkim.hyundaiAndGretel.roadnote.DraftCipher
import com.jinkim.hyundaiAndGretel.roadnote.DraftFileStore
import com.jinkim.hyundaiAndGretel.roadnote.DraftState
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteInput
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.crypto.KeyGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DraftCipherIntegrationTest {
    @get:Rule val folder = TemporaryFolder()
    private val now = Instant.parse("2026-11-08T03:00:00Z")
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = DraftCipher { key }

    @Test fun savedFileIsEncryptedAndReopensWithSameKey() = runTest {
        val file = File(folder.root, "drafts.bin")
        val store = store(file)
        store.setConsent(true)
        store.save(RoadNoteInput("private report content", LocationMode.NAMED_PLACE), now)
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("private report content"))
        val restored = store(file).list().single()
        assertEquals("private report content", restored.input.content)
        assertEquals(DraftState.AWAITING_LOCATION, restored.state)
        assertFalse(File(folder.root, "drafts.bin.tmp").exists())
    }

    @Test fun cipherUsesFreshIvAndDetectsTampering() {
        val payload = "secret".toByteArray()
        val first = cipher.encrypt(payload)
        assertFalse(first.contentEquals(cipher.encrypt(payload)))
        first[first.lastIndex] = (first.last().toInt() xor 1).toByte()
        try {
            cipher.decrypt(first)
            fail("Tampering must fail")
        } catch (_: IOException) { }
    }

    @Test fun missingKeyDoesNotSilentlyResetSavedData() = runTest {
        val file = File(folder.root, "drafts.bin")
        store(file).setConsent(true)
        val original = file.readBytes()
        val unavailable = DraftCipher { throw IOException("No key") }
        try {
            DraftFileStore(file, { now }, encode = unavailable::encrypt, decode = unavailable::decrypt).list()
            fail("Missing key must surface")
        } catch (_: IOException) {
            assertArrayEquals(original, file.readBytes())
        }
    }

    private fun store(file: File) = DraftFileStore(file, { now }, encode = cipher::encrypt, decode = cipher::decrypt)
}
