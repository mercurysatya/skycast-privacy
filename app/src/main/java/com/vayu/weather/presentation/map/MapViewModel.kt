package com.vayu.weather.presentation.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.remote.RainViewerApi
import com.vayu.weather.data.remote.dto.RainViewerFrameDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "MapVM"

enum class BaseMapStyle(val displayName: String, val styleUrl: String) {
    STREET(
        "Street",
        "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json"
    ),
    SATELLITE(
        "Satellite",
        "https://api.mapbox.com/styles/v1/mapbox/satellite-v9?access_token="
    ),
    TERRAIN(
        "Terrain",
        "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
    ),
    DARK(
        "Dark",
        "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
    )
}

data class RadarFrame(
    val time: Long,
    val path: String,
    val label: String = ""
)

data class RadarState(
    val tileHost: String = "https://tilecache.rainviewer.com",
    val frames: List<RadarFrame> = emptyList(),
    val selectedFrameIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentPath: String?
        get() = if (selectedFrameIndex in frames.indices) frames[selectedFrameIndex].path else null

    val currentLabel: String
        get() = if (selectedFrameIndex in frames.indices) frames[selectedFrameIndex].label else ""

    val hasFrames: Boolean
        get() = frames.isNotEmpty()
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val rainViewerApi: RainViewerApi
) : ViewModel() {

    private val _radarState = MutableStateFlow(RadarState())
    val radarState: StateFlow<RadarState> = _radarState.asStateFlow()

    private val _selectedBaseMap = MutableStateFlow(BaseMapStyle.STREET)
    val selectedBaseMap: StateFlow<BaseMapStyle> = _selectedBaseMap.asStateFlow()

    private val _isRadarVisible = MutableStateFlow(true)
    val isRadarVisible: StateFlow<Boolean> = _isRadarVisible.asStateFlow()

    private var autoPlayJob: kotlinx.coroutines.Job? = null

    init {
        loadRadarFrames()
        autoRefresh()
    }

    fun loadRadarFrames() {
        viewModelScope.launch {
            _radarState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = rainViewerApi.getRadarFrames()
                val pastFrames = response.radar?.past.orEmpty()
                val nowcastFrames = response.radar?.nowcast.orEmpty()
                val host = response.host ?: "https://tilecache.rainviewer.com"

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val allFrames = (pastFrames + nowcastFrames).map { frame ->
                    val isNowcast = nowcastFrames.any { it.time == frame.time }
                    val timeStr = timeFormat.format(Date(frame.time * 1000))
                    val label = if (isNowcast) "Forecast $timeStr" else timeStr
                    RadarFrame(
                        time = frame.time,
                        path = frame.path,
                        label = label
                    )
                }.sortedBy { it.time }

                val selectedIndex = if (allFrames.isNotEmpty()) {
                    // Select the latest past frame (not nowcast) by default
                    val pastOnly = allFrames.filter { frame ->
                        pastFrames.any { it.time == frame.time }
                    }
                    pastOnly.lastIndex.coerceAtLeast(0)
                } else 0

                Log.d(TAG, "Loaded ${allFrames.size} frames (${pastFrames.size} past, ${nowcastFrames.size} nowcast)")

                _radarState.update {
                    it.copy(
                        tileHost = host,
                        frames = allFrames,
                        selectedFrameIndex = selectedIndex,
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load radar frames", e)
                _radarState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load radar data"
                    )
                }
            }
        }
    }

    fun selectFrame(index: Int) {
        _radarState.update { state ->
            state.copy(
                selectedFrameIndex = index.coerceIn(0, (state.frames.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun selectNextFrame() {
        _radarState.update { state ->
            val next = (state.selectedFrameIndex + 1).coerceAtMost(state.frames.size - 1)
            state.copy(selectedFrameIndex = next)
        }
    }

    fun selectPreviousFrame() {
        _radarState.update { state ->
            val prev = (state.selectedFrameIndex - 1).coerceAtLeast(0)
            state.copy(selectedFrameIndex = prev)
        }
    }

    fun toggleAutoPlay() {
        if (autoPlayJob?.isActive == true) {
            autoPlayJob?.cancel()
            autoPlayJob = null
        } else {
            autoPlayJob = viewModelScope.launch {
                while (true) {
                    delay(800)
                    val state = _radarState.value
                    if (state.selectedFrameIndex < state.frames.size - 1) {
                        selectNextFrame()
                    } else {
                        // Loop back to the first frame
                        selectFrame(0)
                    }
                }
            }
        }
    }

    fun isAutoPlaying(): Boolean = autoPlayJob?.isActive == true

    fun selectBaseMapStyle(style: BaseMapStyle) {
        _selectedBaseMap.value = style
    }

    fun toggleRadarVisibility() {
        _isRadarVisible.value = !_isRadarVisible.value
    }

    /** Refresh radar frames every 5 minutes */
    private fun autoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                loadRadarFrames()
            }
        }
    }
}
