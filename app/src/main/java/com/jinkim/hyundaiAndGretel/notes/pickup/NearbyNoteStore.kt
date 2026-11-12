package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.notes.RoadNote
import kotlinx.coroutines.flow.Flow

interface NearbyNoteStore {

    fun observeNearby(cells: Set<String>): Flow<List<RoadNote>>
}
