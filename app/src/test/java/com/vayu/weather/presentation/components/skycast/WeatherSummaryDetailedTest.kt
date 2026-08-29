package com.vayu.weather.presentation.components.skycast

import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherSummaryDetailedTest {

    private fun current(
        temp: Double = 25.0,
        apparent: Double? = 27.0,
        code: Int = 0,
        isDay: Boolean = true,
        humidity: Double? = 50.0,
        wind: Double? = 8.0
    ) = CurrentWeather(
        time = "2026-08-28T12:00",
        temperature = temp,
        humidity = humidity,
        weatherCode = code,
        windSpeed = wind,
        windDirection = 180.0,
        apparentTemperature = apparent,
        isDay = isDay
    )

    private fun daily(max: Double = 30.0, min: Double = 20.0, uv: Double = 5.0) = listOf(
        DailyWeather(
            date = LocalDate.now().toString(),
            weatherCode = 0,
            maxTemp = max,
            minTemp = min,
            uvIndex = uv,
            precipitationProbability = 10
        )
    )

    @Test
    fun detailedSummaryIncludesHighAndLow() {
        val info = WeatherInfo(current = current(), hourly = emptyList(), daily = daily(max = 32.0, min = 27.0))
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        assertTrue("primary should mention high 32°: ${result.primary}", result.primary.contains("32°"))
        assertTrue("primary should mention low 27°: ${result.primary}", result.primary.contains("27°"))
    }

    @Test
    fun detailedSummaryWithUpcomingRainMentionsTiming() {
        val now = LocalDateTime.now()
        val hourly = listOf(
            HourlyWeather(time = now.plusHours(2).toString(), temperature = 22.0, weatherCode = 61, humidity = 50.0, pressure = 1013.0, windSpeed = 8.0, precipitationProbability = 60)
        )
        val info = WeatherInfo(current = current(code = 0), hourly = hourly, daily = daily())
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        assertTrue("expected rain mention in: ${result.primary}", result.primary.contains("rain", ignoreCase = true))
    }

    @Test
    fun detailedSummaryOvercastDoesNotMentionClearSkies() {
        val info = WeatherInfo(current = current(code = 3), hourly = emptyList(), daily = daily())
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        assertFalse(
            "Overcast must not claim clear skies: ${result.primary}",
            result.primary.contains("clear skies", ignoreCase = true)
        )
    }

    @Test
    fun detailedSummaryThunderstormDoesNotMentionSunny() {
        val info = WeatherInfo(current = current(code = 95), hourly = emptyList(), daily = daily())
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        assertFalse(
            "Thunderstorm must not claim sunny: ${result.primary}",
            result.primary.contains("sunny", ignoreCase = true)
        )
    }

    @Test
    fun detailedSummaryHighUvAddsSunscreenHint() {
        val info = WeatherInfo(current = current(), hourly = emptyList(), daily = daily(uv = 8.0))
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        assertNotNull(result.secondary)
        assertTrue(
            "High UV should produce sunscreen hint: ${result.secondary}",
            result.secondary!!.contains("sunscreen", ignoreCase = true)
        )
    }

    @Test
    fun detailedSummaryFahrenheitUsesFahrenheitNumbers() {
        val info = WeatherInfo(current = current(), hourly = emptyList(), daily = daily(max = 32.0, min = 27.0))
        val result = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = false)
        // 32°C = ~90°F, 27°C = ~81°F
        assertTrue("Expected 90° (F) in: ${result.primary}", result.primary.contains("90°"))
        assertTrue("Expected 81° (F) in: ${result.primary}", result.primary.contains("81°"))
    }
}
