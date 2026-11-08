package com.jinkim.hyundaiAndGretel.roadnote

import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class DraftCipher(private val key: (create: Boolean) -> SecretKey) {
    fun encrypt(plain: ByteArray): ByteArray = crypt {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true))
        require(cipher.iv.size == IV_SIZE)
        cipher.updateAAD(HEADER)
        HEADER + cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(encoded: ByteArray): ByteArray = crypt {
        if (encoded.size < HEADER.size + IV_SIZE + 16 ||
            !encoded.copyOfRange(0, HEADER.size).contentEquals(HEADER)
        ) throw IOException("Invalid encrypted draft format")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = encoded.copyOfRange(HEADER.size, HEADER.size + IV_SIZE)
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, iv))
        cipher.updateAAD(HEADER)
        cipher.doFinal(encoded, HEADER.size + IV_SIZE, encoded.size - HEADER.size - IV_SIZE)
    }

    private fun crypt(operation: () -> ByteArray): ByteArray = try {
        operation()
    } catch (_: GeneralSecurityException) {
        throw IOException("Cannot access encrypted local drafts")
    }

    private companion object {
        val HEADER = byteArrayOf(0x44, 0x52, 0x46, 0x01)
        const val IV_SIZE = 12
    }
}
