package com.jinkim.hyundaiAndGretel.notes.spatial

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

interface NearbyNoteStore {
    /** Spatial rows, including legacy data; time validity is checked on application and consumption. */
    fun observeNearby(cells: Set<String>): Flow<List<RoadNote>>
}
