package com.jinkim.hyundaiAndGretel.notes.pickup

import com.jinkim.hyundaiAndGretel.location.LocationFix
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.spatial.distanceMeters
import kotlinx.coroutines.flow.Flow

const val PICKUP_RADIUS_METERS = 500.0

data class ReceiptRequest(val noteId: String, val recipient: SyntheticSession, val position: LocationFix, val scenarioId: String? = null)
data class InboxItem(
    val note: RoadNote,
    val recipientId: String,
    val receivedAtUtcMillis: Long,
    val distanceMeters: Double,
    val displayedAtUtcMillis: Long?,
)
enum class ReceiveResult { RECEIVED, ALREADY_RECEIVED, INELIGIBLE, STORAGE_FAILURE }

interface ReceiptStore {
    fun observeInbox(recipientId: String): Flow<List<InboxItem>>
    suspend fun receive(request: ReceiptRequest): ReceiveResult
    suspend fun confirm(noteId: String, recipientId: String)
}

fun eligibleDistance(note: RoadNote, setup: SyntheticSession, position: LocationFix, nowUtc: Long): Double? {
    if (note.authorId == setup.actorId || !note.isActiveAt(nowUtc)) return null
    val route = note.route ?: return null
    if (!SyntheticRoute.matches(route, note.latitude, note.longitude)) return null
    val current = SyntheticRoute.project(position.latitude, position.longitude, setup) ?: return null
    val ahead = SyntheticRoute.ahead(route, current) ?: return null
    if (ahead !in 0.0..PICKUP_RADIUS_METERS) return null
    return distanceMeters(position.latitude, position.longitude, note.latitude, note.longitude).takeIf { it <= PICKUP_RADIUS_METERS }
}
