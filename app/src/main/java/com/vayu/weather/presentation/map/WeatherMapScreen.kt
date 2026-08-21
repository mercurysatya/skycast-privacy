package com.vayu.weather.presentation.map

import android.location.Location
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vayu.weather.domain.location.LocationTracker
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

@Composable
fun WeatherMapScreen(
    locationTracker: LocationTracker,
    mapViewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLayerPanel by remember { mutableStateOf(false) }

    val radarState by mapViewModel.radarState.collectAsState()
    val selectedBaseMap by mapViewModel.selectedBaseMap.collectAsState()
    val isRadarVisible by mapViewModel.isRadarVisible.collectAsState()

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
        userLocation?.let { location ->
            val zoom = clampZoom(DEFAULT_ZOOM.toDouble())
            cameraState.position = CameraPosition(
                target = Position(
                    latitude = location.latitude,
                    longitude = location.longitude
                ),
                zoom = zoom
            )
        }
    }

    val tileUrl = radarState.currentPath?.let { path ->
        "${radarState.tileHost}$path/256/{z}/{x}/{y}/2/1_1.png"
    }

    val mapStyle = remember(selectedBaseMap) {
        BaseStyle.Uri(selectedBaseMap.styleUrl)
    }

    val mapOptions = remember {
        MapOptions(
            renderOptions = RenderOptions(isDebugEnabled = false),
            ornamentOptions = OrnamentOptions.OnlyLogo
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = mapStyle,
            zoomRange = MAP_MIN_ZOOM..MAP_MAX_ZOOM,
            onMapLoadFailed = { reason ->
                Log.w(TAG, "Map load failed: $reason")
            },
            options = mapOptions
        ) {
            if (tileUrl != null && isRadarVisible) {
                val source = rememberRasterSource(
                    tiles = listOf(tileUrl),
                    options = TileSetOptions(
                        minZoom = 3,
                        maxZoom = 18
                    ),
                    tileSize = 256
                )
                RasterLayer(
                    id = "rainviewer-radar",
                    source = source,
                    visible = true,
                    minZoom = 3f,
                    maxZoom = 18f,
                    opacity = const(0.65f)
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Layer selector panel (top-right)
        AnimatedVisibility(
            visible = showLayerPanel,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            LayerSelectorPanel(
                selectedBaseMap = selectedBaseMap,
                isRadarVisible = isRadarVisible,
                onBaseMapSelected = { mapViewModel.selectBaseMapStyle(it) },
                onRadarToggle = { mapViewModel.toggleRadarVisibility() },
                onDismiss = { showLayerPanel = false }
            )
        }

        // FABs (right side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showLayerPanel = !showLayerPanel },
                containerColor = if (showLayerPanel)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = stringResource(R.string.map_layers),
                    tint = if (showLayerPanel)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FloatingActionButton(
                onClick = {
                    userLocation?.let { location ->
                        val zoom = clampZoom(FAB_ZOOM.toDouble())
                        cameraState.position = CameraPosition(
                            target = Position(
                                latitude = location.latitude,
                                longitude = location.longitude
                            ),
                            zoom = zoom
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Rounded.MyLocation,
                    contentDescription = stringResource(R.string.center_on_location),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Time slider (bottom)
        if (radarState.hasFrames && isRadarVisible) {
            RadarTimeSlider(
                radarState = radarState,
                onFrameSelected = { mapViewModel.selectFrame(it) },
                onPreviousFrame = { mapViewModel.selectPreviousFrame() },
                onNextFrame = { mapViewModel.selectNextFrame() },
                onToggleAutoPlay = { mapViewModel.toggleAutoPlay() },
                isAutoPlaying = mapViewModel.isAutoPlaying(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun LayerSelectorPanel(
    selectedBaseMap: BaseMapStyle,
    isRadarVisible: Boolean,
    onBaseMapSelected: (BaseMapStyle) -> Unit,
    onRadarToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.map_layers),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Radar toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRadarToggle)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRadarVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = if (isRadarVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.map_radar_overlay),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isRadarVisible,
                    onCheckedChange = { onRadarToggle() },
                    modifier = Modifier.scale(0.8f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = stringResource(R.string.map_base_map),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            BaseMapStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onBaseMapSelected(style)
                            onDismiss()
                        }
                        .background(
                            if (selectedBaseMap == style)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (style) {
                            BaseMapStyle.STREET -> Icons.Rounded.Map
                            BaseMapStyle.SATELLITE -> Icons.Rounded.SatelliteAlt
                            BaseMapStyle.TERRAIN -> Icons.Rounded.Terrain
                            BaseMapStyle.DARK -> Icons.Rounded.DarkMode
                        },
                        contentDescription = null,
                        tint = if (selectedBaseMap == style)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = style.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedBaseMap == style)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selectedBaseMap == style) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selectedBaseMap == style) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarTimeSlider(
    radarState: RadarState,
    onFrameSelected: (Int) -> Unit,
    onPreviousFrame: () -> Unit,
    onNextFrame: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    isAutoPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Time label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = radarState.currentLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Frame counter
                Text(
                    text = "${radarState.selectedFrameIndex + 1}/${radarState.frames.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Slider
            Slider(
                value = radarState.selectedFrameIndex.toFloat(),
                onValueChange = { onFrameSelected(it.toInt()) },
                valueRange = 0f..(radarState.frames.size - 1).coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            // Time range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = radarState.frames.firstOrNull()?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
                Text(
                    text = radarState.frames.lastOrNull()?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = onPreviousFrame,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.map_previous_frame),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play/Pause
                FilledTonalIconButton(
                    onClick = onToggleAutoPlay,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isAutoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isAutoPlaying) stringResource(R.string.map_pause) else stringResource(R.string.map_play),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next
                IconButton(
                    onClick = onNextFrame,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.map_next_frame),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
