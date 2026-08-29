package com.vayu.weather.presentation.components.skycast

import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherSummaryEngineTest {

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

    private fun daily() = listOf(
        DailyWeather(
            date = LocalDate.now().toString(),
            weatherCode = 0,
            maxTemp = 30.0,
            minTemp = 20.0,
            uvIndex = 5.0,
            precipitationProbability = 10
        )
    )

    private fun info(
        temp: Double = 25.0,
        code: Int = 0,
        isDay: Boolean = true,
        hourly: List<HourlyWeather> = emptyList()
    ) = WeatherInfo(
        current = current(temp = temp, code = code, isDay = isDay),
        hourly = hourly,
        daily = daily()
    )

    @Test
    fun sunnyDayMentionsClear() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 0, isDay = true), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(joined.contains("Clear", ignoreCase = true))
    }

    @Test
    fun thunderstormMentionsStorm() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 95), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(joined.contains("Thunderstorm", ignoreCase = true))
    }

    @Test
    fun feelsLikeCalloutWhenDeltaIsLarge() {
        val sentences = WeatherSummaryEngine.summarize(
            info(temp = 30.0).copy(current = current(temp = 30.0, apparent = 38.0)),
            isCelsius = true
        )
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(joined.contains("feels like", ignoreCase = true))
    }

    @Test
    fun dryAirCallout() {
        val sentences = WeatherSummaryEngine.summarize(
            info().copy(current = current(humidity = 18.0)),
            isCelsius = true
        )
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(joined.contains("Dry", ignoreCase = true))
    }

    @Test
    fun highLowReported() {
        val sentences = WeatherSummaryEngine.summarize(info(), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(joined.contains("30°") && joined.contains("20°"))
    }

    @Test
    fun suggestOutdoorWindowWithGoodHour() {
        val now = java.time.LocalDateTime.now()
        val goodHours = (1..6).map { i ->
            HourlyWeather(
                time = now.plusHours(i.toLong()).toString(),
                temperature = 22.0,
                weatherCode = 0,
                humidity = 50.0,
                pressure = 1013.0,
                windSpeed = 8.0,
                precipitationProbability = 5
            )
        }
        val window = WeatherSummaryEngine.suggestOutdoorWindow(goodHours, isCelsius = true)
        assertTrue(window != null && window.contains("AM") || window!!.contains("PM"))
    }

    // ── Contradiction tests ──

    @Test
    fun clearSkyDoesNotMentionRain() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 0), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Expected no 'rain' in clear-sky summary: $joined",
            !joined.contains("rain", ignoreCase = true)
        )
    }

    @Test
    fun sunnyDoesNotMentionSnow() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 1), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Expected no 'snow' in sunny summary: $joined",
            !joined.contains("snow", ignoreCase = true)
        )
    }

    @Test
    fun overcastSummaryDoesNotClaimClear() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 3), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Overcast must not claim 'Clear skies': $joined",
            !joined.contains("Clear skies", ignoreCase = true)
        )
    }

    @Test
    fun thunderstormSummaryDoesNotClaimSunny() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 95), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Thunderstorm must not claim 'sunny' or 'clear': $joined",
            !(joined.contains("sunny", ignoreCase = true) || joined.contains("clear skies", ignoreCase = true))
        )
    }

    @Test
    fun snowSummaryDoesNotClaimDryAir() {
        val sentences = WeatherSummaryEngine.summarize(
            info().copy(current = current(code = 73, humidity = 18.0)),
            isCelsius = true
        )
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Snowy+humid scenario must not claim 'Dry air': $joined",
            !joined.contains("Dry air", ignoreCase = true)
        )
    }

    @Test
    fun nightClearSkiesReportsNight() {
        val sentences = WeatherSummaryEngine.summarize(info(code = 0, isDay = false), isCelsius = true)
        val joined = sentences.joinToString(" ") { it.text }
        assertTrue(
            "Night clear should reference night or evening: $joined",
            joined.contains("night", ignoreCase = true) || joined.contains("evening", ignoreCase = true)
        )
    }

    @Test
    fun fahrenheitConversionProducesHigherNumbers() {
        val c = WeatherSummaryEngine.summarize(info(temp = 25.0), isCelsius = true)
            .joinToString(" ") { it.text }
        val f = WeatherSummaryEngine.summarize(info(temp = 25.0), isCelsius = false)
            .joinToString(" ") { it.text }
        // The C version should reference 25°; the F version should reference 77°.
        assertTrue("Celsius summary should mention 25°: $c", c.contains("25°"))
        assertTrue("Fahrenheit summary should mention 77°: $f", f.contains("77°"))
    }
}
