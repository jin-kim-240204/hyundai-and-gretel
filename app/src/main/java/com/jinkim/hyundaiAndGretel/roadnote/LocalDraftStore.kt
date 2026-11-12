package com.jinkim.hyundaiAndGretel.roadnote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.time.Instant
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class LocalDraftStore(context: Context, now: () -> Instant = Instant::now) : DraftStore by
    encryptedStore(context.applicationContext, now)

/** 백업 제외 경로의 파일 저장소에 Keystore 기반 암호화·복호화를 연결한다. */
private fun encryptedStore(context: Context, now: () -> Instant): DraftStore {
    val cipher = DraftCipher(::draftKey)
    return DraftFileStore(
        File(context.noBackupFilesDir, "road-note-drafts.bin"), now,
        encode = cipher::encrypt, decode = cipher::decrypt,
    )
}

/**
 * 기존 Keystore 키를 반환하고, 허용된 경우에만 새 암호키를 만든다.
 * 복호화할 키가 없을 때 새 키로 대체하지 않아 기존 파일 손실을 감추지 않는다.
 */
private fun draftKey(create: Boolean): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing
    if (!create) throw IOException("Local draft key is unavailable")

    val parameters = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(parameters)
    return generator.generateKey()
}

private const val KEY_ALIAS = "road-note-drafts-v1"
