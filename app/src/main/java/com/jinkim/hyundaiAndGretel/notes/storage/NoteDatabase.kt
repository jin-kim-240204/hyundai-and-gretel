package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Dao
internal interface RoadNoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: RoadNoteEntity)

    @Query("SELECT * FROM road_notes WHERE h3CellId IN (:cells) OR h3CellId IS NULL")
    fun observeNearby(cells: List<String>): Flow<List<RoadNoteEntity>>

    @Query("SELECT * FROM road_notes WHERE noteId = :noteId")
    suspend fun findById(noteId: String): RoadNoteEntity?
}

@Database(entities = [RoadNoteEntity::class], version = 2, exportSchema = true)
internal abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): RoadNoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE road_notes ADD COLUMN h3CellId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_road_notes_h3CellId ON road_notes (h3CellId)")
            }
        }

        @Volatile private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, NoteDatabase::class.java, "road-notes.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
