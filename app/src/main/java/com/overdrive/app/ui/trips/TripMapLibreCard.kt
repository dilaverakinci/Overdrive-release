package com.overdrive.app.ui.trips

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.overdrive.app.R
import com.overdrive.app.navmap.nav.MapNetworking
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale

private const val ROUTE_SOURCE_ID = "trip-route-source"
private const val ROUTE_CASING_ID = "trip-route-casing"
private const val ROUTE_LINE_ID = "trip-route-line"
private const val PUCK_SOURCE_ID = "trip-puck-source"
private const val PUCK_HALO_ID = "trip-puck-halo"
private const val PUCK_SYMBOL_ID = "trip-puck-symbol"
private const val PUCK_ARROW_IMG = "trip-puck-arrow-img"
private const val ENDPOINTS_SOURCE_ID = "trip-endpoints-source"
private const val START_POINT_ID = "trip-start-point"
private const val END_POINT_ID = "trip-end-point"

/**
 * Native MapLibre vector map card for Trip Detail route playback.
 * Renders real road network basemap (OpenFreeMap liberty/dark) and overlays
 * GPS route polyline, start/end pins, and heading-oriented vehicle puck.
 */
@Composable
fun TripMapLibreCard(
    points: List<TripTelemetryPoint>,
    currentPoint: TripTelemetryPoint?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val brandTeal = MaterialTheme.colorScheme.primary.toArgb()

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var puckSourceRef by remember { mutableStateOf<GeoJsonSource?>(null) }
    var routeSourceRef by remember { mutableStateOf<GeoJsonSource?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }
    var followCar by remember { mutableStateOf(false) }

    val validPoints = remember(points) {
        points.filter { it.lat != 0.0 && it.lon != 0.0 }
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapNetworking.installMapLibreHttpClient()
        MapView(context).apply {
            onCreate(null)
        }
    }

    // Lifecycle forwarding
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Camera framing function
    val fitRouteCamera: (Boolean) -> Unit = remember(validPoints, mapInstance) {
        { animated ->
            mapInstance?.let { map ->
                if (validPoints.size >= 2) {
                    val builder = LatLngBounds.Builder()
                    validPoints.forEach { p ->
                        builder.include(LatLng(p.lat, p.lon))
                    }
                    try {
                        val bounds = builder.build()
                        val update = CameraUpdateFactory.newLatLngBounds(bounds, 70)
                        if (animated) map.easeCamera(update, 600) else map.moveCamera(update)
                    } catch (_: Throwable) {}
                } else if (validPoints.size == 1) {
                    val p = validPoints.first()
                    val update = CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lon), 14.5)
                    if (animated) map.easeCamera(update, 600) else map.moveCamera(update)
                }
            }
        }
    }

    // Setup map style and layers
    LaunchedEffect(mapView, isDark) {
        mapView.getMapAsync { map ->
            mapInstance = map
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isCompassEnabled = false

            val styleAsset = if (isDark) "maps/dark_style.json" else "maps/liberty_style.json"
            val json = try {
                context.assets.open(styleAsset).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Throwable) {
                null
            }

            val styleBuilder = if (json != null) {
                Style.Builder().fromJson(json)
            } else {
                Style.Builder().fromUri(if (isDark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty")
            }

            map.setStyle(styleBuilder) { style ->
                // Register arrow icon
                val arrowBmp = createPuckArrowBitmap(brandTeal)
                style.addImage(PUCK_ARROW_IMG, arrowBmp)

                // 1. Route Source & Layers
                val coords = validPoints.map { Point.fromLngLat(it.lon, it.lat) }
                val routeSource = if (coords.size >= 2) {
                    GeoJsonSource(ROUTE_SOURCE_ID, LineString.fromLngLats(coords))
                } else {
                    GeoJsonSource(ROUTE_SOURCE_ID)
                }
                style.addSource(routeSource)
                routeSourceRef = routeSource

                // Casing
                style.addLayer(
                    LineLayer(ROUTE_CASING_ID, ROUTE_SOURCE_ID).withProperties(
                        PropertyFactory.lineColor(if (isDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()),
                        PropertyFactory.lineWidth(9f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                )

                // Main route line
                style.addLayer(
                    LineLayer(ROUTE_LINE_ID, ROUTE_SOURCE_ID).withProperties(
                        PropertyFactory.lineColor(brandTeal),
                        PropertyFactory.lineWidth(5.5f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                )

                // 2. Start / Finish Endpoints
                if (validPoints.size >= 2) {
                    val startP = validPoints.first()
                    val endP = validPoints.last()
                    val startFeature = Feature.fromGeometry(Point.fromLngLat(startP.lon, startP.lat)).apply {
                        addStringProperty("type", "start")
                    }
                    val endFeature = Feature.fromGeometry(Point.fromLngLat(endP.lon, endP.lat)).apply {
                        addStringProperty("type", "end")
                    }
                    val endpointsSource = GeoJsonSource(
                        ENDPOINTS_SOURCE_ID,
                        FeatureCollection.fromFeatures(arrayOf(startFeature, endFeature))
                    )
                    style.addSource(endpointsSource)

                    // Start Dot (Emerald Green)
                    style.addLayer(
                        CircleLayer(START_POINT_ID, ENDPOINTS_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor(0xFF10B981.toInt()),
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                            PropertyFactory.circleStrokeWidth(2.5f)
                        ).withFilter(Expression.eq(Expression.get("type"), "start"))
                    )

                    // End Dot (Coral Red)
                    style.addLayer(
                        CircleLayer(END_POINT_ID, ENDPOINTS_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor(0xFFEF4444.toInt()),
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                            PropertyFactory.circleStrokeWidth(2.5f)
                        ).withFilter(Expression.eq(Expression.get("type"), "end"))
                    )
                }

                // 3. Vehicle Puck (Current Scrubber Point)
                val cur = currentPoint ?: validPoints.firstOrNull()
                val initPuckFeature = if (cur != null) {
                    Feature.fromGeometry(Point.fromLngLat(cur.lon, cur.lat)).apply {
                        addNumberProperty("bearing", cur.headingDegrees)
                    }
                } else {
                    Feature.fromGeometry(Point.fromLngLat(0.0, 0.0)).apply {
                        addNumberProperty("bearing", 0f)
                    }
                }
                val puckSource = GeoJsonSource(PUCK_SOURCE_ID, initPuckFeature)
                style.addSource(puckSource)
                puckSourceRef = puckSource

                // Puck base halo
                style.addLayer(
                    CircleLayer(PUCK_HALO_ID, PUCK_SOURCE_ID).withProperties(
                        PropertyFactory.circleColor(0xFFFFFFFF.toInt()),
                        PropertyFactory.circleRadius(14f),
                        PropertyFactory.circleOpacity(0.9f),
                        PropertyFactory.circleStrokeColor(brandTeal),
                        PropertyFactory.circleStrokeWidth(2f)
                    )
                )

                // Puck Arrow
                style.addLayer(
                    SymbolLayer(PUCK_SYMBOL_ID, PUCK_SOURCE_ID).withProperties(
                        PropertyFactory.iconImage(PUCK_ARROW_IMG),
                        PropertyFactory.iconRotate(Expression.get("bearing")),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconSize(0.95f)
                    )
                )

                isStyleReady = true
                fitRouteCamera(false)
            }
        }
    }

    // Dynamic update when currentPoint (timeline scrubber) changes
    LaunchedEffect(currentPoint, isStyleReady, followCar) {
        if (isStyleReady && currentPoint != null && currentPoint.lat != 0.0 && currentPoint.lon != 0.0) {
            val f = Feature.fromGeometry(Point.fromLngLat(currentPoint.lon, currentPoint.lat)).apply {
                addNumberProperty("bearing", currentPoint.headingDegrees)
            }
            puckSourceRef?.setGeoJson(f)

            if (followCar) {
                mapInstance?.easeCamera(
                    CameraUpdateFactory.newLatLng(LatLng(currentPoint.lat, currentPoint.lon)),
                    150
                )
            }
        }
    }

    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row with Map Legend & Telemetry Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Rota Haritası (MapLibre Native)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Legend & Coordinates
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "Başlangıç",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = "Bitiş",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentPoint != null) {
                        Text(
                            text = "Yön: ${currentPoint.headingDegrees.toInt()}° · İrtifa: ${currentPoint.altitudeM.toInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Map Container with Overlaid Action Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // MapLibre View
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                )

                // Top-right floating controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OverdriveButton(
                        text = if (followCar) "Takip Açık" else "Aracı Takip Et",
                        variant = if (followCar) OverdriveButtonVariant.PRIMARY else OverdriveButtonVariant.TONAL,
                        onClick = {
                            followCar = !followCar
                            if (followCar && currentPoint != null) {
                                mapInstance?.easeCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(currentPoint.lat, currentPoint.lon),
                                        15.5
                                    ),
                                    400
                                )
                            }
                        }
                    )

                    OverdriveButton(
                        text = "Rotaya Odakla",
                        variant = OverdriveButtonVariant.TONAL,
                        onClick = {
                            followCar = false
                            fitRouteCamera(true)
                        }
                    )
                }

                // Bottom GPS Coordinates Pill
                if (currentPoint != null && currentPoint.lat != 0.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", currentPoint.lat, currentPoint.lon)} · Hız: ${currentPoint.speedKmh} km/s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates a 3D-styled chevron navigation arrow bitmap pointing North.
 */
private fun createPuckArrowBitmap(accentColor: Int): Bitmap {
    val s = 72
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = s / 2f
    val cy = s / 2f
    val r = s * 0.40f

    fun arrowPath(scale: Float, dy: Float) = Path().apply {
        moveTo(cx, cy - r * 0.78f * scale + dy)
        lineTo(cx + r * 0.62f * scale, cy + r * 0.66f * scale + dy)
        lineTo(cx, cy + r * 0.30f * scale + dy)
        lineTo(cx - r * 0.62f * scale, cy + r * 0.66f * scale + dy)
        close()
    }

    // White outline
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    c.drawPath(arrowPath(1.22f, 0f), outline)

    // Body
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accentColor
    }
    c.drawPath(arrowPath(1.0f, 0f), body)
    return bmp
}
