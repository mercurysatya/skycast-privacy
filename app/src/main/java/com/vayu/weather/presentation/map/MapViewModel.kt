package com.vayu.weather.presentation.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.remote.RainViewerApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapVM"

data class RadarState(
    val tileHost: String = "https://tilecache.rainviewer.com",
    val latestPath: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val rainViewerApi: RainViewerApi
) : ViewModel() {

    private val _radarState = MutableStateFlow(RadarState())
    val radarState: StateFlow<RadarState> = _radarState.asStateFlow()

    init {
        loadRadarFrames()
        autoRefresh()
    }

    fun loadRadarFrames() {
        viewModelScope.launch {
            _radarState.value = _radarState.value.copy(isLoading = true, error = null)
            try {
                val response = rainViewerApi.getRadarFrames()
                val allFrames = (response.radar?.past.orEmpty() + response.radar?.nowcast.orEmpty())
                val latest = allFrames.maxByOrNull { it.time }
                val host = response.host ?: "https://tilecache.rainviewer.com"
                Log.d(TAG, "Loaded ${allFrames.size} frames, latest: ${latest?.path}, host: $host")
                _radarState.value = RadarState(
                    tileHost = host,
                    latestPath = latest?.path,
                    isLoading = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load radar frames", e)
                _radarState.value = _radarState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load radar data"
                )
            }
        }
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
