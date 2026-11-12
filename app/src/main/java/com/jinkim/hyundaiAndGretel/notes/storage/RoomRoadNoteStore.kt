package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.jinkim.hyundaiAndGretel.notes.CreateNoteResult
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.RoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.validationError
import com.jinkim.hyundaiAndGretel.notes.spatial.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

class RoomRoadNoteStore internal constructor(
    private val database: NoteDatabase,
    private val spatial: SpatialIndex = H3SpatialIndex(),
    private val onIndexError: (Exception) -> Unit = {},
    private val scenario: StateFlow<String?>? = null,
) : RoadNoteStore, NearbyNoteStore {
    constructor(context: Context, spatial: SpatialIndex = H3SpatialIndex(), scenario: StateFlow<String?>? = null, onIndexError: (Exception) -> Unit = {}) :
        this(NoteDatabase.getInstance(context), spatial, onIndexError, scenario)

    override suspend fun create(note: RoadNote): CreateNoteResult {
        val normalized = note.copy(content = note.content.trim())
        normalized.validationError()?.let { return CreateNoteResult.Invalid(it) }
        return try {
            database.notes().insert(normalized.toEntity(spatial.cell(normalized.latitude, normalized.longitude)))
            CreateNoteResult.Created(normalized)
        } catch (error: SpatialUnavailableException) {
            onIndexError(error)
            CreateNoteResult.StorageFailure
        } catch (error: IllegalArgumentException) {
            onIndexError(error)
            CreateNoteResult.StorageFailure
        } catch (_: SQLiteConstraintException) {
            classifyConstraint(normalized.noteId)
        } catch (_: SQLiteException) {
            CreateNoteResult.StorageFailure
        }
    }

    // After a constraint error, an existing ID means Duplicate; revisit if adding other constraints.
    private suspend fun classifyConstraint(noteId: String): CreateNoteResult = try {
        if (database.notes().findById(noteId) != null) CreateNoteResult.Duplicate
        else CreateNoteResult.StorageFailure
    } catch (_: SQLiteException) {
        CreateNoteResult.StorageFailure
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNearby(cells: Set<String>): Flow<List<RoadNote>> {
        require(cells.isNotEmpty() && cells.size <= CandidateArea.MAX_QUERY_CELLS)
        val rows = scenario?.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else database.notes().observeNearbyScoped(cells.sorted(), id)
        } ?: database.notes().observeNearby(cells.sorted())
        return rows.map { rows ->
            rows.mapNotNull { row ->
                val note = row.toNote()
                require(note.validationError() == null)
                val actualCell = spatial.cell(note.latitude, note.longitude)
                require(row.h3CellId == null || row.h3CellId == actualCell)
                note.takeIf { actualCell in cells }
            }
        }
    }

    override suspend fun findById(noteId: String): RoadNote? = database.notes().findById(noteId)?.toNote()
}
