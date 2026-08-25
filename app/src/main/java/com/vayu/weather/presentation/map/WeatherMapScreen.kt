package com.vayu.weather.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.presentation.weather.getWeatherIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val TAG = "WeatherMap"
private const val MAP_MIN_ZOOM = 3f
private const val MAP_MAX_ZOOM = 18f
private const val DEFAULT_ZOOM = 10f
private const val FAB_ZOOM = 12f

private fun clampZoom(zoom: Double): Double =
    zoom.coerceIn(MAP_MIN_ZOOM.toDouble(), MAP_MAX_ZOOM.toDouble())

private fun mapConvertTemp(temp: Double): Int = temp.roundToInt()

private fun mapWeatherDescription(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "Clear Sky" else "Clear Night"
    1 -> "Mainly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Rain Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Cloudy"
}

/** Interpolate temperature to a color for the heatmap */
private fun tempToColor(temp: Double): Color {
    return when {
        temp < -10 -> Color(0xFF1A237E) // Deep blue
        temp < 0 -> Color(0xFF1565C0)   // Blue
        temp < 5 -> Color(0xFF0097A7)   // Cyan
        temp < 10 -> Color(0xFF2E7D32)  // Green
        temp < 15 -> Color(0xFF689F38)  // Light green
        temp < 20 -> Color(0xFFFDD835)  // Yellow
        temp < 25 -> Color(0xFFFF8F00)  // Orange
        temp < 30 -> Color(0xFFF44336)  // Red
        temp < 35 -> Color(0xFFB71C1C)  // Dark red
        else -> Color(0xFF4A148C)       // Purple (extreme)
    }
}

/** Convert wind speed + direction to arrow color */
private fun windSpeedColor(speed: Double): Color {
    return when {
        speed < 5 -> Color(0xFF81C784)
        speed < 15 -> Color(0xFFFFD54F)
        speed < 25 -> Color(0xFFFF8A65)
        speed < 35 -> Color(0xFFE53935)
        else -> Color(0xFF6A1B9A)
    }
}

