package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

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

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE road_notes ADD COLUMN receiverGuidance TEXT")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rebuild both tables to preserve receipts with foreign keys enabled on older SQLite versions.
        db.execSQL("CREATE TEMP TABLE receipt_backup AS SELECT noteId, recipientId, receivedAtUtcMillis, distanceMeters, displayedAtUtcMillis FROM note_receipts")
        db.execSQL("DROP TABLE note_receipts")
        db.execSQL("CREATE TABLE IF NOT EXISTS `road_notes_new` (`noteId` TEXT NOT NULL, `authorId` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `anchorOffsetMillis` INTEGER NOT NULL, `createdAtUtcMillis` INTEGER NOT NULL, `expiresAtUtcMillis` INTEGER NOT NULL, `h3CellId` TEXT, `routeId` TEXT, `progressMeters` REAL, `scenarioId` TEXT, `receiverGuidance` TEXT, PRIMARY KEY(`noteId`))")
        db.execSQL("INSERT INTO road_notes_new (noteId, authorId, content, category, latitude, longitude, anchorOffsetMillis, createdAtUtcMillis, expiresAtUtcMillis, h3CellId, routeId, progressMeters, scenarioId, receiverGuidance) SELECT noteId, authorId, content, category, latitude, longitude, anchorOffsetMillis, createdAtUtcMillis, expiresAtUtcMillis, h3CellId, routeId, progressMeters, scenarioId, receiverGuidance FROM road_notes")
        db.execSQL("DROP TABLE road_notes")
        db.execSQL("ALTER TABLE road_notes_new RENAME TO road_notes")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_road_notes_h3CellId` ON `road_notes` (`h3CellId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_road_notes_scenarioId_h3CellId` ON `road_notes` (`scenarioId`, `h3CellId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `note_receipts` (`noteId` TEXT NOT NULL, `recipientId` TEXT NOT NULL, `receivedAtUtcMillis` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `displayedAtUtcMillis` INTEGER, PRIMARY KEY(`noteId`, `recipientId`), FOREIGN KEY(`noteId`) REFERENCES `road_notes`(`noteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("INSERT INTO note_receipts (noteId, recipientId, receivedAtUtcMillis, distanceMeters, displayedAtUtcMillis) SELECT noteId, recipientId, receivedAtUtcMillis, distanceMeters, displayedAtUtcMillis FROM receipt_backup")
        db.execSQL("DROP TABLE receipt_backup")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_receipts_recipientId_displayedAtUtcMillis_receivedAtUtcMillis_noteId` ON `note_receipts` (`recipientId`, `displayedAtUtcMillis`, `receivedAtUtcMillis`, `noteId`)")
    }
}

@Database(entities = [RoadNoteEntity::class, ReceiptEntity::class, DemoStateEntity::class], version = 3, exportSchema = true)
internal abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): RoadNoteDao
    abstract fun receipts(): ReceiptDao
    abstract fun scenarios(): ScenarioDao

    companion object {
        @Volatile private var instance: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, NoteDatabase::class.java, "road-notes.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
