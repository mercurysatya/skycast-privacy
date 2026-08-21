package com.vayu.weather.presentation.map

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    var showRadar by remember { mutableStateOf(true) }

    val radarState by mapViewModel.radarState.collectAsState()

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
            Log.d(TAG, "Camera to location: zoom=$zoom (clamped $MAP_MIN_ZOOM..$MAP_MAX_ZOOM)")
            cameraState.position = CameraPosition(
                target = Position(
                    latitude = location.latitude,
                    longitude = location.longitude
                ),
                zoom = zoom
            )
        }
    }

    val tileUrl = radarState.latestPath?.let { path ->
        "${radarState.tileHost}$path/256/{z}/{x}/{y}/2/1_1.png"
    }

    LaunchedEffect(tileUrl) {
        Log.d(TAG, "Radar tile URL: $tileUrl")
        Log.d(TAG, "Map zoom range: $MAP_MIN_ZOOM..$MAP_MAX_ZOOM")
    }

    val mapOptions = remember {
        MapOptions(
            renderOptions = RenderOptions(isDebugEnabled = false),
            ornamentOptions = OrnamentOptions.OnlyLogo
        )
    }
    val mapStyle = remember {
        BaseStyle.Uri("https://basemaps.cartocdn.com/gl/positron-gl-style/style.json")
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
            if (tileUrl != null) {
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
                    visible = showRadar,
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showRadar = !showRadar },
                containerColor = if (showRadar)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = if (showRadar) stringResource(R.string.hide_radar) else stringResource(R.string.show_radar),
                    tint = if (showRadar)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FloatingActionButton(
                onClick = {
                    userLocation?.let { location ->
                        val zoom = clampZoom(FAB_ZOOM.toDouble())
                        Log.d(TAG, "FAB re-center: zoom=$zoom")
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
    }
}
