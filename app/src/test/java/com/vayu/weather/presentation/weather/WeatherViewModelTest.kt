package com.vayu.weather.presentation.weather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationNameResolver
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.location.ResolvedPlaceName
import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.repository.WeatherRepository
import com.vayu.weather.domain.use_case.GetAirQualityUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import com.vayu.weather.domain.use_case.EvaluateWeatherAdvisoriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getWeatherUseCase: GetWeatherUseCase
    private lateinit var getAirQualityUseCase: GetAirQualityUseCase
    private lateinit var locationTracker: LocationTracker
    private lateinit var locationNameResolver: LocationNameResolver
    private lateinit var settingsManager: SettingsManager
    private lateinit var evaluateWeatherAdvisories: EvaluateWeatherAdvisoriesUseCase
    private lateinit var repository: WeatherRepository
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getWeatherUseCase = mock()
        getAirQualityUseCase = mock()
        locationTracker = mock()
        locationNameResolver = mock()
        settingsManager = mock()
        evaluateWeatherAdvisories = mock()
        repository = mock()
        viewModel = WeatherViewModel(
            getWeatherUseCase = getWeatherUseCase,
            getAirQualityUseCase = getAirQualityUseCase,
            repository = repository,
            evaluateWeatherAdvisories = evaluateWeatherAdvisories,
            locationTracker = locationTracker,
            locationNameResolver = locationNameResolver,
            settingsManager = settingsManager
        )
        // The init block collects the alert flow; stub it with an argument
        // matcher so advanceUntilIdle in later tests cannot NPE on null.
        whenever(repository.getWeatherAlerts(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.getActiveWeatherAlerts(any(), any())).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading false with no data`() {
        assertEquals(false, viewModel.state.isLoading)
        assertNull(viewModel.state.weatherInfo)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `loadWeatherForCity sets city name`() {
        viewModel.loadWeatherForCity(51.5, -0.1, "London")
        assertEquals("London", viewModel.currentCityName)
    }

    @Test
    fun `loadWeatherForCity sets coordinates`() {
        viewModel.loadWeatherForCity(51.5, -0.1, "London")
        assertEquals(51.5, viewModel.currentLat!!, 0.001)
        assertEquals(-0.1, viewModel.currentLon!!, 0.001)
    }

    @Test
    fun `clearRefreshError clears error state`() {
        viewModel.clearRefreshError()
        assertNull(viewModel.state.refreshError)
    }

    // ------------------------------------------------------------------
    // F-NEW-1 regression: a GPS-only user must get LAST_LAT/LON persisted on
    // the GPS-success path, otherwise a later launch without a fix (revoked
    // permission, airplane mode, GPS outage) has no fallback data at all.
    // ------------------------------------------------------------------

    private fun minimalWeatherInfo() = WeatherInfo(
        current = CurrentWeather(
            time = "2026-10-17T19:53",
            temperature = 25.0,
            humidity = null,
            weatherCode = 0,
            windSpeed = null,
            windDirection = null,
            apparentTemperature = null,
            isDay = false
        ),
        hourly = emptyList(),
        daily = listOf(
            DailyWeather(
                date = "2026-10-17",
                weatherCode = 0,
                maxTemp = 30.0,
                minTemp = 25.0,
                uvIndex = null,
                precipitationProbability = null
            )
        )
    )

    @Test
    fun `gps-based load persists location fallback metadata`() = runTest(testDispatcher) {
        whenever(getAirQualityUseCase.invoke(any(), any()))
            .thenReturn(Result.failure(Exception("no aqi in test")))

        val location = mock<android.location.Location>()
        whenever(location.latitude).thenReturn(12.9716)
        whenever(location.longitude).thenReturn(77.5946)
        whenever(locationTracker.getCurrentLocation()).thenReturn(location)
        whenever(getWeatherUseCase.invoke(any(), any()))
            .thenReturn(Result.success(minimalWeatherInfo()))

        viewModel.loadWeatherInfo()
        advanceUntilIdle()

        assertEquals(12.9716, viewModel.currentLat!!, 0.001)
        assertEquals(77.5946, viewModel.currentLon!!, 0.001)
        // The fix itself is persisted for future offline fallbacks.
        verify(settingsManager).setLastLocation(eq(12.9716), eq(77.5946), isNull())
    }

    @Test
    fun `city switched into later is NOT overwritten by stale gps load`() = runTest(testDispatcher) {
        whenever(getAirQualityUseCase.invoke(any(), any()))
            .thenReturn(Result.failure(Exception("no aqi in test")))

        val location = mock<android.location.Location>()
        whenever(location.latitude).thenReturn(12.9716)
        whenever(location.longitude).thenReturn(77.5946)
        whenever(locationTracker.getCurrentLocation()).thenReturn(location)
        whenever(getWeatherUseCase.invoke(any(), any()))
            .thenReturn(Result.success(minimalWeatherInfo()))

        viewModel.loadWeatherInfo()
        // The user picks another city while the GPS load is still running;
        // neither the snapshot nor the persistence guard may clobber the
        // picked city's coordinates.
        viewModel.loadWeatherForCity(51.5074, -0.1278, "London")
        advanceUntilIdle()

        assertEquals(51.5074, viewModel.currentLat!!, 0.001)
        // The GPS persistence guard sees currentLat != gps fix and must not write.
        verify(settingsManager, never()).setLastLocation(eq(12.9716), eq(77.5946), any())
        // The picked city is still persisted.
        verify(settingsManager).setLastLocation(eq(51.5074), eq(-0.1278), eq("London"))
    }

    // ------------------------------------------------------------------
    // Location-name regression: the header used to stay on "Current Location"
    // forever because a GPS fix never resolved a place name (currentCityName
    // was hard-reset to null and nothing reverse-geocoded the coordinates).
    // ------------------------------------------------------------------

    private suspend fun stubGps(lat: Double, lon: Double) {
        val location = mock<android.location.Location>()
        whenever(location.latitude).thenReturn(lat)
        whenever(location.longitude).thenReturn(lon)
        whenever(locationTracker.getCurrentLocation()).thenReturn(location)
        whenever(getAirQualityUseCase.invoke(any(), any()))
            .thenReturn(Result.failure(Exception("no aqi in test")))
        whenever(getWeatherUseCase.invoke(any(), any()))
            .thenReturn(Result.success(minimalWeatherInfo()))
    }

    @Test
    fun `gps location resolves the place name for the header`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenReturn(ResolvedPlaceName(city = "Srikakulam", region = "Andhra Pradesh"))

        viewModel.loadWeatherInfo()
        advanceUntilIdle()

        assertEquals("Srikakulam", viewModel.currentCityName)
        assertEquals("Andhra Pradesh", viewModel.state.regionName)
    }

    @Test
    fun `resolved place name is persisted for the next launch`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenReturn(ResolvedPlaceName(city = "Srikakulam", region = "Andhra Pradesh"))

        viewModel.loadWeatherInfo()
        advanceUntilIdle()

        verify(settingsManager).setLastLocation(eq(18.4865), eq(84.1061), eq("Srikakulam"))
    }

    @Test
    fun `unresolvable place keeps the current-location fallback without error`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any())).thenReturn(null)

        viewModel.loadWeatherInfo()
        advanceUntilIdle()

        // No name -> the UI keeps showing the localized "Current Location"
        // fallback, but the weather itself is still on screen.
        assertNull(viewModel.currentCityName)
        assertNull(viewModel.state.error)
        assertEquals(18.4865, viewModel.currentLat!!, 0.001)
    }

    @Test
    fun `failing place lookup does not break the weather load`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenThrow(IllegalStateException("geocoder backend unavailable"))

        viewModel.loadWeatherInfo()
        advanceUntilIdle()

        assertNull(viewModel.currentCityName)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `manual city selection is not overwritten by a late place lookup`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenReturn(ResolvedPlaceName(city = "Srikakulam", region = "Andhra Pradesh"))

        viewModel.loadWeatherInfo()
        // The user picks a city while the reverse-geocode is still running.
        viewModel.loadWeatherForCity(51.5074, -0.1278, "London", "England")
        advanceUntilIdle()

        assertEquals("London", viewModel.currentCityName)
        assertEquals("England", viewModel.state.regionName)
    }

    @Test
    fun `place lookup of a stale position does not overwrite the current one`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenReturn(ResolvedPlaceName(city = "Srikakulam", region = "Andhra Pradesh"))

        viewModel.loadWeatherInfo()
        // Location changed before the lookup came back.
        viewModel.loadWeatherForCity(12.9716, 77.5946, "Bengaluru", "Karnataka")
        advanceUntilIdle()

        assertEquals("Bengaluru", viewModel.currentCityName)
        assertEquals("Karnataka", viewModel.state.regionName)
    }

    @Test
    fun `a known place name is not looked up again`() = runTest(testDispatcher) {
        stubGps(18.4865, 84.1061)
        whenever(locationNameResolver.resolveName(any(), any()))
            .thenReturn(ResolvedPlaceName(city = "Srikakulam", region = "Andhra Pradesh"))

        viewModel.loadWeatherInfo()
        advanceUntilIdle()
        val lookupsAfterFirstLoad = mockingDetails(locationNameResolver).invocations.size

        viewModel.refreshWeatherInfo()
        advanceUntilIdle()

        // The refresh keeps the name and does not trigger another lookup.
        assertEquals("Srikakulam", viewModel.currentCityName)
        assertEquals(lookupsAfterFirstLoad, mockingDetails(locationNameResolver).invocations.size)
    }
}
