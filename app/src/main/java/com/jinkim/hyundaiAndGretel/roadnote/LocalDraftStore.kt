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

private fun encryptedStore(context: Context, now: () -> Instant): DraftStore {
    val cipher = DraftCipher { create ->
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        existing ?: if (create) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build())
            }.generateKey()
        } else throw IOException("Local draft key is unavailable")
    }
    return DraftFileStore(
        File(context.noBackupFilesDir, "road-note-drafts.bin"), now,
        encode = cipher::encrypt, decode = cipher::decrypt,
    )
}

private const val KEY_ALIAS = "road-note-drafts-v1"
