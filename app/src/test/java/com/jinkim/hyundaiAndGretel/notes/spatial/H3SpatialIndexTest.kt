package com.jinkim.hyundaiAndGretel.notes.spatial

import com.uber.h3core.H3Core
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class H3SpatialIndexTest {
    private val h3 = H3Core.newInstance()
    private val index = H3SpatialIndex { h3 }

    @Test fun realH3ResolutionRoundTripAndDomainLimits() {
        val cell = index.cell(37.4979, 127.0276)
        assertEquals(7, h3.getResolution(cell))
        assertTrue(h3.isValidCell(cell))
        assertEquals(cell, index.cell(h3.cellToLatLng(cell).lat, h3.cellToLatLng(cell).lng))
        assertTrue(CandidateArea.contains(37.49, 127.02))
        assertTrue(CandidateArea.contains(37.51, 127.05))
        assertFalse(CandidateArea.contains(37.489999, 127.02))
        assertNotNull(index.cell(35.0, 129.0))
        assertThrows(IllegalArgumentException::class.java) { index.cell(Double.NaN, 127.0) }
        assertThrows(IllegalArgumentException::class.java) { index.cover(35.0, 129.0) }
    }

    @Test fun gridEdgesVerticesAndCircleBoundariesAreCoveredInDefinedArea() {
        val centers = mutableSetOf<Pair<Double, Double>>()
        for (y in 0..10) for (x in 0..12) centers += ((37_490_000 + y * 2_000) / 1_000_000.0) to ((127_020_000 + x * 2_500) / 1_000_000.0)
        val region = listOf(com.uber.h3core.util.LatLng(37.49,127.02), com.uber.h3core.util.LatLng(37.49,127.05),
            com.uber.h3core.util.LatLng(37.51,127.05), com.uber.h3core.util.LatLng(37.51,127.02))
        for (cell in h3.polygonToCells(region, emptyList(), 7)) {
            val edges = h3.cellToBoundary(cell)
            for (i in edges.indices) {
                val vertex = edges[i]
                val next = edges[(i + 1) % edges.size]
                for ((lat, lon) in listOf(vertex.lat to vertex.lng, (vertex.lat + next.lat) / 2 to (vertex.lng + next.lng) / 2)) {
                    for (delta in listOf(-1e-8, 0.0, 1e-8)) {
                        if (CandidateArea.contains(lat + delta, lon + delta)) centers += (lat + delta) to (lon + delta)
                    }
                }
            }
        }
        assertTrue(centers.size >= 143)
        for ((lat, lon) in centers) {
            val cover = index.cover(lat, lon)
            assertTrue(cover.size <= CandidateArea.MAX_QUERY_CELLS)
            for (bearing in 0 until 360 step 15) for (radius in listOf(0.0, 500.0, 3_000.0, 5_999.99, 6_000.0, 6_000.01)) {
                val (pointLat, pointLon) = destination(lat, lon, radius, bearing.toDouble())
                assertTrue(pointLat in 37.43..37.57 && pointLon in 126.95..127.12)
                assertEquals(radius, distanceMeters(lat, lon, pointLat, pointLon), 1e-6)
                if (radius <= 6_000.0) assertTrue("Missing boundary cell", index.cell(pointLat, pointLon) in cover)
            }
        }
    }

    @Test fun androidLoaderFailureIsExplicitAndNotRequiredForHostTests() {
        val broken = H3SpatialIndex { throw UnsatisfiedLinkError("test") }
        assertThrows(SpatialUnavailableException::class.java) { broken.cell(37.5, 127.03) }
    }

    private fun destination(latitude: Double, longitude: Double, meters: Double, bearing: Double): Pair<Double, Double> {
        val lat = Math.toRadians(latitude); val lon = Math.toRadians(longitude)
        val angle = meters / 6_371_000.0; val direction = Math.toRadians(bearing)
        val resultLat = asin(sin(lat) * cos(angle) + cos(lat) * sin(angle) * cos(direction))
        val resultLon = lon + atan2(sin(direction) * sin(angle) * cos(lat), cos(angle) - sin(lat) * sin(resultLat))
        return Math.toDegrees(resultLat) to Math.toDegrees(resultLon)
    }
}
