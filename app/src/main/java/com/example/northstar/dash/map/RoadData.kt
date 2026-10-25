package com.example.northstar.dash.map

import com.example.northstar.dash.nav.GeoPoint
import kotlin.math.abs
import kotlin.math.roundToInt

data class RoadFeature(val id: Long, val point: GeoPoint, val camera: Boolean)
data class RoadWay(val id: Long, val points: List<GeoPoint>, val tags: Map<String, String>)
data class RoadSnapshot(
    val center: GeoPoint? = null,
    val fetchedAt: Long = 0,
    val features: List<RoadFeature> = emptyList(),
    val ways: List<RoadWay> = emptyList(),
    val status: String = "Aguardando posição · dados OSM",
) {
    fun covers(point: GeoPoint, now: Long) = center?.let {
        now - fetchedAt in 0..86_400_000L && GeoPoint.distMeters(it, point) < 1800
    } == true
}

/** Conservative matching: unknown is preferable to a limit from an adjacent road. */
object RoadMatcher {
    fun parseSpeed(raw: String?): Int? {
        val match = Regex("^([0-9]{1,3})(?:\\s*(km/h|mph))?$").matchEntire(raw?.trim()?.lowercase() ?: "") ?: return null
        val value = match.groupValues[1].toInt()
        if (value !in 1..200) return null
        return if (match.groupValues[2] == "mph") (value * 1.609344).roundToInt() else value
    }
    private fun angle(a: Double, b: Double) = abs((a - b + 540) % 360 - 180)
    fun limit(ways: List<RoadWay>, point: GeoPoint, accuracy: Float, bearing: Float?, ageMs: Long): Int? {
        if (!accuracy.isFinite() || accuracy !in 0f..20f || ageMs !in 0..10_000L) return null
        data class Candidate(val distance: Double, val speed: Int?)
        val candidates = ways.mapNotNull { way ->
            val nearest = way.points.zipWithNext().map { (a,b) ->
                Triple(GeoPoint.distMeters(point, GeoPoint.projectOnSegment(point,a,b).first), a, b)
            }.minByOrNull { it.first } ?: return@mapNotNull null
            if (nearest.first > 20) return@mapNotNull null
            val forward = bearing?.let { angle(it.toDouble(), GeoPoint.bearing(nearest.second, nearest.third)) }
            if (forward != null && forward > 35 && forward < 145) return@mapNotNull null
            val tags = way.tags
            val unsupported = tags.keys.any { it.startsWith("maxspeed") && (it.contains("conditional") || it.contains("lanes")) }
            val directional = tags.containsKey("maxspeed:forward") || tags.containsKey("maxspeed:backward")
            val raw = if (directional) when {
                forward == null -> null
                forward <= 35 -> tags["maxspeed:forward"] ?: tags["maxspeed"]
                else -> tags["maxspeed:backward"] ?: tags["maxspeed"]
            } else tags["maxspeed"]
            Candidate(nearest.first, if (unsupported) null else parseSpeed(raw))
        }.sortedBy { it.distance }
        val best = candidates.firstOrNull() ?: return null
        // Unknown limits on nearby roads also make the result ambiguous.
        if (candidates.drop(1).any { it.distance <= best.distance + maxOf(8.0, accuracy.toDouble()) && it.speed != best.speed }) return null
        return best.speed
    }
}
