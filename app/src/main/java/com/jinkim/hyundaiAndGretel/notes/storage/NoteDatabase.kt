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

    @Query("SELECT * FROM road_notes WHERE scenarioId = :scenarioId AND (h3CellId IN (:cells) OR h3CellId IS NULL)")
    fun observeNearbyScoped(cells: List<String>, scenarioId: String): Flow<List<RoadNoteEntity>>

    @Query("SELECT * FROM road_notes WHERE noteId = :noteId")
    suspend fun findById(noteId: String): RoadNoteEntity?
}

@Database(entities = [RoadNoteEntity::class, ReceiptEntity::class, DemoStateEntity::class], version = 4, exportSchema = true)
internal abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): RoadNoteDao
    abstract fun receipts(): ReceiptDao
    abstract fun scenarios(): ScenarioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE road_notes ADD COLUMN h3CellId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_road_notes_h3CellId ON road_notes (h3CellId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE road_notes ADD COLUMN routeId TEXT")
                db.execSQL("ALTER TABLE road_notes ADD COLUMN direction INTEGER")
                db.execSQL("ALTER TABLE road_notes ADD COLUMN progressMeters REAL")
                db.execSQL("CREATE TABLE IF NOT EXISTS note_receipts (noteId TEXT NOT NULL, recipientId TEXT NOT NULL, receivedAtUtcMillis INTEGER NOT NULL, distanceMeters REAL NOT NULL, displayedAtUtcMillis INTEGER, PRIMARY KEY(noteId, recipientId), FOREIGN KEY(noteId) REFERENCES road_notes(noteId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_receipts_recipientId_displayedAtUtcMillis_receivedAtUtcMillis_noteId ON note_receipts (recipientId, displayedAtUtcMillis, receivedAtUtcMillis, noteId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE road_notes ADD COLUMN scenarioId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_road_notes_scenarioId_h3CellId ON road_notes (scenarioId, h3CellId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS demo_state (id INTEGER NOT NULL, activeScenarioId TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }

        @Volatile private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, NoteDatabase::class.java, "road-notes.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
