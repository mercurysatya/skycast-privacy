package com.vayu.weather.presentation.weather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.use_case.GetAirQualityUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getWeatherUseCase: GetWeatherUseCase
    private lateinit var getAirQualityUseCase: GetAirQualityUseCase
    private lateinit var locationTracker: LocationTracker
    private lateinit var settingsManager: SettingsManager
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getWeatherUseCase = mock()
        getAirQualityUseCase = mock()
        locationTracker = mock()
        settingsManager = mock()
        viewModel = WeatherViewModel(
            getWeatherUseCase = getWeatherUseCase,
            getAirQualityUseCase = getAirQualityUseCase,
            locationTracker = locationTracker,
            settingsManager = settingsManager
        )
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
        assertEquals(51.5, viewModel.currentLat!!)
        assertEquals(-0.1, viewModel.currentLon!!)
    }

    @Test
    fun `clearRefreshError clears error state`() {
        viewModel.clearRefreshError()
        assertNull(viewModel.state.refreshError)
    }
}
