package com.vayu.weather.presentation.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.remote.OpenMeteoApi
import com.vayu.weather.data.remote.RainViewerApi
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.use_case.GetFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "MapVM"

// === Enums ===

enum class RadarColorScheme(val id: Int, val displayName: String) {
    ORIGINAL(2, "Original"),
    UNIVERSAL_BLUE(3, "Blue"),
    UNIVERSAL_GREEN(4, "Green"),
    TERASCAN(5, "TeraScan"),
    WEATHER_CHANNEL(6, "Weather Ch."),
    DWD(8, "DWD"),
    NEXRAD(9, "NEXRAD"),
    RAINBOW(10, "Rainbow")
}

enum class BaseMapStyle(val displayName: String, val styleUrl: String) {
    STREET("Street", "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json"),
    SATELLITE("Satellite", "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"),
    TERRAIN("Terrain", "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"),
    DARK("Dark", "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"),
    TOPO("Topo", "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json")
}

enum class OverlayType(val displayName: String) {
    NONE("None"),
    RADAR("Rain Radar"),
    CLOUDS("Cloud Cover"),
    TEMPERATURE("Temperature"),
    WIND("Wind")
}

// === Data classes ===

data class RadarFrame(val time: Long, val path: String, val label: String = "")
data class CloudFrame(val time: Long, val path: String, val label: String = "")

data class RadarState(
    val tileHost: String = "https://tilecache.rainviewer.com",
    val frames: List<RadarFrame> = emptyList(),
    val selectedFrameIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val colorScheme: RadarColorScheme = RadarColorScheme.ORIGINAL,
    val opacity: Float = 0.65f,
    val overlayType: OverlayType = OverlayType.RADAR
) {
    val currentPath: String?
        get() = if (selectedFrameIndex in frames.indices) frames[selectedFrameIndex].path else null
    val currentLabel: String
        get() = if (selectedFrameIndex in frames.indices) frames[selectedFrameIndex].label else ""
    val hasFrames: Boolean get() = frames.isNotEmpty()
}

data class CloudState(
    val tileHost: String = "https://tilecache.rainviewer.com",
    val frames: List<CloudFrame> = emptyList(),
    val selectedFrameIndex: Int = -1,
    val opacity: Float = 0.45f
) {
    val currentPath: String?
        get() = if (selectedFrameIndex in frames.indices) frames[selectedFrameIndex].path else null
    val hasFrames: Boolean get() = frames.isNotEmpty()
}

