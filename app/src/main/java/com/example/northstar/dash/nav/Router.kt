package com.example.northstar.dash.nav

import com.example.northstar.settings.L10n
import com.example.northstar.R
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches a road route from the public OSRM demo server. Called at planning time
 * (destination shared) while the phone still has internet — the result is cached
 * so riding can proceed offline. Driving profile suits the Himalayan fine.
 */
object Router {
    private const val TAG = "Router"
    private const val BASE = "https://router.project-osrm.org/route/v1/driving"
    private const val UA = "TerraNav/1.0 (personal motorcycle nav; single user)"

    suspend fun route(from: GeoPoint, to: GeoPoint): Route? = withContext(Dispatchers.IO) {
        val url = "$BASE/${from.lng},${from.lat};${to.lng},${to.lat}" +
                "?overview=full&geometries=polyline&steps=true&annotations=false"
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", UA)
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            conn.disconnect()
            parse(body)
        } catch (e: Exception) {
            Log.w(TAG, "route() failed: ${e.message}")
            null
        }
    }

    private fun parse(json: String): Route? {
        val root = JSONObject(json)
        if (root.optString("code") != "Ok") {
            Log.w(TAG, "OSRM code=${root.optString("code")}")
            return null
        }
        val routes = root.optJSONArray("routes") ?: return null
        if (routes.length() == 0) return null
        val r0 = routes.getJSONObject(0)

        val geometry = PolylineCodec.decode(r0.getString("geometry"))
        if (geometry.size < 2) return null

        // Cumulative distance at each vertex
        val cum = DoubleArray(geometry.size)
        for (i in 1 until geometry.size) {
            cum[i] = cum[i - 1] + GeoPoint.distMeters(geometry[i - 1], geometry[i])
        }

        // Maneuvers from steps
        val maneuvers = ArrayList<Maneuver>()
        val legs = r0.optJSONArray("legs")
        if (legs != null) {
            for (li in 0 until legs.length()) {
                val steps = legs.getJSONObject(li).optJSONArray("steps") ?: continue
                for (si in 0 until steps.length()) {
                    val step = steps.getJSONObject(si)
                    val man = step.optJSONObject("maneuver") ?: continue
                    val loc = man.optJSONArray("location") ?: continue
                    val p = GeoPoint(loc.getDouble(1), loc.getDouble(0))
                    val type = ManeuverType.fromOsrm(man.optString("type"), man.optString("modifier"))
                    val name = step.optString("name").ifBlank { "road" }
                    maneuvers.add(
                        Maneuver(
                            type = type,
                            instruction = buildInstruction(type, name),
                            location = p,
                            cumulativeMeters = nearestCumulative(p, geometry, cum),
                        )
                    )
                }
            }
        }

        return Route(
            geometry = geometry,
            maneuvers = maneuvers,
            totalMeters = r0.optDouble("distance", cum.last()),
            totalSeconds = r0.optDouble("duration", 0.0),
            cumulative = cum,
        )
    }

    private fun buildInstruction(type: ManeuverType, road: String): String = when (type) {
        ManeuverType.DEPART       -> L10n.text(R.string.ui_0286264429aa, road)
        ManeuverType.ARRIVE       -> L10n.text(R.string.ui_752fdaec403b)
        ManeuverType.TURN_LEFT    -> L10n.text(R.string.ui_6d33a3916980, road)
        ManeuverType.TURN_RIGHT   -> L10n.text(R.string.ui_57e343ca8a7a, road)
        ManeuverType.SLIGHT_LEFT  -> L10n.text(R.string.ui_f26820fcb381, road)
        ManeuverType.SLIGHT_RIGHT -> L10n.text(R.string.ui_1890e08dc21f, road)
        ManeuverType.SHARP_LEFT   -> L10n.text(R.string.ui_aba0086c3882, road)
        ManeuverType.SHARP_RIGHT  -> L10n.text(R.string.ui_578132a69223, road)
        ManeuverType.UTURN        -> L10n.text(R.string.ui_9908f9679cde)
        ManeuverType.ROUNDABOUT   -> L10n.text(R.string.ui_fe4aea1e6e00, road)
        ManeuverType.CONTINUE     -> L10n.text(R.string.ui_d594afbedac1, road)
    }

    /** Cumulative distance of the geometry vertex nearest to a maneuver location. */
    private fun nearestCumulative(p: GeoPoint, geom: List<GeoPoint>, cum: DoubleArray): Double {
        var best = 0.0
        var bestD = Double.MAX_VALUE
        for (i in geom.indices) {
            val d = GeoPoint.distMeters(p, geom[i])
            if (d < bestD) { bestD = d; best = cum[i] }
        }
        return best
    }
}
