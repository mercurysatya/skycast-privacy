package com.vayu.weather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vayu.weather.domain.model.*
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WeatherDashboard
import com.vayu.weather.presentation.weather.WeatherState
import org.junit.Rule
import org.junit.Test

class WeatherDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleWeatherInfo = WeatherInfo(
        current = CurrentWeather(
            time = "2024-01-15T14:00",
            temperature = 22.5,
            humidity = 65.0,
            weatherCode = 0,
            windSpeed = 15.0,
            windDirection = 180.0,
            apparentTemperature = 20.0,
            isDay = true,
            visibility = 10000.0,
            surfacePressure = 1013.0
        ),
        hourly = (0 until 24).map { hour ->
            HourlyWeather(
                time = "2024-01-15T${hour.toString().padStart(2, '0')}:00",
                temperature = 20.0 + hour * 0.5,
                weatherCode = 0,
                humidity = 60.0,
                pressure = 1013.0,
                windSpeed = 10.0
            )
        },
        daily = listOf(
            DailyWeather(
                date = "2024-01-15",
                weatherCode = 0,
                maxTemp = 25.0,
                minTemp = 18.0,
                uvIndex = 5.0,
                precipitationProbability = 10,
                sunrise = "07:15",
                sunset = "16:45"
            )
        )
    )

    @Test
    fun dashboard_displays_temperature() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = sampleWeatherInfo,
                    isLoading = false
                ),
                settings = SettingsState(
                    temperatureUnit = TemperatureUnit.CELSIUS
                )
            )
        }
        composeTestRule.onNodeWithText("22°").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_city_name() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = sampleWeatherInfo,
                    isLoading = false
                ),
                cityName = "New York"
            )
        }
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_humidity() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = sampleWeatherInfo,
                    isLoading = false
                )
            )
        }
        composeTestRule.onNodeWithText("65%").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_loading_state() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = null,
                    isLoading = true
                )
            )
        }
        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_error_state() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = null,
                    isLoading = false,
                    error = "Something went wrong"
                )
            )
        }
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_daily_forecast() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = sampleWeatherInfo,
                    isLoading = false
                )
            )
        }
        composeTestRule.onNodeWithText("7-Day Forecast").assertIsDisplayed()
    }

    @Test
    fun dashboard_displays_hourly_forecast_header() {
        composeTestRule.setContent {
            WeatherDashboard(
                state = WeatherState(
                    weatherInfo = sampleWeatherInfo,
                    isLoading = false
                )
            )
        }
        composeTestRule.onNodeWithText("Hourly Forecast").assertIsDisplayed()
    }
}