/** Weather data fetched for a tapped map coordinate */
data class MapWeatherInfo(
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val isDay: Boolean,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Temperature grid point for heatmap overlay */
data class TempGridPoint(
    val lat: Double,
    val lon: Double,
    val temp: Double
)

/** Wind streamline point */
data class WindPoint(
    val lat: Double,
    val lon: Double,
    val speed: Double,
    val direction: Double // degrees
)

/** Weather alert for map zone */
data class MapWeatherAlert(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val description: String,
    val severity: String,
    val code: Int
)

/** Tilt state */
enum class MapTilt(val degrees: Float, val label: String) {
    FLAT(0f, "Flat"),
    TILT_20(20f, "20°"),
    TILT_45(45f, "45°")
}

/** A favorite city pin on the map */
data class MapFavoritePin(
    val city: City,
    val temperature: Double?,
    val weatherCode: Int?,
    val isDay: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val rainViewerApi: RainViewerApi,
    private val openMeteoApi: OpenMeteoApi,
    private val getFavoritesUseCase: GetFavoritesUseCase
) : ViewModel() {

    private val _radarState = MutableStateFlow(RadarState())
    val radarState: StateFlow<RadarState> = _radarState.asStateFlow()

    private val _cloudState = MutableStateFlow(CloudState())
    val cloudState: StateFlow<CloudState> = _cloudState.asStateFlow()

    private val _selectedBaseMap = MutableStateFlow(BaseMapStyle.STREET)
    val selectedBaseMap: StateFlow<BaseMapStyle> = _selectedBaseMap.asStateFlow()

    private val _isAutoPlaying = MutableStateFlow(false)
    val isAutoPlaying: StateFlow<Boolean> = _isAutoPlaying.asStateFlow()

    // Tap-to-weather
    private val _tappedWeather = MutableStateFlow<MapWeatherInfo?>(null)
    val tappedWeather: StateFlow<MapWeatherInfo?> = _tappedWeather.asStateFlow()

    // Favorite pins
    private val _favoritePins = MutableStateFlow<List<MapFavoritePin>>(emptyList())
    val favoritePins: StateFlow<List<MapFavoritePin>> = _favoritePins.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Temperature heatmap
    private val _tempPoints = MutableStateFlow<List<TempGridPoint>>(emptyList())
    val tempPoints: StateFlow<List<TempGridPoint>> = _tempPoints.asStateFlow()

    // Wind streamlines
    private val _windPoints = MutableStateFlow<List<WindPoint>>(emptyList())
    val windPoints: StateFlow<List<WindPoint>> = _windPoints.asStateFlow()

    // Weather alerts on map
    private val _mapAlerts = MutableStateFlow<List<MapWeatherAlert>>(emptyList())
    val mapAlerts: StateFlow<List<MapWeatherAlert>> = _mapAlerts.asStateFlow()

    // Map tilt
    private val _mapTilt = MutableStateFlow(MapTilt.FLAT)
    val mapTilt: StateFlow<MapTilt> = _mapTilt.asStateFlow()

    // Track visible bounds for grid loading
    private var visibleBounds: Pair<Pair<Double, Double>, Pair<Double, Double>>? = null

    private var autoPlayJob: Job? = null
    private var tapWeatherJob: Job? = null
    private var gridJob: Job? = null

    init {
        loadRadarFrames()
        loadFavoritePins()
        autoRefresh()
    }

    // === Radar/Cloud ===

    fun loadRadarFrames() {
        viewModelScope.launch {
            _radarState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = rainViewerApi.getRadarFrames()
                val pastFrames = response.radar?.past.orEmpty()
                val nowcastFrames = response.radar?.nowcast.orEmpty()
                val satelliteFrames = response.satellite?.infrared.orEmpty()
                val host = response.host ?: "https://tilecache.rainviewer.com"

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val allRadarFrames = (pastFrames + nowcastFrames).map { frame ->
                    val isNowcast = nowcastFrames.any { it.time == frame.time }
                    val timeStr = timeFormat.format(Date(frame.time * 1000))
                    RadarFrame(time = frame.time, path = frame.path, label = if (isNowcast) "Fcst $timeStr" else timeStr)
                }.sortedBy { it.time }

                val radarIndex = if (allRadarFrames.isNotEmpty()) {
                    allRadarFrames.filter { f -> pastFrames.any { it.time == f.time } }.lastIndex.coerceAtLeast(0)
                } else 0

                val allCloudFrames = satelliteFrames.map { frame ->
                    CloudFrame(time = frame.time, path = frame.path, label = timeFormat.format(Date(frame.time * 1000)))
                }.sortedBy { it.time }

                val cloudIndex = if (allCloudFrames.isNotEmpty()) {
                    allCloudFrames.filter { cf -> satelliteFrames.any { it.time == cf.time } }.lastIndex.coerceAtLeast(0)
                } else 0

                _radarState.update { it.copy(tileHost = host, frames = allRadarFrames, selectedFrameIndex = radarIndex, isLoading = false) }
                _cloudState.update { it.copy(tileHost = host, frames = allCloudFrames, selectedFrameIndex = cloudIndex) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e(TAG, "Failed to load frames", e)
                _radarState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectFrame(index: Int) {
        val type = _radarState.value.overlayType
        when (type) {
            OverlayType.RADAR -> _radarState.update { s -> s.copy(selectedFrameIndex = index.coerceIn(0, (s.frames.size - 1).coerceAtLeast(0))) }
            OverlayType.CLOUDS -> _cloudState.update { s -> s.copy(selectedFrameIndex = index.coerceIn(0, (s.frames.size - 1).coerceAtLeast(0))) }
            else -> {}
        }
    }

    fun selectNextFrame() {
        val type = _radarState.value.overlayType
        when (type) {
            OverlayType.RADAR -> _radarState.update { s -> s.copy(selectedFrameIndex = (s.selectedFrameIndex + 1).coerceAtMost(s.frames.size - 1)) }
            OverlayType.CLOUDS -> _cloudState.update { s -> s.copy(selectedFrameIndex = (s.selectedFrameIndex + 1).coerceAtMost(s.frames.size - 1)) }
            else -> {}
        }
    }

    fun selectPreviousFrame() {
        val type = _radarState.value.overlayType
        when (type) {
            OverlayType.RADAR -> _radarState.update { s -> s.copy(selectedFrameIndex = (s.selectedFrameIndex - 1).coerceAtLeast(0)) }
            OverlayType.CLOUDS -> _cloudState.update { s -> s.copy(selectedFrameIndex = (s.selectedFrameIndex - 1).coerceAtLeast(0)) }
            else -> {}
        }
    }

    fun toggleAutoPlay() {
        if (autoPlayJob?.isActive == true) {
            autoPlayJob?.cancel(); autoPlayJob = null; _isAutoPlaying.value = false
        } else {
            autoPlayJob = viewModelScope.launch {
                try { while (true) { delay(800); selectNextFrame() } }
                finally { _isAutoPlaying.value = false }
            }
            _isAutoPlaying.value = true
        }
    }

    fun selectBaseMapStyle(style: BaseMapStyle) { _selectedBaseMap.value = style }

    fun setOverlayType(type: OverlayType) {
        _radarState.update { it.copy(overlayType = type) }
        if (autoPlayJob?.isActive == true) { autoPlayJob?.cancel(); autoPlayJob = null; _isAutoPlaying.value = false }
    }

    fun setRadarColorScheme(scheme: RadarColorScheme) { _radarState.update { it.copy(colorScheme = scheme) } }
    fun setRadarOpacity(opacity: Float) { _radarState.update { it.copy(opacity = opacity.coerceIn(0.1f, 1f)) } }
    fun setCloudOpacity(opacity: Float) { _cloudState.update { it.copy(opacity = opacity.coerceIn(0.1f, 1f)) } }

    fun getActiveFrameCount(): Int = when (_radarState.value.overlayType) {
        OverlayType.RADAR -> _radarState.value.frames.size
        OverlayType.CLOUDS -> _cloudState.value.frames.size
        else -> 0
    }
    fun getActiveFrameIndex(): Int = when (_radarState.value.overlayType) {
        OverlayType.RADAR -> _radarState.value.selectedFrameIndex
        OverlayType.CLOUDS -> _cloudState.value.selectedFrameIndex
        else -> -1
    }
    fun getActiveFrameLabel(): String = when (_radarState.value.overlayType) {
        OverlayType.RADAR -> _radarState.value.currentLabel
        OverlayType.CLOUDS -> { val i = _cloudState.value.selectedFrameIndex; if (i in _cloudState.value.frames.indices) _cloudState.value.frames[i].label else "" }
        else -> ""
    }

    // === Tap-to-Weather ===

    fun onMapTap(lat: Double, lon: Double) {
        tapWeatherJob?.cancel()
        tapWeatherJob = viewModelScope.launch {
            _tappedWeather.value = MapWeatherInfo(lat, lon, 0.0, 0, null, null, null, true, isLoading = true)
            try {
                val weather = openMeteoApi.getWeather(lat, lon)
                val current = weather.current
                _tappedWeather.value = MapWeatherInfo(
                    latitude = lat, longitude = lon,
                    temperature = current?.temperature ?: 0.0,
                    weatherCode = current?.weatherCode ?: 0,
                    humidity = current?.humidity,
                    windSpeed = current?.windSpeed,
                    windDirection = current?.windDirection,
                    isDay = (current?.isDay ?: 1) == 1,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather for tap", e)
                _tappedWeather.value = MapWeatherInfo(lat, lon, 0.0, 0, null, null, null, true, error = "Weather unavailable")
            }
        }
    }

    fun dismissTappedWeather() { _tappedWeather.value = null }

    // === Favorite pins ===

    private fun loadFavoritePins() {
        viewModelScope.launch {
            getFavoritesUseCase().first().forEach { city ->
                fetchPinWeather(city)
            }
        }
    }

    private fun fetchPinWeather(city: City) {
        viewModelScope.launch {
            try {
                val weather = openMeteoApi.getWeather(city.latitude, city.longitude)
                val current = weather.current
                val pin = MapFavoritePin(city, current?.temperature, current?.weatherCode, (current?.isDay ?: 1) == 1)
                _favoritePins.update { existing -> existing.filter { it.city.id != city.id } + pin }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch pin weather for ${city.name}", e)
                _favoritePins.update { existing -> existing.filter { it.city.id != city.id } + MapFavoritePin(city, null, null) }
            }
        }
    }

    fun refreshFavoritePins() { loadFavoritePins() }

    // === Search ===

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // === Tilt ===

    fun cycleTilt() {
        val values = MapTilt.entries
        val currentIdx = values.indexOf(_mapTilt.value)
        _mapTilt.value = values[(currentIdx + 1) % values.size]
    }

    // === Grid Data (temp heatmap + wind) ===

    fun loadGridData(centerLat: Double, centerLon: Double, spanLat: Double = 8.0, spanLon: Double = 10.0) {
        gridJob?.cancel()
        gridJob = viewModelScope.launch {
            try {
                val lats = (-4..4).map { centerLat + it * (spanLat / 4) }
                val lons = (-4..4).map { centerLon + it * (spanLon / 4) }

                val tempPointsList = mutableListOf<TempGridPoint>()
                val windPointsList = mutableListOf<WindPoint>()

                for (lat in lats) {
                    for (lon in lons) {
                        try {
                            val weather = openMeteoApi.getWeather(lat, lon)
                            val current = weather.current
                            if (current != null) {
                                tempPointsList.add(TempGridPoint(lat, lon, current.temperature))
                                windPointsList.add(WindPoint(
                                    lat, lon,
                                    current.windSpeed ?: 0.0,
                                    current.windDirection ?: 0.0
                                ))
                            }
                        } catch (_: Exception) { }
                    }
                }

                _tempPoints.value = tempPointsList
                _windPoints.value = windPointsList
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e(TAG, "Failed to load grid data", e)
            }
        }
    }

    // === Weather Alerts on Map ===

    fun loadMapAlerts(centerLat: Double, centerLon: Double) {
        viewModelScope.launch {
            try {
                val weather = openMeteoApi.getWeatherData(centerLat, centerLon)
                val alertsList = mutableListOf<MapWeatherAlert>()
                val dailyCodes = weather.daily?.weatherCodes.orEmpty()
                val maxTemps = weather.daily?.maxTemperatures.orEmpty()
                val precipProbs = weather.daily?.precipitationProbabilities.orEmpty()

                dailyCodes.forEachIndexed { i, code ->
                    val codeVal = code ?: return@forEachIndexed
                    when {
                        codeVal >= 95 -> {
                            alertsList.add(MapWeatherAlert(centerLat, centerLon, "Thunderstorm Warning",
                                "Severe thunderstorm expected", "high", codeVal))
                        }
                        codeVal in 65..67 -> {
                            alertsList.add(MapWeatherAlert(centerLat, centerLon, "Heavy Rain Alert",
                                "Heavy rainfall expected", "medium", codeVal))
                        }
                        codeVal in 71..77 -> {
                            alertsList.add(MapWeatherAlert(centerLat, centerLon, "Snow Warning",
                                "Snow expected in the area", "medium", codeVal))
                        }
                        i < precipProbs.size && (precipProbs[i] ?: 0) > 80 -> {
                            alertsList.add(MapWeatherAlert(centerLat, centerLon, "High Precipitation",
                                "${precipProbs[i]}% chance of precipitation", "low", codeVal))
                        }
                        i < maxTemps.size && (maxTemps[i] ?: 0.0) > 40 -> {
                            alertsList.add(MapWeatherAlert(centerLat, centerLon, "Extreme Heat",
                                "Temperature expected to reach ${maxTemps[i]?.toInt()}°C", "high", codeVal))
                        }
                    }
                }
                _mapAlerts.value = alertsList
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e(TAG, "Failed to load map alerts", e)
            }
        }
    }

    fun onBoundsChanged(centerLat: Double, centerLon: Double, zoom: Double) {
        val span = when {
            zoom > 10 -> 4.0
            zoom > 7 -> 8.0
            zoom > 4 -> 16.0
            else -> 32.0
        }
        visibleBounds = Pair(Pair(centerLat, centerLon), Pair(span, span))
        // Only load heavy data if temp/wind overlays are active
        val type = _radarState.value.overlayType
        if (type == OverlayType.TEMPERATURE || type == OverlayType.WIND) {
            loadGridData(centerLat, centerLon, span, span * 1.25)
        }
        loadMapAlerts(centerLat, centerLon)
    }

    private fun autoRefresh() {
        viewModelScope.launch {
            while (true) { delay(5 * 60 * 1000L); loadRadarFrames() }
        }
    }
}
