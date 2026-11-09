package com.jinkim.hyundaiAndGretel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.jinkim.hyundaiAndGretel.notes.*

@Entity(tableName = "road_notes", indices = [Index(value = ["requestId"], unique = true),
    Index(value = ["scenarioId", "h3CellId", "expiresAt"])])
data class NoteEntity(
    @PrimaryKey val id: String, val requestId: String, val scenarioId: String, val authorId: String,
    val content: String, val category: String, val receivedAt: Long, val createdAt: Long,
    val expiresAt: Long, val latitude: Double, val longitude: Double, val heading: Float?,
    val offsetMillis: Long, val h3CellId: String, val progressMeters: Double,
    val routeId: String, val direction: String,
    val h3Resolution: Int = 9, val timeBasis: String = "CALLBACK_RECEIVED",
    val source: String = "SDK", val isSynthetic: Boolean = true,
) {
    fun toDomain(): RoadNote {
        check(h3Resolution == 9 && timeBasis == "CALLBACK_RECEIVED" && source == "SDK" && isSynthetic)
        return RoadNote(id, requestId, scenarioId, authorId, content, Category.valueOf(category), receivedAt,
            createdAt, expiresAt, latitude, longitude, heading, offsetMillis, h3CellId, progressMeters, routeId, direction)
    }
    companion object {
        fun from(n: RoadNote) = NoteEntity(n.id, n.requestId, n.scenarioId, n.authorId, n.content,
            n.category.name, n.receivedAt, n.createdAt, n.expiresAt, n.latitude, n.longitude, n.heading,
            n.offsetMillis, n.h3CellId, n.progressMeters, n.routeId, n.direction)
    }
}

@Entity(tableName = "note_receipts", primaryKeys = ["noteId", "recipientId"],
    foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["recipientId", "displayedAt", "receivedAt"])])
data class ReceiptEntity(val noteId: String, val recipientId: String, val receivedAt: Long,
                         val distanceMeters: Double, val displayedAt: Long? = null)

@Dao
internal interface NoteDao {
    @Insert suspend fun insert(note: NoteEntity)
    @Query("SELECT * FROM road_notes WHERE requestId=:requestId") suspend fun byRequest(requestId: String): NoteEntity?
    @Query("SELECT * FROM road_notes WHERE id=:id") suspend fun byId(id: String): NoteEntity?
    @Query("SELECT * FROM road_notes WHERE scenarioId=:scenario AND h3CellId IN (:cells)")
    fun nearby(scenario: String, cells: List<String>): Flow<List<NoteEntity>>
    @Query("DELETE FROM road_notes WHERE scenarioId!=:active OR expiresAt<=:now") suspend fun clean(active: String, now: Long)
    @Query("DELETE FROM road_notes WHERE scenarioId=:scenario") suspend fun deleteScenario(scenario: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReceipt(receipt: ReceiptEntity): Long
    @Query("SELECT COUNT(*) FROM note_receipts WHERE noteId=:id AND recipientId=:recipient") suspend fun receiptCount(id: String, recipient: String): Int
    @Query("SELECT note_receipts.* FROM note_receipts INNER JOIN road_notes ON road_notes.id=noteId WHERE road_notes.scenarioId=:scenario AND recipientId=:recipient ORDER BY receivedAt, noteId")
    fun receipts(scenario: String, recipient: String): Flow<List<ReceiptEntity>>
    @Query("UPDATE note_receipts SET displayedAt=:at WHERE noteId=:id AND recipientId=:recipient AND displayedAt IS NULL")
    suspend fun markDisplayed(id: String, recipient: String, at: Long)
    @Query("SELECT COUNT(*) FROM road_notes WHERE scenarioId=:scenario") suspend fun noteCount(scenario: String): Int
}

@Database(entities = [NoteEntity::class, ReceiptEntity::class], version = 1, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    internal abstract fun notes(): NoteDao
}
