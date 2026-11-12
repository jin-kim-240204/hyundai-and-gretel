package com.jinkim.hyundaiAndGretel.demo

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

interface ScenarioStore {
    fun observeActive(): Flow<String?>
    fun observeNotes(scenarioId: String): Flow<List<RoadNote>>
    /** Deletes only the active scenario and atomically selects a fresh one. Legacy stays untouched. */
    suspend fun reset(): String
}
