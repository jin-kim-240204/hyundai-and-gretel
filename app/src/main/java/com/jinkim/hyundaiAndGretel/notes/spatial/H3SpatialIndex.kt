package com.jinkim.hyundaiAndGretel.notes.spatial

import com.uber.h3core.H3Core
import com.uber.h3core.util.LatLng
import java.io.IOException
import kotlin.math.cos

class H3SpatialIndex(private val create: () -> H3Core = H3Core::newSystemInstance) : SpatialIndex {
    private val h3 by lazy {
        try {
            create()
        } catch (error: LinkageError) {
            throw SpatialUnavailableException(error)
        } catch (error: IOException) {
            throw SpatialUnavailableException(error)
        }
    }

    // 리뷰용: 저장 시 쪽지 좌표를 해상도 7의 H3 셀 ID로 바꾼다.
    override fun cell(latitude: Double, longitude: Double): String {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        return access { it.latLngToCellAddress(latitude, longitude, RESOLUTION) }
    }

    // 리뷰용: 조회 시 6km 주변을 덮는 셀 집합을 만든다. 실제 원형 거리 판정은 후보 처리자가 별도로 수행한다.
    override fun cover(latitude: Double, longitude: Double): Set<String> {
        require(CandidateArea.contains(latitude, longitude))
        // shortcut: 보수적인 평면 근사 조회 범위는 고정된 서울 프로토타입 구역에서만 검증했다. 구역 확장 시 다시 검증한다.
        val latitudeMargin = CandidateArea.RADIUS_METERS / 111_000.0
        val longitudeMargin = CandidateArea.RADIUS_METERS / (111_000.0 * cos(Math.toRadians(latitude)))
        val boundary = listOf(
            LatLng(latitude - latitudeMargin, longitude - longitudeMargin),
            LatLng(latitude - latitudeMargin, longitude + longitudeMargin),
            LatLng(latitude + latitudeMargin, longitude + longitudeMargin),
            LatLng(latitude + latitudeMargin, longitude - longitudeMargin),
        )
        val cells = access { core ->
            core.polygonToCells(boundary, emptyList(), RESOLUTION)
                .flatMap { core.gridDisk(it, 1) }.map(core::h3ToString).toMutableSet()
        }
        cells += cell(latitude, longitude)
        check(cells.size <= CandidateArea.MAX_QUERY_CELLS)
        return cells.toSortedSet()
    }

    private inline fun <T> access(operation: (H3Core) -> T): T = try {
        operation(h3)
    } catch (error: LinkageError) {
        throw SpatialUnavailableException(error)
    }

    companion object { const val RESOLUTION = 7 }
}
