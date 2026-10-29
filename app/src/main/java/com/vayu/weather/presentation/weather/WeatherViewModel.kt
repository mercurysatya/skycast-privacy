package com.vayu.weather.presentation.weather

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationNameResolver
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.WeatherHistorySnapshot
import com.vayu.weather.domain.repository.WeatherRepository
import com.vayu.weather.domain.use_case.GetAirQualityUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import com.vayu.weather.domain.use_case.EvaluateWeatherAdvisoriesUseCase
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
    private val evaluateWeatherAdvisories: EvaluateWeatherAdvisoriesUseCase,
    val locationTracker: LocationTracker,
    val locationNameResolver: LocationNameResolver,
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

    /** Reverse-geocoding lookup for the GPS position; cancelled on a manual pick. */
    private var nameResolveJob: Job? = null

    /** Every alert ever stored; filtered down to the visible location on use. */
    private var allAlerts: List<com.vayu.weather.domain.repository.WeatherAlert> = emptyList()

    init {
        Log.d("WeatherViewModel", "ViewModel Initialized")
        // Collect alerts so the home screen can display them without a
        // separate ViewModel — the alerts screen has its own AlertsViewModel.
        viewModelScope.launch {
            repository.getActiveWeatherAlerts().collect { alerts ->
                allAlerts = alerts
                publishAlerts()
            }
        }
    }

    /**
     * An alert belongs to the location it was generated for. Without this,
     * a UV alert raised for Bangalore would show up on London's dashboard.
     * Only advisories that are still valid count as active.
     */
    private fun publishAlerts() {
        val lat = currentLat
        val lon = currentLon
        val now = System.currentTimeMillis()
        val visible = if (lat == null || lon == null) {
            allAlerts.filter { it.isActive(now) }
        } else {
            allAlerts.filter { alert ->
                val aLat = alert.latitude
                val aLon = alert.longitude
                aLat != null && aLon != null &&
                    Math.abs(aLat - lat) <= 1.0 &&
                    Math.abs(aLon - lon) <= 1.0 &&
                    alert.isActive(now)
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
                    val sameCoordinates = currentLat == location.latitude &&
                        currentLon == location.longitude
                    // A GPS fix is the device's own location, not a picked city.
                    // A name that already belongs to these very coordinates stays
                    // valid; a name from different coordinates is stale and must go.
                    currentCityName = if (sameCoordinates) currentCityName else null
                    // Fall back to the last known city name so the header shows
                    // a real place name immediately instead of waiting for the
                    // asynchronous reverse-geocode (which can fail on devices
                    // without a Geocoder backend).
                    if (currentCityName == null) {
                        currentCityName = settingsManager.getLastCity()
                    }
                    // Captured before the asynchronous lookup can change it, so
                    // the location is persisted exactly once with a known name.
                    val nameToPersist = currentCityName
                    currentLat = location.latitude
                    currentLon = location.longitude
                    if (!sameCoordinates) {
                        state = state.copy(regionName = null)
                    }
                    publishAlerts()
                    dropStaleData(switching)
                    // Resolve the place name in the background so the header can
                    // show the real city instead of the "current location" fallback.
                    resolveNameForLocation(location.latitude, location.longitude)
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
                            evaluateAdvisories(weatherInfo, location.latitude, location.longitude, nameToPersist)
                            // Persist the GPS fix as well: a device that only ever
                            // used GPS otherwise never wrote LAST_LAT/LON, so a
                            // later launch without a fix (revoked permission,
                            // airplane mode, GPS outage) had no fallback at all.
                            // Guard: this coroutine may outlive this load if the
                            // user picked another city first — never overwrite.
                            if (currentLat == location.latitude && currentLon == location.longitude) {
                                viewModelScope.launch {
                                    settingsManager.setLastLocation(
                                        location.latitude, location.longitude, nameToPersist
                                    )
                                }
                            }
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
                            evaluateAdvisories(weatherInfo, fallbackLat, fallbackLon, currentCityName)
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

    fun loadWeatherForCity(
        lat: Double,
        lon: Double,
        cityName: String? = null,
        regionName: String? = null
    ) {
        // Only treat this as a location switch when the coordinates actually
        // change — re-selecting the same city should keep its data on screen.
        val switching = state.weatherInfo != null && (currentLat != lat || currentLon != lon)
        // A manual pick owns the title: cancel a GPS name lookup that is still
        // in flight so a slow reverse-geocode cannot overwrite the chosen city.
        nameResolveJob?.cancel()
        nameResolveJob = null
        currentCityName = cityName
        currentLat = lat
        currentLon = lon
        state = state.copy(regionName = regionName?.takeIf { it.isNotBlank() })
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
                        evaluateAdvisories(weatherInfo, lat, lon, currentCityName)
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
        // A refresh must not lose the place name: only look one up when none is
        // known yet (GPS position whose first lookup failed, for example).
        resolveNameForLocation(lat, lon)
        getWeatherUseCase(lat, lon)
            .onSuccess { weatherInfo ->
                state = state.copy(
                    weatherInfo = weatherInfo,
                    isRefreshing = false,
                    refreshError = null,
                    lastUpdatedTime = currentTimeString()
                )
                // The forecast may have changed — keep the advisories in sync.
                // The upsert is idempotent, so this cannot duplicate anything.
                evaluateAdvisories(weatherInfo, lat, lon, currentCityName)
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

    /**
     * Reverse-geocodes the given coordinates so the header can show the real
     * place name instead of the localized "current location" fallback.
     *
     * - Runs off the main thread (the resolver does the lookup on IO).
     * - A result is only applied when it still belongs to the location that is
     *   on screen, so a slow lookup can never overwrite a newer position.
     * - A name that is already known is never looked up again.
     * - When no name can be resolved the fallback simply stays — no error is
     *   shown and nothing is suppressed.
     */
    private fun resolveNameForLocation(lat: Double, lon: Double) {
        if (currentCityName != null) return
        nameResolveJob?.cancel()
        nameResolveJob = viewModelScope.launch {
            val resolved = try {
                locationNameResolver.resolveName(lat, lon)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("WeatherViewModel", "Location name lookup failed: ${e.message}")
                null
            }
            // Stale guard: the user may have moved or picked a city meanwhile.
            if (currentLat != lat || currentLon != lon) return@launch
            val city = resolved?.city?.takeIf { it.isNotBlank() } ?: return@launch
            currentCityName = city
            state = state.copy(regionName = resolved.region?.takeIf { it.isNotBlank() })
            // Persist it so the next launch (and the widget/notification
            // workers) can show the same name without another lookup.
            viewModelScope.launch {
                settingsManager.setLastLocation(lat, lon, city)
            }
        }
    }

    /**
     * Keeps the stored advisories in sync with the freshly loaded forecast.
     *
     * The write is an idempotent upsert keyed on the advisory's identity, so
     * refreshing the weather can never pile up duplicate cards. No
     * notification is sent from here — that is the background worker's job.
     */
    private suspend fun evaluateAdvisories(
        weatherInfo: com.vayu.weather.domain.model.WeatherInfo,
        latitude: Double,
        longitude: Double,
        cityName: String?
    ) {
        try {
            evaluateWeatherAdvisories(
                weatherInfo = weatherInfo,
                latitude = latitude,
                longitude = longitude,
                cityName = cityName
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // An advisory problem must never break the weather screen.
            Log.w("WeatherViewModel", "Advisory evaluation failed: ${e.message}")
        }
    }

    private suspend fun saveWeatherSnapshot(        weatherInfo: com.vayu.weather.domain.model.WeatherInfo,
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
