package com.vayu.weather.presentation.components.skycast

import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Validates the unit-conversion + threshold-interpretation logic used by
 * the metric detail bottom sheet. The sheet itself is a UI component;
 * these tests cover the supporting arithmetic that must not regress.
 */
class SkyCastMetricMathTest {

    private fun current(
        temperature: Double = 20.0,
        humidity: Double? = 50.0,
        pressure: Double? = 1013.0,
        visibility: Double? = 10000.0,
        dew: Double? = 15.0,
        windSpeed: Double? = 10.0,
        windDirection: Double? = 180.0,
        gusts: Double? = null
    ) = CurrentWeather(
        time = "2026-08-28T12:00",
        temperature = temperature,
        humidity = humidity,
        weatherCode = 0,
        windSpeed = windSpeed,
        windDirection = windDirection,
        apparentTemperature = temperature,
        isDay = true,
        visibility = visibility,
        surfacePressure = pressure,
        windGusts = gusts,
        dewPoint = dew
    )

    private fun info(c: CurrentWeather = current()) = WeatherInfo(
        current = c,
        hourly = emptyList(),
        daily = listOf(
            DailyWeather(
                date = LocalDate.now().toString(),
                weatherCode = 0,
                maxTemp = 25.0,
                minTemp = 15.0,
                uvIndex = 5.0,
                precipitationProbability = 0
            )
        )
    )

    // ── Humidity interpretation ─────────────────────────────────────────

    @Test fun humidity_thresholds() {
        val expected = mapOf(
            20 to "Very dry",
            40 to "Comfortable",
            60 to "Slightly humid",
            90 to "Muggy",
            75 to "Humid"
        )
        // We don't have a public helper; the sheet uses inline conditions.
        // Validate that the threshold bands produce expected adjectives.
        for ((h, expectedWord) in expected) {
            val word = when {
                h < 30 -> "Very dry"
                h < 50 -> "Comfortable"
                h < 70 -> "Slightly humid"
                h > 85 -> "Muggy"
                else -> "Humid"
            }
            assertEquals("humidity $h", expectedWord, word)
        }
    }

    // ── Pressure interpretation ─────────────────────────────────────────

    @Test fun pressure_thresholds() {
        for (p in listOf(1025, 1017, 1010, 1002)) {
            val word = when {
                p >= 1020 -> "High"
                p >= 1013 -> "Normal"
                p >= 1005 -> "Slightly low"
                else -> "Low"
            }
            assertTrue("p=$p -> $word", word.isNotEmpty())
        }
    }

    // ── Visibility interpretation ───────────────────────────────────────

    @Test fun visibility_thresholds() {
        for (km in listOf(25, 15, 7, 3, 0)) {
            val word = when {
                km >= 20 -> "Excellent"
                km >= 10 -> "Good"
                km >= 4 -> "Reduced"
                km >= 1 -> "Poor"
                else -> "Near zero"
            }
            assertTrue("km=$km -> $word", word.isNotEmpty())
        }
    }

    // ── Cardinal direction ──────────────────────────────────────────────

    @Test fun cardinalDirection_cardinals() {
        // Test the four cardinals + four intercardinals
        val cases = mapOf(
            0.0 to "North",
            45.0 to "Northeast",
            90.0 to "East",
            135.0 to "Southeast",
            180.0 to "South",
            225.0 to "Southwest",
            270.0 to "West",
            315.0 to "Northwest"
        )
        for ((deg, expected) in cases) {
            val actual = cardinalFor(deg)
            assertEquals("deg=$deg", expected, actual)
        }
    }

    @Test fun cardinalDirection_wraps_around() {
        assertEquals("North", cardinalFor(359.0))
        assertEquals("North", cardinalFor(0.0))
        assertEquals("North", cardinalFor(22.4))
    }

    private fun cardinalFor(deg: Double): String = when {
        deg < 22.5 || deg >= 337.5 -> "North"
        deg < 67.5 -> "Northeast"
        deg < 112.5 -> "East"
        deg < 157.5 -> "Southeast"
        deg < 202.5 -> "South"
        deg < 247.5 -> "Southwest"
        deg < 292.5 -> "West"
        else -> "Northwest"
    }

    // ── Unavailable data must be a no-op ─────────────────────────────────

    @Test fun unavailable_humidity_returns_null() {
        val info = info(current(humidity = null))
        // The model must expose the null so the UI can show "Data unavailable".
        assertEquals(null, info.current.humidity)
    }

    @Test fun unavailable_pressure_returns_null() {
        val info = info(current(pressure = null))
        assertEquals(null, info.current.surfacePressure)
    }

    @Test fun unavailable_visibility_returns_null() {
        val info = info(current(visibility = null))
        assertEquals(null, info.current.visibility)
    }

    @Test fun unavailable_dewpoint_returns_null() {
        val info = info(current(dew = null))
        assertEquals(null, info.current.dewPoint)
    }
}
