package com.jinkim.hyundaiAndGretel.notes

import kotlinx.coroutines.flow.Flow

enum class Category { WARNING, INFORMATION }
enum class Actor(val id: String) {
    A("8c23438a-a617-4ce5-87ad-294c7c8c7a01"),
    B("8c23438a-a617-4ce5-87ad-294c7c8c7a02")
}

data class RoadNote(
    val id: String, val requestId: String, val scenarioId: String, val authorId: String,
    val content: String, val category: Category, val receivedAt: Long, val createdAt: Long,
    val expiresAt: Long, val latitude: Double, val longitude: Double, val heading: Float?,
    val offsetMillis: Long, val h3CellId: String, val progressMeters: Double,
    val routeId: String = DemoRoute.ID, val direction: String = "FORWARD",
) {
    init {
        require(id.isNotBlank() && requestId.isNotBlank() && scenarioId.isNotBlank() && authorId.isNotBlank())
        require(content == content.trim() && content.length in 1..500)
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        require(heading == null || heading.isFinite() && heading >= 0f && heading < 360f)
        require(progressMeters.isFinite() && offsetMillis in 0L..35_000L)
        require(expiresAt > createdAt && h3CellId.isNotBlank())
        require(routeId == DemoRoute.ID && direction in setOf("FORWARD", "REVERSE"))
    }
}

interface RoadNoteRepository {
    suspend fun create(note: RoadNote): RoadNote
    fun observeNearby(scenarioId: String, cells: List<String>): Flow<List<RoadNote>>
}

data class InboxItem(val note: RoadNote, val recipientId: String, val receivedAt: Long)

fun distanceAhead(note: RoadNote, location: LocationSample): Double =
    (note.progressMeters - location.progressMeters) * (if (location.direction == "FORWARD") 1 else -1)

fun proximityCandidates(notes: List<RoadNote>, actor: Actor, location: LocationSample?,
                        received: Set<String>, now: Long, elapsed: Long): List<RoadNote> {
    if (actor != Actor.B || location == null || elapsed - location.elapsedMillis !in 0..5_000) return emptyList()
    return notes.filter {
        it.authorId != actor.id && it.id !in received && it.expiresAt > now &&
            it.routeId == DemoRoute.ID && it.direction == location.direction && distanceAhead(it, location) >= 0
    }.sortedBy { distanceAhead(it, location) }
}

fun eligible(note: RoadNote, actor: Actor, location: LocationSample, now: Long, elapsed: Long): Boolean =
    note.authorId != actor.id && note.expiresAt > now && elapsed - location.elapsedMillis in 0..5_000 &&
        note.routeId == DemoRoute.ID && note.direction == location.direction &&
        distanceAhead(note, location) in 0.0..150.0 &&
        DemoRoute.distance(location.latitude, location.longitude, note.latitude, note.longitude) <= 150
