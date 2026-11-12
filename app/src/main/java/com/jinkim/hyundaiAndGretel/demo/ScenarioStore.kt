package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

interface ScenarioStore {
    fun observeActive(): Flow<String?>
    fun observeNotes(scenarioId: String): Flow<List<RoadNote>>
    /** 현재 시나리오만 삭제하고 새 시나리오 선택까지 한 트랜잭션으로 처리한다. 소속 없는 기존 데이터는 유지한다. */
    suspend fun reset(): String
}
