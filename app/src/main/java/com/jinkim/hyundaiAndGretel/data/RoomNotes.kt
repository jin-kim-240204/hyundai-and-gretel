package com.jinkim.hyundaiAndGretel.data

import androidx.room.withTransaction
import com.jinkim.hyundaiAndGretel.notes.*
import kotlinx.coroutines.flow.map

class RoomNotes(private val db: NoteDatabase, private val spatial: SpatialIndex) : RoadNoteRepository {
    private val dao = db.notes()
    override suspend fun create(note: RoadNote): RoadNote = db.withTransaction {
        require(spatial.validCell(note.h3CellId, note.latitude, note.longitude))
        val previous = dao.byRequest(note.requestId)?.toDomain()
        if (previous != null) {
            check(previous == note) { "REQUEST_CONFLICT" }
            previous
        } else {
            dao.insert(NoteEntity.from(note))
            note
        }
    }
    override fun observeNearby(scenarioId: String, cells: List<String>) =
        dao.nearby(scenarioId, cells).map { rows -> rows.map(NoteEntity::toDomain) }

    fun observeScenario(scenario: String) = dao.scenarioNotes(scenario).map { rows -> rows.map(NoteEntity::toDomain) }
    suspend fun latest(scenario: String, now: Long) = dao.latest(scenario, now)?.toDomain()

    suspend fun clean(active: String, now: Long) = dao.clean(active, now)
    suspend fun deleteScenario(scenario: String) = dao.deleteScenario(scenario)
}

/** Local receipt state stays on device when shared notes move to a server. */
class ReceiptStore(private val db: NoteDatabase) {
    private val dao = db.notes()
    suspend fun record(id: String, scenario: String, actor: Actor, location: LocationSample, now: Long, elapsed: Long): Boolean = db.withTransaction {
        val note = dao.byId(id)?.toDomain() ?: return@withTransaction false
        if (note.scenarioId != scenario || !eligible(note, actor, location, now, elapsed)) return@withTransaction false
        dao.insertReceipt(ReceiptEntity(id, actor.id, now,
            DemoRoute.distance(location.latitude, location.longitude, note.latitude, note.longitude))) != -1L
    }
    fun observe(scenario: String, actor: Actor) = dao.receipts(scenario, actor.id).map { rows ->
        rows.mapNotNull { r -> dao.byId(r.noteId)?.toDomain()?.let { note -> InboxItem(note, r.recipientId, r.receivedAt) } }
    }
    suspend fun markDisplayed(id: String, actor: Actor, at: Long) = dao.markDisplayed(id, actor.id, at)
}
