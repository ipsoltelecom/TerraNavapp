package com.example.northstar.ui.components

import com.example.northstar.settings.L10n
import com.example.northstar.R
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.northstar.dash.map.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.northstar.dash.nav.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

// Free, keyless, redistributable vector basemap (look-first, per the distribution decision).
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FOLLOW_ZOOM = 15.5
private const val NAV_ZOOM = 17.5
private const val NAV_TILT = 45.0
private const val RIDER_ICON = "rider-chevron"
private const val DEST_ICON = "dest-pin"

/**
 * In-app phone map (MapLibre + OpenFreeMap). Keyless and redistributable — no Google
 * Maps SDK / API key. The physical dash still uses the off-screen power-efficient renderer.
 *
 * Modes: [fitRoute] frames the whole route; [navMode] tilts/zooms/rotates to heading with a
 * rider chevron; default follows the rider north-up.
 */
@Composable
fun TerraNavMap(
    riderLat: Double?,
    riderLng: Double?,
    dest: Pair<Double, Double>?,
    routePoints: List<GeoPoint>,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
    fitRoute: Boolean = false,
    navMode: Boolean = false,
    riderBearing: Float = 0f,
    movingPerspective: Boolean = false,
) {
    val context = LocalContext.current
    val preferences = remember { MapPreferences.get(context) }
    val night by preferences.night.collectAsState()
    val roadInfo = remember { RoadInfoRepository.get(context) }
    val roadData by roadInfo.state.collectAsState()
    val speedLimit by roadInfo.limit.collectAsState()
    val roadFeatures = if (riderLat != null && riderLng != null && roadData.covers(GeoPoint(riderLat,riderLng),System.currentTimeMillis())) roadData.features else emptyList()
    LaunchedEffect(riderLat,riderLng) {
        if (riderLat != null && riderLng != null) roadInfo.ensure(riderLat,riderLng)
    }
    remember { MapLibre.getInstance(context) }
    val mapView = remember { MapView(context) }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var lineMgr by remember { mutableStateOf<LineManager?>(null) }
    var symbolMgr by remember { mutableStateOf<SymbolManager?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var styleGeneration by remember { mutableStateOf(0) }

    // Bind the MapView to the composition lifecycle.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_CREATE  -> mapView.onCreate(null)
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            lineMgr?.onDestroy(); symbolMgr?.onDestroy()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, night) {
        val generation = ++styleGeneration
        styleReady = false
        lineMgr?.onDestroy(); symbolMgr?.onDestroy()
        lineMgr = null; symbolMgr = null
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                isAttributionEnabled = true   // OSM/OpenFreeMap attribution (keep — licensing)
                isLogoEnabled = false
            }
            m.setStyle(Style.Builder().fromUri(if (night) "https://tiles.openfreemap.org/styles/dark" else STYLE_URL)) { style ->
                if (generation != styleGeneration) return@setStyle
                style.addImage("road-camera", RoadIcons.bitmap(true))
                style.addImage("road-signal", RoadIcons.bitmap(false))
                style.addImage(RIDER_ICON, chevronBitmap())
                style.addImage(DEST_ICON, destPinBitmap())
                lineMgr = LineManager(mapView, m, style)
                symbolMgr = SymbolManager(mapView, m, style).apply {
                    iconAllowOverlap = true; iconIgnorePlacement = true
                }
                styleReady = true
            }
        }
    }

    // Redraw route + markers whenever the data or mode changes.
    LaunchedEffect(styleReady, routePoints, dest, riderLat, riderLng, riderBearing, navMode, roadFeatures) {
        if (!styleReady) return@LaunchedEffect
        val lm = lineMgr ?: return@LaunchedEffect
        val sm = symbolMgr ?: return@LaunchedEffect
        lm.deleteAll(); sm.deleteAll()
        if (routePoints.size >= 2) {
            lm.create(
                LineOptions().withLatLngs(routePoints.map { LatLng(it.lat, it.lng) })
                    .withLineColor("#4285F4").withLineWidth(5.5f)
            )
        }
        roadFeatures.forEach { feature ->
            sm.create(SymbolOptions().withLatLng(LatLng(feature.point.lat,feature.point.lng))
                .withIconImage(if(feature.camera) "road-camera" else "road-signal").withIconSize(0.6f))
        }
        dest?.let { sm.create(SymbolOptions().withLatLng(LatLng(it.first, it.second)).withIconImage(DEST_ICON).withIconSize(1.1f)) }
        if (navMode && riderLat != null && riderLng != null) {
            sm.create(
                SymbolOptions().withLatLng(LatLng(riderLat, riderLng))
                    .withIconImage(RIDER_ICON).withIconRotate(riderBearing).withIconSize(1.0f)
            )
        }
    }

    // Camera control.
    LaunchedEffect(styleReady, riderLat, riderLng, riderBearing, navMode, movingPerspective, fitRoute, routePoints.size) {
        val m = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        when {
            fitRoute && routePoints.size >= 2 -> {
                val b = LatLngBounds.Builder()
                routePoints.forEach { b.include(LatLng(it.lat, it.lng)) }
                runCatching { m.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 110)) }
            }
            riderLat != null && riderLng != null -> {
                val target = LatLng(riderLat, riderLng)
                val pos = if (navMode)
                    CameraPosition.Builder().target(target).zoom(if (movingPerspective) NAV_ZOOM - 0.6 else NAV_ZOOM).tilt(if (movingPerspective) NAV_TILT else 0.0).bearing(riderBearing.toDouble()).build()
                else
                    CameraPosition.Builder().target(target).zoom(FOLLOW_ZOOM).tilt(0.0).bearing(0.0).build()
                runCatching { m.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 600) }
            }
            dest != null -> runCatching {
                m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(dest.first, dest.second), 13.0))
            }
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (riderLat != null && riderLng != null) Column(Modifier.align(Alignment.TopStart).padding(10.dp)
            .background(androidx.compose.ui.graphics.Color(0xE608111F), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(10.dp)) {
            androidx.compose.material3.Text(L10n.text(R.string.mapped_speed_limit, speedLimit?.toString() ?: "—"), color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
            androidx.compose.material3.Text(if (riderLat != null && riderLng != null && roadData.covers(GeoPoint(riderLat,riderLng),System.currentTimeMillis())) L10n.known(roadData.status) else L10n.text(R.string.ui_2d5f83271c84), color = androidx.compose.ui.graphics.Color(0xFFA0B3CC), fontSize = 10.sp)
        }
    }
}

/** Google-style blue chevron-in-a-circle, pointing "up" (rotated to heading by the symbol). */
private fun chevronBitmap(): Bitmap {
    val s = 84
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.WHITE
    c.drawCircle(s / 2f, s / 2f, s * 0.34f, p)
    p.color = android.graphics.Color.rgb(66, 133, 244)
    c.drawCircle(s / 2f, s / 2f, s * 0.30f, p)
    p.color = android.graphics.Color.WHITE
    val cx = s / 2f
    c.drawPath(Path().apply {
        moveTo(cx, s * 0.24f); lineTo(s * 0.72f, s * 0.70f); lineTo(cx, s * 0.58f); lineTo(s * 0.28f, s * 0.70f); close()
    }, p)
    return bmp
}

/** Simple red destination pin (white ring + red fill). */
private fun destPinBitmap(): Bitmap {
    val s = 72
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.WHITE; c.drawCircle(s / 2f, s / 2f, s * 0.30f, p)
    p.color = android.graphics.Color.rgb(234, 67, 53); c.drawCircle(s / 2f, s / 2f, s * 0.24f, p)
    p.color = android.graphics.Color.WHITE; c.drawCircle(s / 2f, s / 2f, s * 0.09f, p)
    return bmp
}
