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

    override fun cell(latitude: Double, longitude: Double): String {
        require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
        return access { it.latLngToCellAddress(latitude, longitude, RESOLUTION) }
    }

    override fun cover(latitude: Double, longitude: Double): Set<String> {
        require(CandidateArea.contains(latitude, longitude))

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
