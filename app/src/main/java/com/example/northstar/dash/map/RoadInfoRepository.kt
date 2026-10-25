package com.example.northstar.dash.map

import com.example.northstar.settings.L10n
import com.example.northstar.R
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.example.northstar.dash.nav.GeoPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Small, cached regional requests; never called by the frame encoder. */
class RoadInfoRepository private constructor(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val cache = File(context.cacheDir, "terranav-roads.json")
    private val _state = MutableStateFlow(RoadSnapshot())
    val state = _state.asStateFlow()
    private val _limit = MutableStateFlow<Int?>(null)
    val limit = _limit.asStateFlow()
    private var lastRequest = 0L
    private var busy = false
    private var endpointIndex = 0
    @Volatile private var lastFix: Location? = null

    init {
        scope.launch {
            runCatching {
                if (cache.exists() && cache.length() <= 8_000_000) {
                    val j = JSONObject(cache.readText())
                    _state.value = parse(j.getJSONObject("data"), GeoPoint(j.getDouble("lat"), j.getDouble("lng")), j.getLong("time"))
                }
            }
            while (isActive) { updateLimit(); delay(1000) }
        }
    }
    fun onLocation(location: Location) {
        lastFix = Location(location)
        ensure(location.latitude, location.longitude)
    }
    private fun updateLimit() {
        val fix = lastFix
        val snapshot = _state.value
        _limit.value = if (fix == null || !snapshot.covers(GeoPoint(fix.latitude,fix.longitude), System.currentTimeMillis())) null
        else RoadMatcher.limit(snapshot.ways, GeoPoint(fix.latitude,fix.longitude), if(fix.hasAccuracy()) fix.accuracy else Float.MAX_VALUE,
            if (fix.hasBearing() && fix.hasSpeed() && fix.speed > 2f) fix.bearing else null,
            (SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000)
    }
    @Synchronized fun ensure(lat: Double, lng: Double) {
        if (!lat.isFinite() || !lng.isFinite() || lat !in -85.0..85.0 || lng !in -180.0..180.0) return
        val point = GeoPoint(lat,lng)
        val now = System.currentTimeMillis()
        val old = _state.value
        if (busy || SystemClock.elapsedRealtime() - lastRequest < 90_000 && lastRequest != 0L) return
        if (old.covers(point,now) && old.center?.let { GeoPoint.distMeters(it,point) < 1000 } == true) return
        busy = true
        lastRequest = SystemClock.elapsedRealtime()
        scope.launch {
            _state.value = old.copy(status = L10n.text(R.string.ui_f87cf68a9cbf))
            var conn: HttpURLConnection? = null
            try {
                // Include roads WITHOUT maxspeed so adjacent unknown roads cannot be mistaken for known ones.
                val query = "[out:json][timeout:20];(node[highway~\"^(traffic_signals|speed_camera)$\"](around:2500,$lat,$lng);way[highway][highway!~\"^(footway|path|cycleway|steps|pedestrian)$\"](around:2500,$lat,$lng););out body geom;"
                val endpoints = listOf("https://overpass-api.de/api/interpreter", "https://overpass.private.coffee/api/interpreter")
                val url = URL(endpoints[endpointIndex] + "?data=" + URLEncoder.encode(query,"UTF-8"))
                @Suppress("DEPRECATION")
                val net = cm.allNetworks.firstOrNull { n -> cm.getNetworkCapabilities(n)?.let {
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                } == true }
                conn = (net?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 30_000
                conn.setRequestProperty("User-Agent", "TerraNav/0.2 (Android motorcycle navigation prototype; OpenStreetMap road features)")
                conn.setRequestProperty("Accept", "application/json")
                check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
                val bytes = conn.inputStream.use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer); if(count < 0) break
                        check(out.size() + count <= 8_000_000) { "Region too large" }
                        out.write(buffer,0,count)
                    }; out.toByteArray()
                }
                val data = JSONObject(bytes.toString(Charsets.UTF_8))
                check(!data.has("remark")) { "Incomplete Overpass response" }
                val at = System.currentTimeMillis()
                val parsed = parse(data,point,at)
                _state.value = parsed
                runCatching { cache.writeText(JSONObject().put("lat",lat).put("lng",lng).put("time",at).put("data",data).toString()) }
                updateLimit()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                endpointIndex = (endpointIndex + 1) % 2
                _state.value = old.copy(status = if(old.covers(point,System.currentTimeMillis())) L10n.text(R.string.ui_f33c92bf5593) else L10n.text(R.string.ui_592feddbc10a))
            } finally { conn?.disconnect(); synchronized(this@RoadInfoRepository) { busy = false } }
        }
    }
    private fun parse(json: JSONObject, center: GeoPoint, at: Long): RoadSnapshot {
        val features = mutableListOf<RoadFeature>(); val ways = mutableListOf<RoadWay>()
        val elements = json.getJSONArray("elements")
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i); val t = e.optJSONObject("tags") ?: continue
            val tags = t.keys().asSequence().associateWith { t.getString(it) }
            if(e.optString("type") == "node") {
                val kind = tags["highway"]
                if (kind == "traffic_signals" || kind == "speed_camera") features += RoadFeature(e.getLong("id"),GeoPoint(e.getDouble("lat"),e.getDouble("lon")),kind == "speed_camera")
            } else {
                val g = e.optJSONArray("geometry") ?: continue
                val points = (0 until g.length()).map { g.getJSONObject(it).let { p -> GeoPoint(p.getDouble("lat"),p.getDouble("lon")) } }
                ways += RoadWay(e.getLong("id"),points,tags)
            }
        }
        return RoadSnapshot(center,at,features,ways,L10n.text(R.string.ui_d17d7711679c))
    }
    companion object {
        @Volatile private var instance: RoadInfoRepository? = null
        fun get(context: Context): RoadInfoRepository = instance ?: synchronized(this) {
            instance ?: RoadInfoRepository(context.applicationContext).also { instance = it }
        }
    }
}
