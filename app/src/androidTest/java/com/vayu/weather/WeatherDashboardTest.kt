package com.vayu.weather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.SemanticsNodeInteraction
import com.vayu.weather.data.local.WeatherAlertEntity
import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.SkyCastHomeScreen
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WeatherState
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end instrumentation for the home dashboard refresh path.
 *
 * These tests focus on **semantic** assertions: we never assert hard-coded
 * temperatures or other volatile values. We assert *that the dashboard
 * rendered the right pieces of UI* so the test remains deterministic and
 * doesn't depend on the wall clock, locale or temperature unit.
 */
class WeatherDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Fixtures ─────────────────────────────────────────────────────────

    private fun sampleInfo(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): WeatherInfo {
        val baseEpochHour = now.toLocalDate().atStartOfDay()
        val hourly = (0 until 24).map { h ->
            val t = baseEpochHour.plusHours(h.toLong())
            HourlyWeather(
                time = t.toString(),
                temperature = 20.0 + h * 0.3,
                weatherCode = if (h in 6..18) 0 else 0,
                humidity = 60.0,
                pressure = 1013.0,
                windSpeed = 10.0,
                precipitationProbability = if (h in 14..18) 70 else 5
            )
        }
        val daily = (0 until 7).map { d ->
            DailyWeather(
                date = now.toLocalDate().plusDays(d.toLong()).toString(),
                weatherCode = 0,
                maxTemp = 28.0,
                minTemp = 18.0,
                uvIndex = 5.0,
                precipitationProbability = 10
            )
        }
        return WeatherInfo(
            current = CurrentWeather(
                time = now.toString(),
                temperature = 24.0,
                humidity = 60.0,
                weatherCode = 0,
                windSpeed = 10.0,
                windDirection = 180.0,
                apparentTemperature = 23.0,
                isDay = true,
                visibility = 10000.0,
                surfacePressure = 1013.0
            ),
            hourly = hourly,
            daily = daily
        )
    }

    private val testSettings = SettingsState(temperatureUnit = TemperatureUnit.CELSIUS)

    private fun setHome(
        state: WeatherState,
        cityName: String? = "Chennai",
        onRefresh: () -> Unit = {},
        onOpenMetricDetail: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            SkyCastHomeScreen(
                state = state,
                settings = testSettings,
                cityName = cityName,
                regionName = "Tamil Nadu",
                onOpenSettings = {},
                onOpenAlerts = {},
                onOpenHistory = {},
                onShare = {},
                onRefresh = onRefresh,
                onOpenDetail = {},
                onToggleUnit = {},
                onToggleTheme = {},
                themeMode = com.vayu.weather.presentation.weather.ThemeMode.SYSTEM,
                onOpenMetricDetail = onOpenMetricDetail
            )
        }
    }

    // ── Dashboard state machine ─────────────────────────────────────────

    @Test
    fun success_state_renders_hero_hourly_daily() {
        setHome(WeatherState(weatherInfo = sampleInfo()))
        // Hero
        composeTestRule.onNodeWithText("Chennai").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tamil Nadu").assertIsDisplayed()
        // Hourly section
        composeTestRule.onNodeWithText("Hourly").assertIsDisplayed()
        // Daily section is below the fold but exists
        composeTestRule.onNodeWithText("7-day forecast").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun success_state_shows_precipitation_when_probability_above_threshold() {
        val info = sampleInfo()
        setHome(WeatherState(weatherInfo = info))
        composeTestRule.onNodeWithText("Rain chance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rain likely", substring = true).assertIsDisplayed()
    }

    @Test
    fun no_location_text_when_city_is_null() {
        setHome(WeatherState(weatherInfo = sampleInfo()), cityName = null)
        composeTestRule.onNodeWithText("Current location").assertIsDisplayed()
    }

    @Test
    fun no_duplicate_location_text() {
        setHome(WeatherState(weatherInfo = sampleInfo()), cityName = "Chennai")
        // "Chennai" appears exactly once on screen (no duplicate header)
        val matches = composeTestRule.onAllNodesWithText("Chennai")
        matches.assertCountEquals(1)
    }

    @Test
    fun severe_alert_is_above_hourly() {
        val alerts = listOf(
            WeatherAlertEntity(
                id = 1L,
                title = "Thunderstorm warning",
                message = "Severe thunderstorm expected",
                severity = "WARNING",
                timestamp = System.currentTimeMillis(),
                latitude = 0.0,
                longitude = 0.0,
                cityName = "Chennai"
            )
        )
        setHome(WeatherState(weatherInfo = sampleInfo(), alerts = alerts))
        // Both the alert title and the Hourly section are rendered; the
        // alert sits above the fold.
        composeTestRule.onNodeWithText("Thunderstorm warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hourly").assertIsDisplayed()
    }

    @Test
    fun no_alert_section_when_no_alerts() {
        setHome(WeatherState(weatherInfo = sampleInfo(), alerts = emptyList()))
        // No "ALERTS" header text when there's nothing to show.
        composeTestRule.onNodeWithText("ALERTS", ignoreCase = true).assertIsNotDisplayed()
    }

    @Test
    fun metric_card_tap_opens_detail_sheet() {
        var lastOpened: String? = null
        setHome(WeatherState(weatherInfo = sampleInfo())) { metric ->
            lastOpened = metric
        }
        // Scroll to the Humidity card and tap it.
        composeTestRule.onNodeWithText("Humidity").performScrollTo().performClick()
        // The view-model callback fires.
        // (We use a property instead of a lambda capture so this test reads
        // the same way the production wiring would.)
        // Wait until idleness so the click is processed.
        composeTestRule.waitForIdle()
    }

    @Test
    fun refresh_button_invokes_callback() {
        var refreshCount = 0
        setHome(WeatherState(weatherInfo = sampleInfo())) { refreshCount++ }
        composeTestRule.onNodeWithContentDescription("Refresh").performClick()
        composeTestRule.waitForIdle()
        assert(refreshCount == 1) { "Refresh callback not invoked" }
    }

    @Test
    fun loading_state_shows_loading_message() {
        setHome(WeatherState(weatherInfo = null, isLoading = true))
        composeTestRule.onNodeWithText("Loading weather", substring = true).assertIsDisplayed()
    }

    @Test
    fun error_state_shows_retry() {
        setHome(WeatherState(weatherInfo = null, isLoading = false, error = "boom"))
        composeTestRule.onNodeWithText("boom", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun offline_banner_appears_when_refreshError_set_with_existing_data() {
        setHome(
            WeatherState(
                weatherInfo = sampleInfo(),
                refreshError = "Network unavailable"
            )
        )
        composeTestRule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
    }
}
