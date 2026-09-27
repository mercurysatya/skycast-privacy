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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
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

    /** Every alert ever stored; filtered down to the visible location on use. */
    private var allAlerts: List<com.vayu.weather.domain.repository.WeatherAlert> = emptyList()

    init {
        Log.d("WeatherViewModel", "ViewModel Initialized")
        // Collect alerts so the home screen can display them without a
        // separate ViewModel — the alerts screen has its own AlertsViewModel.
        viewModelScope.launch {
            repository.getWeatherAlerts().collect { alerts ->
                allAlerts = alerts
                publishAlerts()
            }
        }
    }

    /**
     * An alert belongs to the location it was generated for. Without this,
     * a UV alert raised for Bangalore would show up on London's dashboard.
     */
    private fun publishAlerts() {
        val lat = currentLat
        val lon = currentLon
        val visible = if (lat == null || lon == null) {
            allAlerts
        } else {
            allAlerts.filter { alert ->
                val aLat = alert.latitude
                val aLon = alert.longitude
                aLat != null && aLon != null &&
                    Math.abs(aLat - lat) <= 1.0 &&
                    Math.abs(aLon - lon) <= 1.0
            }
        }
        state = state.copy(alerts = visible)
    }

    private fun friendlyError(e: Throwable): String {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.io.IOException -> "Network error. Check your connection and try again."
            is java.util.concurrent.TimeoutException -> "Request timed out. Please try again."
            is java.net.UnknownHostException -> "No internet connection. Enable Wi-Fi or mobile data."
            is CancellationException -> "Request cancelled."
            else -> "Couldn't retrieve weather data. Please try again."
        }
    }

    fun setCityName(name: String?) {
        currentCityName = name
    }

    /**
     * Drops data that belongs to a different location so it can never be shown
     * under the location currently being loaded, and clears the auxiliary
     * values (AQI / vs-yesterday) that are re-fetched with every load.
     */
    private fun dropStaleData(switchingLocation: Boolean) {
        if (switchingLocation) {
            state = state.copy(
                weatherInfo = null,
                airQuality = null,
                previousDayTempC = null
            )
        } else if (state.weatherInfo == null) {
            state = state.copy(airQuality = null, previousDayTempC = null)
        }
    }

    /**
     * A failed load must not blank data that is already valid on screen: keep
     * it and surface the failure as a refresh error (offline banner) instead.
     * Only an empty dashboard falls back to the full-screen error state.
     */
    private fun handleLoadFailure(e: Throwable) {
        val message = friendlyError(e)
        state = if (state.weatherInfo != null) {
            state.copy(isLoading = false, refreshError = message)
        } else {
            state.copy(weatherInfo = null, isLoading = false, error = message)
        }
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
                val fallbackLat = currentLat ?: settingsManager.getLastLat()
                val fallbackLon = currentLon ?: settingsManager.getLastLon()
                if (location != null) {
                    val switching = state.weatherInfo != null &&
                        (currentLat != location.latitude || currentLon != location.longitude)
                    // A GPS fix is the device's own location, not a picked city.
                    currentCityName = null
                    currentLat = location.latitude
                    currentLon = location.longitude
                    publishAlerts()
                    dropStaleData(switching)
                    loadAirQuality(location.latitude, location.longitude)
                    loadPreviousDayTemp(location.latitude, location.longitude)
                    getWeatherUseCase(location.latitude, location.longitude)
                        .onSuccess { weatherInfo ->
                            state = state.copy(
                                weatherInfo = weatherInfo,
                                isLoading = false,
                                error = null,
                                lastUpdatedTime = currentTimeString()
                            )
                            saveWeatherSnapshot(weatherInfo, location.latitude, location.longitude)
                        }
                        .onFailure { e -> handleLoadFailure(e) }
                } else if (fallbackLat != null && fallbackLon != null) {
                    // GPS unavailable — fall back to the last successfully loaded location
                    // so the dashboard keeps showing real data instead of an error screen
                    val hadNoLocation = currentLat == null
                    if (hadNoLocation) currentCityName = settingsManager.getLastCity()
                    currentLat = fallbackLat
                    currentLon = fallbackLon
                    publishAlerts()
                    dropStaleData(state.weatherInfo != null && hadNoLocation)
                    loadAirQuality(fallbackLat, fallbackLon)
                    loadPreviousDayTemp(fallbackLat, fallbackLon)
                    getWeatherUseCase(fallbackLat, fallbackLon)
                        .onSuccess { weatherInfo ->
                            state = state.copy(
                                weatherInfo = weatherInfo,
                                isLoading = false,
                                error = null,
                                lastUpdatedTime = currentTimeString()
                            )
                            saveWeatherSnapshot(weatherInfo, fallbackLat, fallbackLon)
                            // Persist successful location so future refreshes can fall back to it
                            viewModelScope.launch { settingsManager.setLastLocation(
                                fallbackLat, fallbackLon, currentCityName
                            ) }
                        }
                        .onFailure { e -> handleLoadFailure(e) }
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Location unavailable. Please ensure GPS is on and permissions are granted."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleLoadFailure(e)
            }
        }
    }

    private suspend fun loadAirQuality(lat: Double, lon: Double) {
        getAirQualityUseCase(lat, lon).onSuccess { airQuality ->
            state = state.copy(airQuality = airQuality)
        }
    }

    private suspend fun loadPreviousDayTemp(lat: Double, lon: Double) {
        try {
            val snapshot = repository.getYesterdaySnapshot(lat, lon)
            state = state.copy(previousDayTempC = snapshot?.temperature)
        } catch (_: Exception) {
            // Non-fatal — the pill simply won't appear
        }
    }

    fun loadWeatherForCity(lat: Double, lon: Double, cityName: String? = null) {
        // Only treat this as a location switch when the coordinates actually
        // change — re-selecting the same city should keep its data on screen.
        val switching = state.weatherInfo != null && (currentLat != lat || currentLon != lon)
        currentCityName = cityName
        currentLat = lat
        currentLon = lon
        publishAlerts()
        loadJob?.cancel()
        refreshJob?.cancel()
        loadJob = viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                error = null,
                refreshError = null
            )
            dropStaleData(switching)
            try {
                loadAirQuality(lat, lon)
                loadPreviousDayTemp(lat, lon)
                getWeatherUseCase(lat, lon)
                    .onSuccess { weatherInfo ->
                        state = state.copy(
                            weatherInfo = weatherInfo,
                            isLoading = false,
                            error = null,
                            lastUpdatedTime = currentTimeString()
                        )
                        saveWeatherSnapshot(weatherInfo, lat, lon)
                        // Persist successful location so future refreshes can fall back to it
                        viewModelScope.launch { settingsManager.setLastLocation(
                            lat, lon, currentCityName
                        ) }
                    }
                    .onFailure { e -> handleLoadFailure(e) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleLoadFailure(e)
            }
        }
    }

    fun refreshWeatherInfo() {
        // The error screen surfaces `state.error`, which only full loads write.
        // A plain refresh would leave that screen stuck, so route empty-state
        // retries through a full load of the location the user is actually
        // looking at — a picked city must retry that city, not fall back to GPS.
        if (state.weatherInfo == null) {
            val lat = currentLat
            val lon = currentLon
            if (lat != null && lon != null) {
                loadWeatherForCity(lat, lon, currentCityName)
            } else {
                loadWeatherInfo()
            }
            return
        }
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
                    refreshError = friendlyError(e)
                )
            }
        }
    }

    private suspend fun refreshWithCoords(lat: Double, lon: Double) {
        loadAirQuality(lat, lon)
        loadPreviousDayTemp(lat, lon)
        getWeatherUseCase(lat, lon)
            .onSuccess { weatherInfo ->
                state = state.copy(
                    weatherInfo = weatherInfo,
                    isRefreshing = false,
                    refreshError = null,
                    lastUpdatedTime = currentTimeString()
                )
                viewModelScope.launch { settingsManager.setLastLocation(lat, lon, currentCityName) }
            }
            .onFailure { e ->
                state = state.copy(
                    isRefreshing = false,
                    refreshError = friendlyError(e)
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

    // On this day - get historical weather for today's month/day
    fun getOnThisDayHistory(): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>> {
        val monthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"))
        return repository.getWeatherHistoryForLocationAndMonthDay(currentLat ?: 0.0, currentLon ?: 0.0, monthDay)
    }
}
