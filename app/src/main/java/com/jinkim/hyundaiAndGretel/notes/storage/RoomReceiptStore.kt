package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.*
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.validationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import java.util.UUID

internal data class InboxRow(
    @Embedded val note: RoadNoteEntity,
    val recipientId: String,
    val receivedAtUtcMillis: Long,
    val distanceMeters: Double,
    val displayedAtUtcMillis: Long?,
) {
    fun toItem() = InboxItem(note.toNote().also { require(it.validationError() == null) }, recipientId,
        receivedAtUtcMillis, distanceMeters, displayedAtUtcMillis)
}

@Dao
internal interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: ReceiptEntity): Long

    @Query("SELECT road_notes.*, note_receipts.recipientId, note_receipts.receivedAtUtcMillis, note_receipts.distanceMeters, note_receipts.displayedAtUtcMillis FROM note_receipts INNER JOIN road_notes ON road_notes.noteId = note_receipts.noteId WHERE recipientId = :recipient ORDER BY receivedAtUtcMillis, road_notes.noteId")
    fun inbox(recipient: String): kotlinx.coroutines.flow.Flow<List<InboxRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM note_receipts WHERE noteId = :noteId AND recipientId = :recipient)")
    suspend fun exists(noteId: String, recipient: String): Boolean

    @Query("UPDATE note_receipts SET displayedAtUtcMillis = MAX(:at, receivedAtUtcMillis) WHERE noteId = :noteId AND recipientId = :recipient AND displayedAtUtcMillis IS NULL")
    suspend fun confirm(noteId: String, recipient: String, at: Long)
}

class RoomReceiptStore internal constructor(
    private val database: NoteDatabase,
    private val utcMillis: () -> Long,
    private val onError: (Exception) -> Unit = {},
) : ReceiptStore {
    constructor(context: Context, utcMillis: () -> Long = System::currentTimeMillis, onError: (Exception) -> Unit = {}) :
        this(NoteDatabase.getInstance(context), utcMillis, onError)

    override fun observeInbox(recipientId: String) = database.receipts().inbox(recipientId).map { rows -> rows.map { it.toItem() } }

    override suspend fun receive(request: ReceiptRequest): ReceiveResult = try {
        require(UUID.fromString(request.noteId).toString() == request.noteId)
        database.withTransaction {
            val note = database.notes().findById(request.noteId)?.toNote()
                ?: return@withTransaction ReceiveResult.INELIGIBLE
            require(note.validationError() == null)
            val now = utcMillis()
            val distance = eligibleDistance(note, request.recipient, request.position, now)
                ?: return@withTransaction ReceiveResult.INELIGIBLE
            if (database.receipts().exists(note.noteId, request.recipient.actorId)) return@withTransaction ReceiveResult.ALREADY_RECEIVED
            val inserted = database.receipts().insert(ReceiptEntity(note.noteId, request.recipient.actorId, now, distance))
            if (inserted == -1L) {
                check(database.receipts().exists(note.noteId, request.recipient.actorId))
                ReceiveResult.ALREADY_RECEIVED
            } else ReceiveResult.RECEIVED
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onError(error)
        ReceiveResult.STORAGE_FAILURE
    }

    override suspend fun confirm(noteId: String, recipientId: String) {
        require(recipientId.isNotBlank() && UUID.fromString(noteId).toString() == noteId)
        database.receipts().confirm(noteId, recipientId, utcMillis())
    }
}
