package com.jinkim.hyundaiAndGretel.notes.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 리뷰용: DAO는 Room이 SQL 실행 코드를 생성하는 내부 인터페이스다. 화면이나 기능 처리자가 직접 사용하지 않는다.
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

// 리뷰용: 원본·수신 이력·시연 상태를 같은 DB에 둔다. 현재는 미출시 개발 데이터를 초기화하는 새 버전 1 기준이다.
@Database(entities = [RoadNoteEntity::class, ReceiptEntity::class, DemoStateEntity::class], version = 1, exportSchema = true)
internal abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): RoadNoteDao
    abstract fun receipts(): ReceiptDao
    abstract fun scenarios(): ScenarioDao

    companion object {
        @Volatile private var instance: NoteDatabase? = null

        // 리뷰용: 앱 프로세스 안에서 하나의 Room 인스턴스를 재사용한다. Activity 대신 애플리케이션 문맥만 보관한다.
        fun getInstance(context: Context): NoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, NoteDatabase::class.java, "road-notes.db",
            ).build().also { instance = it }
        }
    }
}
