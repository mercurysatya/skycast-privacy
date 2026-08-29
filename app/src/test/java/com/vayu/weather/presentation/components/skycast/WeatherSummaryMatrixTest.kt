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

/**
 * Comprehensive contradiction tests for the deterministic
 * [WeatherSummaryEngine.summarizeDetailed] output.
 *
 * Every common weather code is exercised. The summary must never
 * combine phrases that disagree with the underlying data (e.g. "Clear
 * skies" with a "Heavy rain" probability).
 */
class WeatherSummaryMatrixTest {

    private fun current(
        code: Int,
        isDay: Boolean = true,
        temp: Double = 25.0,
        apparent: Double? = null,
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

    private fun daily(uv: Double = 5.0) = listOf(
        DailyWeather(
            date = LocalDate.now().toString(),
            weatherCode = 0,
            maxTemp = 30.0,
            minTemp = 20.0,
            uvIndex = uv,
            precipitationProbability = 10
        )
    )

    private fun info(
        current: CurrentWeather,
        hourly: List<HourlyWeather> = emptyList()
    ) = WeatherInfo(
        current = current,
        hourly = hourly,
        daily = daily()
    )

    private val matrix = listOf(
        0 to "clear",
        1 to "partly cloudy",
        2 to "partly cloudy",
        3 to "overcast",
        45 to "fog",
        48 to "fog",
        51 to "drizzle",
        53 to "drizzle",
        55 to "drizzle",
        61 to "rain",
        63 to "rain",
        65 to "rain",
        71 to "snow",
        73 to "snow",
        75 to "snow",
        80 to "rain",
        81 to "rain",
        82 to "rain",
        95 to "thunderstorm",
        96 to "thunderstorm",
        99 to "thunderstorm"
    )

    // ── Condition claims must match the WMO code ─────────────────────────

    @Test
    fun no_code_claims_clear_when_not_clear() {
        val nonClear = listOf(1, 2, 3, 45, 51, 61, 71, 80, 95)
        for (code in nonClear) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code, temp = 15.0, humidity = 80.0)),
                isCelsius = true
            )
            assertFalse(
                "WMO $code must not claim 'clear skies' (got '${result.primary}')",
                result.primary.contains("clear skies", ignoreCase = true)
            )
        }
    }

    @Test
    fun no_rain_or_thunder_claim_when_clear_or_partly_cloudy() {
        val dry = listOf(0, 1, 2)
        for (code in dry) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code)),
                isCelsius = true
            )
            assertFalse(
                "WMO $code must not mention rain (got '${result.primary}')",
                result.primary.contains("rain", ignoreCase = true)
            )
            assertFalse(
                "WMO $code must not mention thunder (got '${result.primary}')",
                result.primary.contains("thunder", ignoreCase = true)
            )
        }
    }

    @Test
    fun thunderstorm_codes_always_mention_thunder() {
        for (code in listOf(95, 96, 99)) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code)),
                isCelsius = true
            )
            assertTrue(
                "WMO $code should mention thunderstorm (got '${result.primary}')",
                result.primary.contains("thunder", ignoreCase = true)
            )
        }
    }

    @Test
    fun snow_codes_mention_snow() {
        for (code in listOf(71, 73, 75)) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code, temp = -2.0)),
                isCelsius = true
            )
            assertTrue(
                "WMO $code should mention snow (got '${result.primary}')",
                result.primary.contains("snow", ignoreCase = true)
            )
        }
    }

    @Test
    fun fog_codes_mention_fog() {
        for (code in listOf(45, 48)) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code)),
                isCelsius = true
            )
            assertTrue(
                "WMO $code should mention fog (got '${result.primary}')",
                result.primary.contains("fog", ignoreCase = true)
            )
        }
    }

    // ── High wind must not contradict calm temperature adjectives ──────

    @Test
    fun high_wind_triggers_wind_callout() {
        val result = WeatherSummaryEngine.summarize(
            info(current(code = 0, wind = 45.0)),
            isCelsius = true
        )
        val joined = result.joinToString(" ") { it.text }
        assertTrue("Expected wind callout: $joined", joined.contains("Wind", ignoreCase = true))
    }

    // ── High UV gets a sunscreen hint in the secondary line ────────────

    @Test
    fun high_uv_secondary_mentions_sunscreen() {
        val result = WeatherSummaryEngine.summarizeDetailed(
            info(current(0), hourly = emptyList())
                .let { it.copy(daily = daily(uv = 8.0)) },
            isCelsius = true
        )
        assertNotNull("Secondary should be present for high UV", result.secondary)
        assertTrue(
            "Expected sunscreen: ${result.secondary}",
            result.secondary!!.contains("sunscreen", ignoreCase = true)
        )
    }

    // ── Unit conversion is consistent across both methods ──────────────

    @Test
    fun unit_consistency_across_summarize_and_summarizeDetailed() {
        val info = info(current(code = 0, temp = 25.0))
        val c = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = true)
        val f = WeatherSummaryEngine.summarizeDetailed(info, isCelsius = false)
        assertTrue("Celsius should mention 30°: ${c.primary}", c.primary.contains("30°"))
        assertTrue("Celsius should mention 20°: ${c.primary}", c.primary.contains("20°"))
        // 30°C ≈ 86°F, 20°C ≈ 68°F
        assertTrue("Fahrenheit should mention 86°: ${f.primary}", f.primary.contains("86°"))
        assertTrue("Fahrenheit should mention 68°: ${f.primary}", f.primary.contains("68°"))
    }

    // ── Humidity must not contradict condition ─────────────────────────

    @Test
    fun no_dry_air_mention_when_raining() {
        // Even if humidity is 20%, the raining conditions should suppress
        // the "Dry air" callout (it's clearly wet).
        for (code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)) {
            val result = WeatherSummaryEngine.summarize(
                info(current(code, humidity = 18.0)),
                isCelsius = true
            )
            val joined = result.joinToString(" ") { it.text }
            assertFalse(
                "WMO $code with 18% humidity should not claim 'Dry air' (got '$joined')",
                joined.contains("dry air", ignoreCase = true)
            )
        }
    }

    @Test
    fun no_humid_air_mention_when_snowing() {
        for (code in listOf(71, 73, 75, 77, 85, 86)) {
            val result = WeatherSummaryEngine.summarize(
                info(current(code, humidity = 90.0, temp = -2.0)),
                isCelsius = true
            )
            val joined = result.joinToString(" ") { it.text }
            // "Humid air — it may feel stickier" is the callout; with snow
            // it would be misleading.
            assertFalse(
                "WMO $code with 90% humidity should not claim 'Humid air' (got '$joined')",
                joined.contains("humid air", ignoreCase = true)
            )
        }
    }

    // ── Feels-like delta is sign-aware ──────────────────────────────────

    @Test
    fun feels_like_warmer_direction() {
        val result = WeatherSummaryEngine.summarize(
            info(current(code = 0, temp = 30.0, apparent = 38.0)),
            isCelsius = true
        )
        val joined = result.joinToString(" ") { it.text }
        assertTrue("Should report warmer: $joined", joined.contains("warmer", ignoreCase = true))
    }

    @Test
    fun feels_like_cooler_direction() {
        val result = WeatherSummaryEngine.summarize(
            info(current(code = 0, temp = 20.0, apparent = 14.0)),
            isCelsius = true
        )
        val joined = result.joinToString(" ") { it.text }
        assertTrue("Should report cooler: $joined", joined.contains("cooler", ignoreCase = true))
    }

    // ── Cross-condition — every WMO code renders something ─────────────

    @Test
    fun all_codes_produce_non_empty_summary() {
        for ((code, label) in matrix) {
            val result = WeatherSummaryEngine.summarizeDetailed(
                info(current(code, temp = 18.0, humidity = 60.0)),
                isCelsius = true
            )
            assertTrue(
                "WMO $code ($label) produced empty summary",
                result.primary.isNotBlank()
            )
        }
    }

    @Test
    fun all_codes_produce_non_empty_summarize_list() {
        for ((code, label) in matrix) {
            val sentences = WeatherSummaryEngine.summarize(
                info(current(code, temp = 18.0, humidity = 60.0)),
                isCelsius = true
            )
            assertTrue(
                "WMO $code ($label) produced no sentences",
                sentences.isNotEmpty()
            )
        }
    }
}