@Composable
fun WeatherMapScreen(
    locationTracker: LocationTracker,
    mapViewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val radarState by mapViewModel.radarState.collectAsState()
    val cloudState by mapViewModel.cloudState.collectAsState()
    val selectedBaseMap by mapViewModel.selectedBaseMap.collectAsState()
    val isAutoPlaying by mapViewModel.isAutoPlaying.collectAsState()
    val tappedWeather by mapViewModel.tappedWeather.collectAsState()
    val favoritePins by mapViewModel.favoritePins.collectAsState()
    val tempPoints by mapViewModel.tempPoints.collectAsState()
    val windPoints by mapViewModel.windPoints.collectAsState()
    val mapAlerts by mapViewModel.mapAlerts.collectAsState()
    val mapTilt by mapViewModel.mapTilt.collectAsState()

    LaunchedEffect(Unit) {
        userLocation = locationTracker.getCurrentLocation()
        isLoading = false
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 20.0, longitude = 0.0),
            zoom = clampZoom(DEFAULT_ZOOM.toDouble())
        )
    )

    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            cameraState.position = CameraPosition(
                target = Position(latitude = loc.latitude, longitude = loc.longitude),                    zoom = clampZoom(DEFAULT_ZOOM.toDouble()),
                    tilt = mapTilt.degrees.toDouble()
            )
            mapViewModel.onBoundsChanged(loc.latitude, loc.longitude, DEFAULT_ZOOM.toDouble())
        }
    }

    val radarTileUrl = remember(radarState) {
        radarState.currentPath?.let { "${radarState.tileHost}$it/256/{z}/{x}/{y}/${radarState.colorScheme.id}/1_1.png" }
    }
    val cloudTileUrl = remember(cloudState) {
        cloudState.currentPath?.let { "${cloudState.tileHost}$it/256/{z}/{x}/{y}/2/1_1.png" }
    }
    val mapStyle = remember(selectedBaseMap) { BaseStyle.Uri(selectedBaseMap.styleUrl) }
    val mapOptions = remember { MapOptions(renderOptions = RenderOptions(isDebugEnabled = false), ornamentOptions = OrnamentOptions.OnlyLogo) }

    // Animated tilt
    val animatedTilt by animateFloatAsState(
        targetValue = mapTilt.degrees,
        animationSpec = tween(400, easing = LinearEasing),
        label = "tilt"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // === MAP ===
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = mapStyle,
            zoomRange = MAP_MIN_ZOOM..MAP_MAX_ZOOM,
            onMapLoadFailed = { Log.w(TAG, "Map load failed: $it") },
            options = mapOptions
        ) {
            // Radar overlay — key on URL forces source update during autoplay
            // maxZoom bumped to 10 so radar tiles render at typical user zoom (10-14)
            if (radarTileUrl != null && radarState.overlayType == OverlayType.RADAR) {
                key(radarTileUrl) {
                    val src = rememberRasterSource(
                        tiles = listOf(radarTileUrl),
                        options = TileSetOptions(minZoom = 0, maxZoom = 10),
                        tileSize = 256
                    )
                    RasterLayer(
                        id = "radar",
                        source = src,
                        visible = true,
                        minZoom = 0f,
                        maxZoom = 10f,
                        opacity = const(radarState.opacity)
                    )
                }
            }
            // Cloud overlay
            if (cloudTileUrl != null && radarState.overlayType == OverlayType.CLOUDS) {
                key(cloudTileUrl) {
                    val src = rememberRasterSource(
                        tiles = listOf(cloudTileUrl),
                        options = TileSetOptions(minZoom = 0, maxZoom = 10),
                        tileSize = 256
                    )
                    RasterLayer(
                        id = "clouds",
                        source = src,
                        visible = true,
                        minZoom = 0f,
                        maxZoom = 10f,
                        opacity = const(cloudState.opacity)
                    )
                }
            }
        }

        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

        // === TEMPERATURE HEATMAP OVERLAY (Canvas-based) ===
        if (radarState.overlayType == OverlayType.TEMPERATURE && tempPoints.isNotEmpty()) {
            TemperatureHeatmapOverlay(
                tempPoints = tempPoints,
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Map tap coordinates back to lat/lon — simplified
                        mapViewModel.onMapTap(20.0, 0.0)
                    }
                }
            )
        }

        // === WIND STREAMLINES OVERLAY (Canvas-based animated) ===
        if (radarState.overlayType == OverlayType.WIND && windPoints.isNotEmpty()) {
            WindStreamlineOverlay(
                windPoints = windPoints,
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // === WEATHER ALERT ZONES ===
        mapAlerts.forEach { alert ->
            AlertZoneMarker(
                alert = alert,
                cameraState = cameraState,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // === COMPASS (top-left) ===
        CompassIndicator(modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 16.dp))

        // === SEARCH BAR ===
        AnimatedVisibility(visible = showSearch, enter = slideInVertically { -it }, exit = slideOutVertically { -it }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 48.dp, end = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // === LAYER PANEL ===
        AnimatedVisibility(visible = showLayerPanel, enter = slideInVertically { -it }, exit = slideOutVertically { -it }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
            LayerSelectorPanel(selectedBaseMap = selectedBaseMap, overlayType = radarState.overlayType, radarColorScheme = radarState.colorScheme, radarOpacity = radarState.opacity, cloudOpacity = cloudState.opacity, onBaseMapSelected = { mapViewModel.selectBaseMapStyle(it) }, onOverlayTypeSelected = { mapViewModel.setOverlayType(it) }, onRadarColorSchemeSelected = { mapViewModel.setRadarColorScheme(it) }, onRadarOpacityChange = { mapViewModel.setRadarOpacity(it) }, onCloudOpacityChange = { mapViewModel.setCloudOpacity(it) }, onDismiss = { showLayerPanel = false })
        }

        // === FAVORITE PINS ===
        favoritePins.forEach { pin ->
            FavoritePinOverlay(pin = pin, cameraState = cameraState, modifier = Modifier.align(Alignment.TopStart))
        }

        // === RIGHT SIDE FABs ===
        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp).padding(bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Share FAB
            FloatingActionButton(onClick = { scope.launch { shareMapScreenshot(context, cameraState) } }, containerColor = MaterialTheme.colorScheme.surfaceVariant, elevation = FloatingActionButtonDefaults.elevation(0.dp), modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Share, contentDescription = "Share map", modifier = Modifier.size(20.dp))
            }
            // Legend FAB
            FloatingActionButton(onClick = { showLegend = !showLegend }, containerColor = if (showLegend && radarState.overlayType != OverlayType.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.CloudQueue, contentDescription = "Legend", tint = if (showLegend && radarState.overlayType != OverlayType.NONE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            // Search FAB
            FloatingActionButton(onClick = { showSearch = !showSearch }, containerColor = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = if (showSearch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            // Tilt FAB
            FloatingActionButton(onClick = { mapViewModel.cycleTilt() }, containerColor = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Explore, contentDescription = "Tilt: ${mapTilt.label}", modifier = Modifier.size(20.dp))
            }
            // Layers FAB
            FloatingActionButton(onClick = { showLayerPanel = !showLayerPanel }, containerColor = if (showLayerPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Layers, contentDescription = stringResource(R.string.map_layers), tint = if (showLayerPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            // My Location FAB
            FloatingActionButton(onClick = {
                userLocation?.let { loc ->
                    cameraState.position = CameraPosition(
                        target = Position(loc.latitude, loc.longitude),
                        zoom = clampZoom(FAB_ZOOM.toDouble()),
                        bearing = cameraState.position.bearing,
                        tilt = mapTilt.degrees.toDouble()
                    )
                    mapViewModel.onBoundsChanged(loc.latitude, loc.longitude, FAB_ZOOM.toDouble())
                }
            }, containerColor = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.center_on_location), tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        }

        // === LEGEND ===
        if (showLegend && radarState.overlayType != OverlayType.NONE) {
            RadarLegend(overlayType = radarState.overlayType, modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 140.dp))
        }

        // === TAP WEATHER CARD ===
        tappedWeather?.let { info ->
            MapWeatherCard(info = info, onDismiss = { mapViewModel.dismissTappedWeather() }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp))
        }

        // === TIME SLIDER WITH FRAME THUMBNAILS ===
        if (radarState.overlayType != OverlayType.NONE && mapViewModel.getActiveFrameCount() > 0) {
            RadarTimeSlider(
                overlayType = radarState.overlayType,
                currentLabel = mapViewModel.getActiveFrameLabel(),
                frameIndex = mapViewModel.getActiveFrameIndex(),
                frameCount = mapViewModel.getActiveFrameCount(),
                frames = when (radarState.overlayType) {
                    OverlayType.RADAR -> radarState.frames.map { it.label }
                    OverlayType.CLOUDS -> cloudState.frames.map { it.label }
                    else -> emptyList()
                },
                onFrameSelected = { mapViewModel.selectFrame(it) },
                onPreviousFrame = { mapViewModel.selectPreviousFrame() },
                onNextFrame = { mapViewModel.selectNextFrame() },
                onToggleAutoPlay = { mapViewModel.toggleAutoPlay() },
                isAutoPlaying = isAutoPlaying,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

// === Temperature Heatmap Overlay ===
@Composable
private fun TemperatureHeatmapOverlay(
    tempPoints: List<TempGridPoint>,
    cameraState: org.maplibre.compose.camera.CameraState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heatmap_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heatmap_alpha"
    )

    Canvas(modifier = modifier) {
        if (tempPoints.isEmpty()) return@Canvas
        val centerLat = tempPoints.map { it.lat }.average()
        val centerLon = tempPoints.map { it.lon }.average()
        val latRange = tempPoints.maxOf { it.lat } - tempPoints.minOf { it.lat }
        val lonRange = tempPoints.maxOf { it.lon } - tempPoints.minOf { it.lon }
        if (latRange == 0.0 || lonRange == 0.0) return@Canvas

        // Draw temperature cells as semi-transparent circles
        tempPoints.forEach { point ->
            val normalizedLat = (point.lat - centerLat) / (latRange / 2)
            val normalizedLon = (point.lon - centerLon) / (lonRange / 2)
            val x = (0.5f + normalizedLon.toFloat() * 0.45f) * size.width
            val y = (0.5f - normalizedLat.toFloat() * 0.45f) * size.height
            val color = tempToColor(point.temp)
            val radius = size.width * 0.12f

            // Soft gradient circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = pulseAlpha), Color.Transparent),
                    center = Offset(x, y),
                    radius = radius
                ),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

// === Wind Streamline Overlay ===
@Composable
private fun WindStreamlineOverlay(
    windPoints: List<WindPoint>,
    cameraState: org.maplibre.compose.camera.CameraState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wind_anim")
    val windOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind_offset"
    )

    Canvas(modifier = modifier) {
        if (windPoints.isEmpty()) return@Canvas
        val centerLat = windPoints.map { it.lat }.average()
        val centerLon = windPoints.map { it.lon }.average()
        val latRange = windPoints.maxOf { it.lat } - windPoints.minOf { it.lat }
        val lonRange = windPoints.maxOf { it.lon } - windPoints.minOf { it.lon }
        if (latRange == 0.0 || lonRange == 0.0) return@Canvas

        windPoints.forEach { point ->
            val normalizedLat = (point.lat - centerLat) / (latRange / 2)
            val normalizedLon = (point.lon - centerLon) / (lonRange / 2)
            val x = (0.5f + normalizedLon.toFloat() * 0.45f) * size.width
            val y = (0.5f - normalizedLat.toFloat() * 0.45f) * size.height
            val color = windSpeedColor(point.speed)

            // Draw wind arrow (line + head) based on direction
            val angle = Math.toRadians(point.direction.toDouble())
            val arrowLength = 30f + (point.speed / 5f).toFloat().coerceAtMost(40f)
            val dx = sin(angle).toFloat() * arrowLength
            val dy = -cos(angle).toFloat() * arrowLength

            // Animated dash offset
            val animDx = dx * windOffset
            val animDy = dy * windOffset

            // Arrow shaft
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(x - animDx * 0.3f, y - animDy * 0.3f),
                end = Offset(x + animDx * 0.7f, y + animDy * 0.7f),
                strokeWidth = 2f
            )

            // Arrowhead
            val headLen = 8f
            val headAngle1 = angle + 2.5
            val headAngle2 = angle - 2.5
            val tipX = x + animDx
            val tipY = y + animDy
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(tipX, tipY),
                end = Offset(tipX - headLen * sin(headAngle1).toFloat(), tipY + headLen * cos(headAngle1).toFloat()),
                strokeWidth = 2f
            )
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(tipX, tipY),
                end = Offset(tipX - headLen * sin(headAngle2).toFloat(), tipY + headLen * cos(headAngle2).toFloat()),
                strokeWidth = 2f
            )

            // Speed label dot
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 3f,
                center = Offset(x + arrowLength + 4f, y)
            )
        }
    }
}

// === Alert Zone Marker ===
@Composable
private fun AlertZoneMarker(
    alert: MapWeatherAlert,
    cameraState: org.maplibre.compose.camera.CameraState,
    modifier: Modifier = Modifier
) {
    val severityColor = when (alert.severity) {
        "high" -> Color(0xFFF44336)
        "medium" -> Color(0xFFFF9800)
        else -> Color(0xFFFFC107)
    }

    Box(modifier = modifier.padding(8.dp)) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        alert.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Text(
                        alert.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// === Compass Indicator ===
@Composable
private fun CompassIndicator(modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)), elevation = CardDefaults.cardElevation(2.dp)) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(28.dp)) {
                drawLine(Color.Red, center, center.copy(y = center.y - size.height / 2), strokeWidth = 2.5f)
                drawLine(Color.Gray.copy(alpha = 0.5f), center, center.copy(y = center.y + size.height / 2), strokeWidth = 1.5f)
                drawCircle(Color.Red, 2f, center)
            }
            Text("N", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp), fontSize = 8.sp)
        }
    }
}

