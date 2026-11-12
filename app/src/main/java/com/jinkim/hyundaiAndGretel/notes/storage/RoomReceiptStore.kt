package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.*
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.validationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import java.util.UUID

// 리뷰용: 원본과 사용자별 수신 이력을 JOIN한 조회 결과다. 원본 본문을 이력 테이블에 중복 저장하지 않는다.
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

    // 리뷰용: 수신자 전체 이력을 수신시각·쪽지 ID 순서로 조회한다. 현재 시연 소속만으로 제품 수신함을 자르지 않는다.
    @Query("SELECT road_notes.*, note_receipts.recipientId, note_receipts.receivedAtUtcMillis, note_receipts.distanceMeters, note_receipts.displayedAtUtcMillis FROM note_receipts INNER JOIN road_notes ON road_notes.noteId = note_receipts.noteId WHERE recipientId = :recipient ORDER BY receivedAtUtcMillis, road_notes.noteId")
    fun inbox(recipient: String): kotlinx.coroutines.flow.Flow<List<InboxRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM note_receipts WHERE noteId = :noteId AND recipientId = :recipient)")
    suspend fun exists(noteId: String, recipient: String): Boolean

    // 리뷰용: 아직 확인하지 않은 행만 갱신하여 재호출해도 최초 확인 시각을 유지한다.
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

    // 리뷰용: 한 트랜잭션 안에서 원본 재조회·자격 재검증·중복 확인·이력 삽입을 처리한다. 기능 판단과 DB 제약이 함께 중복을 방어한다.
    override suspend fun receive(request: ReceiptRequest): ReceiveResult = try {
        require(UUID.fromString(request.noteId).toString() == request.noteId)
        database.withTransaction {
            val note = database.notes().findById(request.noteId)?.toNote()
                ?: return@withTransaction ReceiveResult.INELIGIBLE
            require(note.validationError() == null)
            if (note.scenarioId != request.scenarioId) return@withTransaction ReceiveResult.INELIGIBLE
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
