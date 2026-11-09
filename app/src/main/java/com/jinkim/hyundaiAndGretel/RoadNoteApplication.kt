package com.jinkim.hyundaiAndGretel

import android.app.Application
import androidx.room.Room
import com.jinkim.hyundaiAndGretel.data.*
import com.jinkim.hyundaiAndGretel.demo.DemoController
import kotlinx.coroutines.*

class RoadNoteApplication : Application() {
    val controller by lazy {
        val database = Room.databaseBuilder(this, NoteDatabase::class.java, "road-notes-demo.db").build()
        val spatial = SpatialIndex()
        DemoController(this, RoomNotes(database, spatial), ReceiptStore(database), spatial,
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }
}
