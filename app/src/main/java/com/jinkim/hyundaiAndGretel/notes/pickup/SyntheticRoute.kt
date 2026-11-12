package com.jinkim.hyundaiAndGretel.notes.pickup

import kotlin.math.abs
import kotlin.math.cos

/** 시연용 합성 설정이며 실제 차량 방향이나 정확도를 측정한 값이 아니다. */
data class SyntheticSession(val actorId: String = "A", val routeId: String = "R1", val direction: Int = 1) {
    init { require(actorId.isNotBlank() && direction in setOf(-1, 1)) }
}

data class RouteContext(val routeId: String, val direction: Int, val progressMeters: Double)

object SyntheticRoute {
    private const val RADIUS = 6_371_000.0
    private val scale = RADIUS * cos(Math.toRadians(37.5))

    // 리뷰용: 위경도를 시연 직선 경로상의 진행 거리로 바꾼다. 경로에서 벗어난 위치는 억지로 투영하지 않는다.
    fun project(latitude: Double, longitude: Double, setup: SyntheticSession): RouteContext? {
        if (setup.routeId != "R1" || !latitude.isFinite() || !longitude.isFinite()) return null
        if (longitude !in 127.02..127.05) return null
        val crossTrack = RADIUS * Math.toRadians(latitude - 37.5)
        if (abs(crossTrack) > 20.0) return null
        return RouteContext(setup.routeId, setup.direction, scale * Math.toRadians(longitude - 127.02))
    }

    fun matches(context: RouteContext, latitude: Double, longitude: Double): Boolean {
        if (context.direction !in setOf(-1, 1) || !context.progressMeters.isFinite()) return false
        val projected = project(latitude, longitude, SyntheticSession(routeId = context.routeId, direction = context.direction))
            ?: return false
        return abs(projected.progressMeters - context.progressMeters) <= 1e-6
    }

    // 리뷰용: 같은 경로·방향인 경우에만 전방 거리를 계산한다. 실제 차량 방위각을 측정하는 함수는 아니다.
    fun ahead(note: RouteContext, current: RouteContext): Double? =
        if (note.routeId == current.routeId && note.direction == current.direction)
            (note.progressMeters - current.progressMeters) * current.direction
        else null
}
