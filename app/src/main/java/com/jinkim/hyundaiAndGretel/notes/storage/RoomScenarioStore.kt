package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.*
import com.jinkim.hyundaiAndGretel.demo.ScenarioStore
import com.jinkim.hyundaiAndGretel.notes.validationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

@Entity(tableName = "demo_state")
internal data class DemoStateEntity(@PrimaryKey val id: Int = 1, val activeScenarioId: String)

@Dao
internal interface ScenarioDao {
    @Query("SELECT activeScenarioId FROM demo_state WHERE id = 1")
    fun observeActive(): Flow<String?>
    @Query("SELECT activeScenarioId FROM demo_state WHERE id = 1")
    suspend fun active(): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun select(state: DemoStateEntity)
    @Query("DELETE FROM road_notes WHERE scenarioId = :scenarioId")
    suspend fun deleteScenario(scenarioId: String)
    @Query("SELECT * FROM road_notes WHERE scenarioId = :scenarioId ORDER BY createdAtUtcMillis DESC, noteId")
    fun notes(scenarioId: String): Flow<List<RoadNoteEntity>>
}

class RoomScenarioStore internal constructor(private val db: NoteDatabase) : ScenarioStore {
    constructor(context: Context) : this(NoteDatabase.getInstance(context))
    override fun observeActive() = db.scenarios().observeActive()
    override fun observeNotes(scenarioId: String) = db.scenarios().notes(scenarioId).map { rows ->
        rows.map { it.toNote().also { note -> require(note.validationError() == null) } }
    }
    override suspend fun reset(): String = db.withTransaction {
        val dao = db.scenarios()
        val next = UUID.randomUUID().toString()
        dao.active()?.let { dao.deleteScenario(it) }
        dao.select(DemoStateEntity(activeScenarioId = next))
        next
    }
}
