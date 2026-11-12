package com.jinkim.hyundaiAndGretel.roadnote

import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class DraftCipher(private val key: (create: Boolean) -> SecretKey) {
    /** 새 IV로 AES-GCM 암호화한 뒤 형식 헤더·IV·암호문/인증 태그를 이어 붙인다. */
    fun encrypt(plain: ByteArray): ByteArray = crypt {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true))
        require(cipher.iv.size == IV_SIZE)
        // 헤더는 암호화하지 않지만 인증 대상에 포함해 형식 정보의 변조도 감지한다.
        cipher.updateAAD(HEADER)
        HEADER + cipher.iv + cipher.doFinal(plain)
    }

    /** 헤더·길이를 확인하고 저장된 IV와 키로 복호화한다. 변조된 데이터는 인증 검증에서 거절한다. */
    fun decrypt(encoded: ByteArray): ByteArray = crypt {
        if (encoded.size < HEADER.size + IV_SIZE + 16 ||
            !encoded.copyOfRange(0, HEADER.size).contentEquals(HEADER)
        ) throw IOException("Invalid encrypted draft format")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = encoded.copyOfRange(HEADER.size, HEADER.size + IV_SIZE)
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, iv))
        // 헤더는 암호화하지 않지만 인증 대상에 포함해 형식 정보의 변조도 감지한다.
        cipher.updateAAD(HEADER)
        cipher.doFinal(encoded, HEADER.size + IV_SIZE, encoded.size - HEADER.size - IV_SIZE)
    }

    /** 암호 연산 오류를 민감한 내부 정보를 담지 않는 저장소 IOException으로 변환한다. */
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
