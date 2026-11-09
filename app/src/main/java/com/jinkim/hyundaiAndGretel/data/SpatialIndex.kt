package com.jinkim.hyundaiAndGretel.data

import com.uber.h3core.H3Core
import com.jinkim.hyundaiAndGretel.notes.DemoRoute
import kotlin.math.*

class SpatialIndex {
    private val h3 = H3Core.newSystemInstance()
    fun cell(lat: Double, lon: Double): String = h3.latLngToCellAddress(lat, lon, RESOLUTION)
    fun validCell(cell: String, lat: Double, lon: Double) = cell == cell(lat, lon)

    fun nearby(lat: Double, lon: Double): List<String> {
        // shortcut: planar bounding box is limited to the fixed Seoul demo route; use spherical cover for real roads.
        require(DemoRoute.progress(lat, lon) != null)
        val radius = 400.0
        val dLat = radius / 111_195
        val dLon = radius / (111_195 * cos(Math.toRadians(lat)))
        val ring = listOf(
            com.uber.h3core.util.LatLng(lat-dLat, lon-dLon),
            com.uber.h3core.util.LatLng(lat-dLat, lon+dLon),
            com.uber.h3core.util.LatLng(lat+dLat, lon+dLon),
            com.uber.h3core.util.LatLng(lat+dLat, lon-dLon),
        )
        // Center cover plus one neighbor ring retains cells crossing the query boundary.
        return h3.polygonToCells(ring, emptyList(), RESOLUTION)
            .flatMap { h3.gridDisk(it, 1) }.distinct().map(h3::h3ToString).sorted()
    }
    companion object { const val RESOLUTION = 9 }
}
