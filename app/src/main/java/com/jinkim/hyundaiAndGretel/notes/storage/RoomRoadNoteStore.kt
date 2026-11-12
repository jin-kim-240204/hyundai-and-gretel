package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.jinkim.hyundaiAndGretel.notes.CreateNoteResult
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.RoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.pickup.NearbyNoteStore
import com.jinkim.hyundaiAndGretel.notes.spatial.*
import com.jinkim.hyundaiAndGretel.notes.validationError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class RoomRoadNoteStore internal constructor(
    private val database: NoteDatabase,
    private val spatial: SpatialIndex = H3SpatialIndex(),
    private val onIndexError: (Exception) -> Unit = {},
    private val scenario: StateFlow<String?>? = null,
) : RoadNoteStore, NearbyNoteStore {
    constructor(context: Context, spatial: SpatialIndex = H3SpatialIndex(), scenario: StateFlow<String?>? = null, onIndexError: (Exception) -> Unit = {}) :
        this(NoteDatabase.getInstance(context), spatial, onIndexError, scenario)

    // 리뷰용: 실제 원본 저장 구현: 입력 정리·검증 후 H3 셀을 계산해 삽입한다. DB 저장 성공 후에만 Created를 반환한다.
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

    // 제약 위반 후 같은 ID가 있으면 중복으로 분류한다. 다른 제약을 추가하면 이 분류도 재검토한다.
    private suspend fun classifyConstraint(noteId: String): CreateNoteResult = try {
        if (database.notes().findById(noteId) != null) CreateNoteResult.Duplicate
        else CreateNoteResult.StorageFailure
    } catch (_: SQLiteException) {
        CreateNoteResult.StorageFailure
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    // 리뷰용: DB가 셀 범위의 변경을 Flow로 전달한다. 활성 시나리오가 있으면 그 범위로 좁히고, 각 행의 H3 정보도 재검증한다.
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
