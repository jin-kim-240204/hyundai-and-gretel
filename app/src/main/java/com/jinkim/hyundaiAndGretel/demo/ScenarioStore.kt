package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

interface ScenarioStore {
    fun observeActive(): Flow<String?>
    fun observeNotes(scenarioId: String): Flow<List<RoadNote>>

    suspend fun reset(): String
}
