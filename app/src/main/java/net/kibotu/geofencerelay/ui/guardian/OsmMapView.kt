package net.kibotu.geofencerelay.ui.guardian

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.model.GeofenceZone
import net.kibotu.geofencerelay.model.LocationPing
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    zone: GeofenceZone,
    targetPing: LocationPing?,
    isBreached: Boolean,
    recenterTrigger: Int = 0,
    onMapTapped: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current

    // Initialize MapView and persistent overlays ONCE to prevent memory leaks and crashes
    val (mapView, circleOverlay, centerMarker, targetMarker) = remember {
        val sharedPrefs = context.getSharedPreferences("${context.packageName}_osm", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, sharedPrefs)
        Configuration.getInstance().userAgentValue = context.packageName

        val initialLat = when {
            targetPing != null && targetPing.latitude != 0.0 -> targetPing.latitude
            zone.latitude != 0.0 -> zone.latitude
            else -> 12.9716
        }
        val initialLon = when {
            targetPing != null && targetPing.longitude != 0.0 -> targetPing.longitude
            zone.longitude != 0.0 -> zone.longitude
            else -> 77.5946
        }

        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(initialLat, initialLon))
        }

        val circle = Polygon(map).apply {
            outlinePaint.strokeWidth = 6f
            fillPaint.style = Paint.Style.FILL
        }

        val cMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Safe Zone Center"
        }

        val tMarker = Marker(map).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_location_marker)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tracked Device"
        }

        // Add overlays in fixed order
        map.overlays.add(circle)
        map.overlays.add(cMarker)
        map.overlays.add(tMarker)

        // Tap overlay to allow setting safe zone by tapping
        map.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent?, mv: MapView?): Boolean {
                if (e != null && mv != null) {
                    val projection = mv.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as? GeoPoint
                    if (geoPoint != null) {
                        onMapTapped(geoPoint.latitude, geoPoint.longitude)
                        return true
                    }
                }
                return false
            }
        })

        arrayOf(map, circle, cMarker, tMarker)
    }

    val map = mapView as MapView
    val circle = circleOverlay as Polygon
    val cMarker = centerMarker as Marker
    val tMarker = targetMarker as Marker

    DisposableEffect(Unit) {
        map.onResume()
        onDispose {
            map.onPause()
        }
    }

    var hasCenteredInitialFix by remember { mutableStateOf(false) }
    var lastHandledRecenterTrigger by remember { mutableIntStateOf(0) }
    var lastZoneLat by remember { mutableStateOf(0.0) }
    var lastZoneLon by remember { mutableStateOf(0.0) }
    var lastZoneRadius by remember { mutableStateOf(0.0) }
    var lastBreachState by remember { mutableStateOf<Boolean?>(null) }
    var lastTargetLat by remember { mutableStateOf(0.0) }
    var lastTargetLon by remember { mutableStateOf(0.0) }

    // Smooth camera control: Center only on initial fix OR when user clicks recenter button
    LaunchedEffect(targetPing?.latitude, targetPing?.longitude, recenterTrigger) {
        val lat = targetPing?.latitude ?: zone.latitude
        val lon = targetPing?.longitude ?: zone.longitude
        if (lat != 0.0 && lon != 0.0) {
            if (!hasCenteredInitialFix || recenterTrigger != lastHandledRecenterTrigger) {
                hasCenteredInitialFix = true
                lastHandledRecenterTrigger = recenterTrigger
                map.controller.animateTo(GeoPoint(lat, lon))
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { map },
        update = {
            var overlaysDirty = false

            // 1. Update Safe Zone Circle ONLY when zone values or breach state actually change
            val zoneChanged = zone.latitude != lastZoneLat || zone.longitude != lastZoneLon || zone.radiusMeters != lastZoneRadius || isBreached != lastBreachState
            if (zoneChanged) {
                lastZoneLat = zone.latitude
                lastZoneLon = zone.longitude
                lastZoneRadius = zone.radiusMeters
                lastBreachState = isBreached

                if (zone.latitude != 0.0 && zone.longitude != 0.0) {
                    circle.points = Polygon.pointsAsCircle(
                        GeoPoint(zone.latitude, zone.longitude),
                        zone.radiusMeters
                    )
                    val strokeColor = if (isBreached) Color.RED else Color.rgb(251, 121, 34) // IRCTC Saffron
                    val fillColor = if (isBreached) Color.argb(40, 220, 38, 38) else Color.argb(35, 251, 121, 34)
                    circle.outlinePaint.color = strokeColor
                    circle.fillPaint.color = fillColor
                    circle.isEnabled = true

                    cMarker.position = GeoPoint(zone.latitude, zone.longitude)
                    cMarker.title = "🛡️ ${zone.name}"
                    cMarker.snippet = "Radius: ${zone.radiusMeters.toInt()}m"
                    cMarker.isEnabled = true
                } else {
                    circle.isEnabled = false
                    cMarker.isEnabled = false
                }
                overlaysDirty = true
            }

            // 2. Update Target Device Marker ONLY when coordinates actually change
            val curLat = targetPing?.latitude ?: 0.0
            val curLon = targetPing?.longitude ?: 0.0
            val targetChanged = curLat != lastTargetLat || curLon != lastTargetLon
            if (targetChanged) {
                lastTargetLat = curLat
                lastTargetLon = curLon

                if (targetPing != null && targetPing.latitude != 0.0) {
                    tMarker.position = GeoPoint(targetPing.latitude, targetPing.longitude)
                    tMarker.title = if (isBreached) "🚨 ${targetPing.deviceName} (BREACH)" else "📍 ${targetPing.deviceName}"
                    tMarker.snippet = targetPing.address
                    tMarker.isEnabled = true
                } else {
                    tMarker.isEnabled = false
                }
                overlaysDirty = true
            }

            if (overlaysDirty) {
                map.invalidate()
            }
        }
    )
}
