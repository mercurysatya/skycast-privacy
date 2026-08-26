package com.vayu.weather.presentation.weather

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.WeatherHistorySnapshot
import com.vayu.weather.domain.repository.WeatherRepository
import com.vayu.weather.domain.use_case.GetAirQualityUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val getAirQualityUseCase: GetAirQualityUseCase,
    private val repository: WeatherRepository,
    val locationTracker: LocationTracker,
    val settingsManager: SettingsManager
) : ViewModel() {

    var state by mutableStateOf(WeatherState())
        private set

    var currentCityName by mutableStateOf<String?>(null)
        private set

    internal var currentLat: Double? = null
        private set
    internal var currentLon: Double? = null
        private set

    private var refreshJob: Job? = null
    private var loadJob: Job? = null

    init {
        Log.d("WeatherViewModel", "ViewModel Initialized")
    }

    fun setCityName(name: String?) {
        currentCityName = name
    }

    fun loadWeatherInfo() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                error = null,
                refreshError = null
            )
            try {
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    loadAirQuality(location.latitude, location.longitude)
                    getWeatherUseCase(location.latitude, location.longitude)                    .onSuccess { weatherInfo ->
                        state = state.copy(
                            weatherInfo = weatherInfo,
                            isLoading = false,
                            error = null,
                            lastUpdatedTime = currentTimeString()
                        )
                        saveWeatherSnapshot(weatherInfo, location.latitude, location.longitude)
                    }
                    .onFailure { e ->
                        state = state.copy(
                            weatherInfo = null,
                            isLoading = false,
                            error = "Couldn't retrieve weather data: ${e.message}"
                        )
                    }
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Location unavailable. Please ensure GPS is on and permissions are granted."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadAirQuality(lat: Double, lon: Double) {
        getAirQualityUseCase(lat, lon).onSuccess { airQuality ->
            state = state.copy(airQuality = airQuality)
        }
    }

    fun loadWeatherForCity(lat: Double, lon: Double, cityName: String? = null) {
        currentCityName = cityName
        currentLat = lat
        currentLon = lon
        loadJob?.cancel()
        refreshJob?.cancel()
        loadJob = viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                error = null,
                refreshError = null
            )
            try {
                loadAirQuality(lat, lon)
                getWeatherUseCase(lat, lon)
                    .onSuccess { weatherInfo ->
                        state = state.copy(
                            weatherInfo = weatherInfo,
                            isLoading = false,
                            error = null,
                            lastUpdatedTime = currentTimeString()
                        )
                        saveWeatherSnapshot(weatherInfo, lat, lon)
                    }
                    .onFailure { e ->
                        state = state.copy(
                            weatherInfo = null,
                            isLoading = false,
                            error = "Couldn't retrieve weather data for this city."
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "Failed to load city weather: ${e.message}"
                )
            }
        }
    }

    fun refreshWeatherInfo() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            state = state.copy(
                isRefreshing = true,
                refreshError = null
            )
            try {
                val lat = currentLat
                val lon = currentLon
                if (lat != null && lon != null) {
                    refreshWithCoords(lat, lon)
                } else {
                    val location = locationTracker.getCurrentLocation()
                    if (location != null) {
                        refreshWithCoords(location.latitude, location.longitude)
                    } else {
                        state = state.copy(
                            isRefreshing = false,
                            refreshError = "Location unavailable. Unable to refresh."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isRefreshing = false,
                    refreshError = "Unable to update weather. Please try again."
                )
            }
        }
    }

    private suspend fun refreshWithCoords(lat: Double, lon: Double) {
        loadAirQuality(lat, lon)
        getWeatherUseCase(lat, lon)
            .onSuccess { weatherInfo ->
                state = state.copy(
                    weatherInfo = weatherInfo,
                    isRefreshing = false,
                    refreshError = null,
                    lastUpdatedTime = currentTimeString()
                )
            }
            .onFailure {
                state = state.copy(
                    isRefreshing = false,
                    refreshError = "Unable to update weather. Please try again."
                )
            }
    }

    fun clearRefreshError() {
        state = state.copy(refreshError = null)
    }

    private suspend fun saveWeatherSnapshot(
        weatherInfo: com.vayu.weather.domain.model.WeatherInfo,
        lat: Double,
        lon: Double
    ) {
        try {
            repository.saveWeatherSnapshot(
                WeatherHistorySnapshot(
                    cityName = currentCityName ?: "Unknown",
                    latitude = lat,
                    longitude = lon,
                    temperature = weatherInfo.current.temperature,
                    weatherCode = weatherInfo.current.weatherCode,
                    humidity = weatherInfo.current.humidity,
                    windSpeed = weatherInfo.current.windSpeed,
                    apparentTemperature = weatherInfo.current.apparentTemperature,
                    isDay = weatherInfo.current.isDay,
                    surfacePressure = weatherInfo.current.surfacePressure,
                    uvIndex = weatherInfo.daily.firstOrNull()?.uvIndex,
                    precipitationProbability = weatherInfo.daily.firstOrNull()?.precipitationProbability
                )
            )
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to save weather snapshot", e)
        }
    }

    private fun currentTimeString(): String {
        val now = LocalTime.now()
        return "Updated ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    }
}
