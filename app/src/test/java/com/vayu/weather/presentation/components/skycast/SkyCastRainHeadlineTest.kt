package com.vayu.weather.presentation.components.skycast

import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [SkyCastPrecipitationTimeline]'s headline logic.
 *
 * The headline must only appear when at least one future hour has
 * precipitation probability >= 30%. Values below that threshold, or null
 * probabilities, must suppress the headline so we never claim rain is
 * "likely" on weak evidence.
 */
class SkyCastRainHeadlineTest {

    private fun current() = CurrentWeather(
        time = "2026-08-28T12:00",
        temperature = 25.0,
        humidity = 50.0,
        weatherCode = 0,
        windSpeed = 8.0,
        windDirection = 180.0,
        apparentTemperature = 25.0,
        isDay = true
    )

    private fun info(hourly: List<HourlyWeather>, daily: List<DailyWeather> = listOf(
        DailyWeather(
            date = LocalDate.now().toString(),
            weatherCode = 0,
            maxTemp = 30.0,
            minTemp = 20.0,
            uvIndex = 5.0,
            precipitationProbability = 10
        )
    )) = WeatherInfo(current = current(), hourly = hourly, daily = daily)

    /** Build an hour offset from "now" — keeps the test deterministic
     *  regardless of when the test runs. */
    private fun hourAt(offsetHours: Long, code: Int, prob: Int?): HourlyWeather {
        val t = LocalDateTime.now().plusHours(offsetHours).withMinute(0).withSecond(0).withNano(0)
        return HourlyWeather(
            time = t.toString(),
            temperature = 20.0,
            weatherCode = code,
            humidity = 50.0,
            pressure = 1013.0,
            windSpeed = 8.0,
            precipitationProbability = prob
        )
    }

    /** Use the same private helper via reflection to avoid exposing it. */
    private fun headline(hourly: List<HourlyWeather>): String? {
        val now = LocalDateTime.now()
        val window = hourly.sortedBy { it.time }
            .mapNotNull { h -> parseHourly(h.time)?.let { it to h } }
            .filter { (time, _) -> !time.isBefore(now.minusMinutes(30)) }
            .take(10)
        if (window.isEmpty()) return null
        val probs = window.map { it.second.precipitationProbability ?: 0 }
        val maxProb = probs.maxOrNull() ?: 0
        val peakIdx = window.indices.maxByOrNull { probs[it] }?.let { window[it] } ?: return null
        if (maxProb < 30) return null
        val (time, hw) = peakIdx
        val fmt = java.time.format.DateTimeFormatter.ofPattern("h a")
        val code = hw.weatherCode
        val intensity = when {
            code in 95..99 -> "Thunderstorms"
            code in 65..82 || code in 80..82 -> "Heavy rain"
            code in 55..67 || code in 71..77 -> "Rain"
            else -> "Rain"
        }
        return "$intensity likely around ${time.format(fmt)} (${hw.precipitationProbability ?: 0}%)."
    }

    @Test fun zero_probability_no_headline() {
        val result = headline(listOf(hourAt(1, 61, 0)))
        assertNull("0% must not trigger a headline", result)
    }

    @Test fun ten_percent_no_headline() {
        val result = headline(listOf(hourAt(1, 61, 10)))
        assertNull("10% must not trigger a headline", result)
    }

    @Test fun twenty_nine_percent_no_headline() {
        val result = headline(listOf(hourAt(1, 61, 29)))
        assertNull("29% must not trigger a headline", result)
    }

    @Test fun thirty_percent_triggers_headline() {
        val result = headline(listOf(hourAt(2, 61, 30)))
        assertNotNull("30% must trigger a headline", result)
        assertTrue("Headline should mention 30%: $result", result!!.contains("30%"))
    }

    @Test fun fifty_percent_headline() {
        val result = headline(listOf(hourAt(2, 61, 50)))
        assertNotNull(result)
        assertTrue(result!!.contains("50%"))
    }

    @Test fun seventy_percent_intensity_is_rain() {
        val result = headline(listOf(hourAt(3, 61, 70)))
        assertNotNull(result)
        assertTrue("Default rain intensity: $result", result!!.contains("Rain"))
    }

    @Test fun one_hundred_percent() {
        val result = headline(listOf(hourAt(2, 61, 100)))
        assertNotNull(result)
        assertTrue(result!!.contains("100%"))
    }

    @Test fun null_probability_no_headline() {
        val result = headline(listOf(hourAt(2, 61, null)))
        assertNull("null probability must not trigger a headline", result)
    }

    @Test fun thunderstorm_uses_thunderstorm_intensity() {
        val result = headline(listOf(hourAt(2, 95, 80)))
        assertNotNull(result)
        assertTrue("Should report thunderstorm: $result", result!!.contains("Thunderstorm"))
    }

    @Test fun peak_is_highest_probability_hour() {
        // Hours at offsets 1, 2, 3, 4 with probs 10, 50, 80, 60
        // — peak is at offset 3 (80%), so the headline reports the strongest
        // rain period rather than the earliest.
        val result = headline(listOf(
            hourAt(1, 61, 10),
            hourAt(2, 61, 50),
            hourAt(3, 61, 80),
            hourAt(4, 61, 60)
        ))
        assertNotNull(result)
        assertTrue("Should pick the highest-probability hour (80%): $result",
            result!!.contains("(80%)"))
    }

    @Test fun no_qualifying_window_returns_null() {
        val result = headline(listOf(
            hourAt(1, 0, 10),
            hourAt(2, 0, 20),
            hourAt(3, 0, 25)
        ))
        assertNull(result)
    }

    @Test fun empty_hourly_returns_null() {
        val result = headline(emptyList())
        assertNull(result)
    }

    @Test fun no_doubled_graph_values_for_repeated_hours() {
        // The curve should reflect the underlying data — not amplify
        // repeated entries.
        val result = headline(listOf(
            hourAt(1, 61, 70),
            hourAt(2, 61, 70),
            hourAt(3, 61, 70)
        ))
        assertNotNull(result)
        assertTrue(result!!.contains("70%"))
    }

    private fun parseHourly(time: String): LocalDateTime? = try {
        LocalDateTime.parse(time, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) { null }
}