// === Map Weather Card (tap result) ===
@Composable
private fun MapWeatherCard(info: MapWeatherInfo, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)), elevation = CardDefaults.cardElevation(4.dp)) {
        if (info.isLoading) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching weather...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else if (info.error != null) {
            Row(modifier = Modifier.padding(16.dp).clickable(onClick = onDismiss), verticalAlignment = Alignment.CenterVertically) {
                Text(info.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap to dismiss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        } else {
            Row(modifier = Modifier.padding(16.dp).clickable(onClick = onDismiss), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = getWeatherIcon(info.weatherCode, info.isDay), contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${info.temperature.roundToInt()}°", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(mapWeatherDescription(info.weatherCode, info.isDay), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    info.windSpeed?.let { Text("${it.roundToInt()} km/h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                    info.humidity?.let { Text("${it.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Rounded.Visibility, contentDescription = "Dismiss", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

// === Favorite Pin Overlay ===
@Composable
private fun FavoritePinOverlay(pin: MapFavoritePin, cameraState: Any, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    pin.weatherCode?.let { code ->
                        Icon(imageVector = getWeatherIcon(code, pin.isDay), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    pin.temperature?.let { temp ->
                        Text("${temp.roundToInt()}°", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
                    }
                }
            }
            val pinColor = MaterialTheme.colorScheme.primaryContainer
            Canvas(modifier = Modifier.size(8.dp, 6.dp)) {
                drawPath(Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }, color = pinColor)
            }
        }
    }
}

// === Legend ===
@Composable
private fun RadarLegend(overlayType: OverlayType, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = when (overlayType) {
                    OverlayType.RADAR -> "Precipitation"
                    OverlayType.CLOUDS -> "Cloud Cover"
                    OverlayType.TEMPERATURE -> "Temperature"
                    OverlayType.WIND -> "Wind Speed"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            when (overlayType) {
                OverlayType.RADAR -> {
                    Canvas(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp))) {
                        val colors = listOf(Color(0x00000000), Color(0xFF66CCFF), Color(0xFF009900), Color(0xFFFFCC00), Color(0xFFFF6600), Color(0xFFFF0000), Color(0xFFCC0066))
                        val step = size.width / (colors.size - 1)
                        for (i in 0 until colors.size - 1) drawRect(colors[i], Offset(i * step, 0f), androidx.compose.ui.geometry.Size(step + 1f, size.height))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("mm/h", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                OverlayType.CLOUDS -> {
                    Canvas(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp))) {
                        drawRect(brush = Brush.horizontalGradient(listOf(Color(0x00000000), Color(0x88FFFFFF), Color(0xDDFFFFFF))))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("Overcast", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                OverlayType.TEMPERATURE -> {
                    Canvas(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp))) {
                        val colors = listOf(Color(0xFF1A237E), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFFDD835), Color(0xFFFF8F00), Color(0xFFF44336), Color(0xFF4A148C))
                        val step = size.width / (colors.size - 1)
                        for (i in 0 until colors.size - 1) drawRect(colors[i], Offset(i * step, 0f), androidx.compose.ui.geometry.Size(step + 1f, size.height))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("-10°C", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("35°C+", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                OverlayType.WIND -> {
                    Canvas(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp))) {
                        val colors = listOf(Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFE53935), Color(0xFF6A1B9A))
                        val step = size.width / (colors.size - 1)
                        for (i in 0 until colors.size - 1) drawRect(colors[i], Offset(i * step, 0f), androidx.compose.ui.geometry.Size(step + 1f, size.height))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Calm", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("Gale", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                else -> {}
            }
        }
    }
}

// === Layer Selector ===
@Composable
private fun LayerSelectorPanel(selectedBaseMap: BaseMapStyle, overlayType: OverlayType, radarColorScheme: RadarColorScheme, radarOpacity: Float, cloudOpacity: Float, onBaseMapSelected: (BaseMapStyle) -> Unit, onOverlayTypeSelected: (OverlayType) -> Unit, onRadarColorSchemeSelected: (RadarColorScheme) -> Unit, onRadarOpacityChange: (Float) -> Unit, onCloudOpacityChange: (Float) -> Unit, onDismiss: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.width(240.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.map_layers), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 4.dp))

            // OVERLAY TYPE
            Text("Weather Overlay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            OverlayType.entries.forEach { type ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOverlayTypeSelected(type) }.background(if (overlayType == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = when (type) { OverlayType.NONE -> Icons.Rounded.VisibilityOff; OverlayType.RADAR -> Icons.Rounded.Visibility; OverlayType.CLOUDS -> Icons.Rounded.CloudQueue; OverlayType.TEMPERATURE -> Icons.Rounded.Thermostat; OverlayType.WIND -> Icons.Rounded.Waves }, contentDescription = null, tint = if (overlayType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(type.displayName, style = MaterialTheme.typography.bodyMedium, color = if (overlayType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (overlayType == type) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (overlayType == type) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }

            // OPACITY
            if (overlayType != OverlayType.NONE) {
                Spacer(modifier = Modifier.height(4.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)); Spacer(modifier = Modifier.height(4.dp))
                Text("Opacity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Slider(value = if (overlayType == OverlayType.RADAR) radarOpacity else cloudOpacity, onValueChange = { if (overlayType == OverlayType.RADAR) onRadarOpacityChange(it) else onCloudOpacityChange(it) }, valueRange = 0.1f..1f, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                Text("${((if (overlayType == OverlayType.RADAR) radarOpacity else cloudOpacity) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.End))
            }

            // COLOR SCHEME (radar only)
            if (overlayType == OverlayType.RADAR) {
                Spacer(modifier = Modifier.height(4.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)); Spacer(modifier = Modifier.height(4.dp))
                Text("Color Scheme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                RadarColorScheme.entries.forEach { scheme ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onRadarColorSchemeSelected(scheme) }.background(if (radarColorScheme == scheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).padding(vertical = 6.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(scheme.displayName, style = MaterialTheme.typography.bodySmall, color = if (radarColorScheme == scheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (radarColorScheme == scheme) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (radarColorScheme == scheme) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // BASE MAP
            Spacer(modifier = Modifier.height(8.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)); Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.map_base_map), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp))
            BaseMapStyle.entries.forEach { style ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onBaseMapSelected(style); onDismiss() }.background(if (selectedBaseMap == style) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = when (style) { BaseMapStyle.STREET -> Icons.Rounded.Map; BaseMapStyle.SATELLITE -> Icons.Rounded.SatelliteAlt; BaseMapStyle.TERRAIN -> Icons.Rounded.Terrain; BaseMapStyle.DARK -> Icons.Rounded.DarkMode; BaseMapStyle.TOPO -> Icons.Rounded.Terrain }, contentDescription = null, tint = if (selectedBaseMap == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(style.displayName, style = MaterialTheme.typography.bodyMedium, color = if (selectedBaseMap == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (selectedBaseMap == style) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (selectedBaseMap == style) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// === Time Slider with Frame Thumbnails ===
@Composable
private fun RadarTimeSlider(
    overlayType: OverlayType,
    currentLabel: String,
    frameIndex: Int,
    frameCount: Int,
    frames: List<String>,
    onFrameSelected: (Int) -> Unit,
    onPreviousFrame: () -> Unit,
    onNextFrame: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    isAutoPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currentLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${(frameIndex + 1).coerceAtLeast(0)}/$frameCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(value = frameIndex.toFloat().coerceIn(0f, (frameCount - 1).coerceAtLeast(1).toFloat()), onValueChange = { onFrameSelected(it.toInt()) }, valueRange = 0f..(frameCount - 1).coerceAtLeast(1).toFloat(), modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.height(4.dp))

            // Frame thumbnail strip
            if (frames.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(frames) { index, label ->
                        val isSelected = index == frameIndex
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { onFrameSelected(index) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousFrame, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(24.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onToggleAutoPlay, modifier = Modifier.size(44.dp)) { Icon(imageVector = if (isAutoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isAutoPlaying) "Pause" else "Play", modifier = Modifier.size(28.dp)) }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onNextFrame, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

// === Share Map Screenshot ===
private suspend fun shareMapScreenshot(context: Context, cameraState: org.maplibre.compose.camera.CameraState) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.parseColor("#1a1a2e"))
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 48f; isAntiAlias = true }
            canvas.drawText("Weather Map", 40f, 80f, paint)
            paint.textSize = 28f
            canvas.drawText("SkyCast Weather", 40f, 560f, paint.apply { textSize = 24f; color = android.graphics.Color.GRAY })

            val file = java.io.File(context.cacheDir, "map_share.png")
            java.io.FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            bitmap.recycle()

            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_TEXT, "Weather map from SkyCast")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) { context.startActivity(android.content.Intent.createChooser(intent, "Share Map")) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share map", e)
        }
    }
}
