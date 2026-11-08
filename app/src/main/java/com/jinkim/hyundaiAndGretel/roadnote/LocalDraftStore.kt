package com.jinkim.hyundaiAndGretel.roadnote

import android.content.Context
import java.io.File
import java.time.Instant

class LocalDraftStore(context: Context, now: () -> Instant = Instant::now) : DraftStore by
    DraftFileStore(File(context.applicationContext.noBackupFilesDir, "road-note-drafts.bin"), now)
