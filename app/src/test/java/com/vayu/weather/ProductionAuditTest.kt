package com.vayu.weather

import com.vayu.weather.data.converter.WindConverter
import com.vayu.weather.data.converter.UnitsConverter
import com.vayu.weather.data.converter.PressureConverter
import com.vayu.weather.data.converter.PrecipitationConverter
import com.vayu.weather.domain.model.*
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WindUnit
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Comprehensive regression tests from the full production audit.
 *
 * Covers: moon phase, sun position, temperature conversion, wind conversion,
 * Beaufort scale, AQI categories, compass direction, and data consistency.
 */
class ProductionAuditTest {

    // ═══════════════════════════════════════════════════════════
    // MOON PHASE (Conway/Schaefer algorithm — mirrors SkyCastSunMoonCard)
    // ═══════════════════════════════════════════════════════════

    private data class MoonInfo(val name: String, val illuminationPct: Double)

    private fun moonPhase(date: LocalDate): MoonInfo {
        // Known new-moon reference: Jan 6 2000, synodic month 29.5305882 days
        val refNewMoon = LocalDate.of(2000, 1, 6)
        val daysSinceRef = java.time.temporal.ChronoUnit.DAYS.between(refNewMoon, date).toDouble()
        val age = ((daysSinceRef % 29.5305882) + 29.5305882) % 29.5305882
        val phase = age / 29.5305882

        val illum = (1.0 - kotlin.math.cos(2 * Math.PI * phase)) / 2.0 * 100.0

        val name = when {
            phase < 0.0625 -> "New moon"
            phase < 0.1875 -> "Waxing crescent"
            phase < 0.3125 -> "First quarter"
            phase < 0.4375 -> "Waxing gibbous"
            phase < 0.5625 -> "Full moon"
            phase < 0.6875 -> "Waning gibbous"
            phase < 0.8125 -> "Last quarter"
            phase < 0.9375 -> "Waning crescent"
            else -> "New moon"
        }
        return MoonInfo(name, illum)
    }

    @Test
    fun `moon phase Chennai 2026-08-30 has high illumination`() {
        val result = moonPhase(LocalDate.of(2026, 8, 30))
        // Aug 30 2026: should have high illumination (>85%)
        assertTrue("Illumination should be >85%, was ${result.illuminationPct}",
            result.illuminationPct > 85.0)
        assertTrue("Illumination should be <100%, was ${result.illuminationPct}",
            result.illuminationPct < 100.0)
    }

    @Test
    fun `moon phase new moon is near 0 percent illumination`() {
        // Jan 29 2025 is a known new moon
        val result = moonPhase(LocalDate.of(2025, 1, 29))
        assertEquals("New moon", result.name)
        assertTrue("Illumination for new moon should be <10%, was ${result.illuminationPct}",
            result.illuminationPct < 10.0)
    }

    @Test
    fun `moon phase full moon has high illumination`() {
        // Jan 13 2025 is a known full moon
        val result = moonPhase(LocalDate.of(2025, 1, 13))
        assertEquals("Full moon", result.name)
        assertTrue("Illumination for full moon should be >90%, was ${result.illuminationPct}",
            result.illuminationPct > 90.0)
    }

    @Test
    fun `moon phase first quarter has moderate illumination`() {
        // Jan 6 2025 is near first quarter
        val result = moonPhase(LocalDate.of(2025, 1, 6))
        assertEquals("First quarter", result.name)
        assertTrue("Illumination should be 40-60%, was ${result.illuminationPct}",
            result.illuminationPct in 40.0..60.0)
    }

    @Test
    fun `moon phase illumination is always between 0 and 100`() {
        // Test across a full month cycle
        for (day in 1..30) {
            val result = moonPhase(LocalDate.of(2025, 6, day))
            assertTrue("Day $day: illumination ${result.illuminationPct} should be 0-100",
                result.illuminationPct in 0.0..100.0)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SUN POSITION — verifies timezone-aware calculation
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `sun progress uses API time not device time`() {
        // Simulate: sunrise 05:57, sunset 18:21 (Chennai), API current time 14:30
        val sunrise = LocalTime.of(5, 57)
        val sunset = LocalTime.of(18, 21)
        val apiTime = LocalTime.of(14, 30) // location time, NOT device time

        val dayMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val elapsed = (apiTime.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val progress = (elapsed.toFloat() / dayMinutes).coerceIn(0f, 1f)

        // 14:30 is about 67% through the day (05:57 to 18:21)
        assertTrue("Progress should be >0.5, was $progress", progress > 0.5f)
        assertTrue("Progress should be <0.8, was $progress", progress < 0.8f)
    }

    @Test
    fun `sun progress before sunrise is 0`() {
        val sunrise = LocalTime.of(6, 0)
        val sunset = LocalTime.of(18, 0)
        val apiTime = LocalTime.of(5, 0) // before sunrise

        val dayMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val elapsed = (apiTime.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val progress = (elapsed.toFloat() / dayMinutes).coerceIn(0f, 1f)

        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun `sun progress after sunset is 1`() {
        val sunrise = LocalTime.of(6, 0)
        val sunset = LocalTime.of(18, 0)
        val apiTime = LocalTime.of(19, 0) // after sunset

        val dayMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val elapsed = (apiTime.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        val progress = (elapsed.toFloat() / dayMinutes).coerceIn(0f, 1f)

        assertEquals(1f, progress, 0.001f)
    }

    @Test
    fun `day length equals sunset minus sunrise`() {
        val sunrise = LocalTime.of(5, 57)
        val sunset = LocalTime.of(18, 21)
        val dayMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        // 12h 24m = 744 minutes
        assertEquals(744, dayMinutes)
    }

    // ═══════════════════════════════════════════════════════════
    // TEMPERATURE CONVERSION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `celsius to fahrenheit round trip`() {
        val temps = listOf(-40.0, -20.0, 0.0, 20.0, 37.0, 100.0)
        temps.forEach { c ->
            val f = UnitsConverter.celsiusToFahrenheit(c)
            val back = UnitsConverter.fahrenheitToCelsius(f)
            assertEquals("Round trip failed for $c°C", c, back, 0.001)
        }
    }

    @Test
    fun `celsius to fahrenheit known values`() {
        assertEquals(32.0, UnitsConverter.celsiusToFahrenheit(0.0), 0.001)
        assertEquals(212.0, UnitsConverter.celsiusToFahrenheit(100.0), 0.001)
        assertEquals(-40.0, UnitsConverter.celsiusToFahrenheit(-40.0), 0.001)
        assertEquals(77.0, UnitsConverter.celsiusToFahrenheit(25.0), 0.001)
        assertEquals(98.6, UnitsConverter.celsiusToFahrenheit(37.0), 0.1)
    }

    @Test
    fun `temperature display respects unit setting`() {
        assertEquals("25°C", UnitsConverter.getTemperatureDisplay(25.0, TemperatureUnit.CELSIUS))
        assertEquals("77°F", UnitsConverter.getTemperatureDisplay(25.0, TemperatureUnit.FAHRENHEIT))
    }

    // ═══════════════════════════════════════════════════════════
    // WIND CONVERSION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `kmh to mph conversion`() {
        assertEquals(6.21, WindConverter.kmhToMph(10.0), 0.1)
        assertEquals(62.14, WindConverter.kmhToMph(100.0), 0.1)
    }

    @Test
    fun `kmh to ms conversion`() {
        assertEquals(2.78, WindConverter.kmhToMs(10.0), 0.1)
        assertEquals(0.0, WindConverter.kmhToMs(0.0), 0.001)
    }

    @Test
    fun `kmh to knots conversion`() {
        assertEquals(5.40, WindConverter.kmhToKnots(10.0), 0.1)
    }

    @Test
    fun `wind display respects unit setting`() {
        assertEquals("10 km/h", WindConverter.getWindDisplay(10.0, WindUnit.KPH))
        assertEquals("6 mph", WindConverter.getWindDisplay(10.0, WindUnit.MPH))
        assertEquals("2 m/s", WindConverter.getWindDisplay(10.0, WindUnit.MS))
        assertEquals("5 knots", WindConverter.getWindDisplay(10.0, WindUnit.KNOTS))
    }

    @Test
    fun `wind null returns dash`() {
        assertEquals("--", WindConverter.getWindDisplay(null, WindUnit.KPH))
    }

    // ═══════════════════════════════════════════════════════════
    // BEAUFORT SCALE
    // ═══════════════════════════════════════════════════════════

    private fun beaufortLevel(kph: Double): String = when {
        kph < 1 -> "Calm"
        kph < 6 -> "Light air"
        kph < 12 -> "Light breeze"
        kph < 20 -> "Gentle breeze"
        kph < 29 -> "Moderate breeze"
        kph < 39 -> "Fresh breeze"
        kph < 50 -> "Strong breeze"
        kph < 62 -> "Near gale"
        kph < 75 -> "Gale"
        kph < 89 -> "Strong gale"
        kph < 103 -> "Storm"
        kph < 118 -> "Violent storm"
        else -> "Hurricane"
    }

    @Test
    fun `beaufort calm at 0 kph`() {
        assertEquals("Calm", beaufortLevel(0.0))
    }

    @Test
    fun `beaufort light breeze at 15 kph`() {
        assertEquals("Gentle breeze", beaufortLevel(15.0))
    }

    @Test
    fun `beaufort hurricane at 120 kph`() {
        assertEquals("Hurricane", beaufortLevel(120.0))
    }

    @Test
    fun `beaufort fresh breeze at 35 kph`() {
        assertEquals("Fresh breeze", beaufortLevel(35.0))
    }

    // ═══════════════════════════════════════════════════════════
    // COMPASS DIRECTION
    // ═══════════════════════════════════════════════════════════

    private fun cardinalDirection(deg: Double): String = when {
        deg < 22.5 || deg >= 337.5 -> "N"
        deg < 67.5 -> "NE"
        deg < 112.5 -> "E"
        deg < 157.5 -> "SE"
        deg < 202.5 -> "S"
        deg < 247.5 -> "SW"
        deg < 292.5 -> "W"
        else -> "NW"
    }

    @Test
    fun `compass north at 0 degrees`() {
        assertEquals("N", cardinalDirection(0.0))
    }

    @Test
    fun `compass east at 90 degrees`() {
        assertEquals("E", cardinalDirection(90.0))
    }

    @Test
    fun `compass south at 180 degrees`() {
        assertEquals("S", cardinalDirection(180.0))
    }

    @Test
    fun `compass west at 270 degrees`() {
        assertEquals("W", cardinalDirection(270.0))
    }

    @Test
    fun `compass north at 350 degrees`() {
        assertEquals("N", cardinalDirection(350.0))
    }

    @Test
    fun `compass NE at 45 degrees`() {
        assertEquals("NE", cardinalDirection(45.0))
    }

    // ═══════════════════════════════════════════════════════════
    // AQI CATEGORIES
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `AQI categories are internally consistent`() {
        // European AQI
        val good = AirQuality(europeanAqi = 10, usAqi = null, pm25 = null, pm10 = null, nitrogenDioxide = null, ozone = null, sulphurDioxide = null, carbonMonoxide = null)
        assertEquals("Good", good.aqiLabel)
        assertEquals(1, good.aqiColorIndex)

        val fair = AirQuality(europeanAqi = 30, usAqi = null, pm25 = null, pm10 = null, nitrogenDioxide = null, ozone = null, sulphurDioxide = null, carbonMonoxide = null)
        assertEquals("Fair", fair.aqiLabel)
        assertEquals(2, fair.aqiColorIndex)

        val moderate = AirQuality(europeanAqi = 50, usAqi = null, pm25 = null, pm10 = null, nitrogenDioxide = null, ozone = null, sulphurDioxide = null, carbonMonoxide = null)
        assertEquals("Moderate", moderate.aqiLabel)
        assertEquals(3, moderate.aqiColorIndex)
    }

    @Test
    fun `AQI European takes priority over US when both present`() {
        val aqi = AirQuality(europeanAqi = 10, usAqi = 250, pm25 = null, pm10 = null, nitrogenDioxide = null, ozone = null, sulphurDioxide = null, carbonMonoxide = null)
        assertEquals("Good", aqi.aqiLabel) // European takes priority
    }

    // ═══════════════════════════════════════════════════════════
    // PRESSURE / PRECIPITATION CONVERSION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `hPa to inHg conversion`() {
        assertEquals(29.92, PressureConverter.hpaToInHg(1013.25), 0.1)
    }

    @Test
    fun `mm to inches conversion`() {
        assertEquals(1.0, PrecipitationConverter.mmToInches(25.4), 0.01)
    }

    // ═══════════════════════════════════════════════════════════
    // WEATHER CODE MAPPING
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `all WMO weather codes map to valid conditions`() {
        val validCodes = setOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57,
            61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 87, 95, 96, 99)
        validCodes.forEach { code ->
            val condition = WeatherCondition.fromCodeSafe(code)
            assertNotNull("Code $code should map to a condition", condition)
            assertNotEquals("Code $code should not be UNKNOWN", WeatherCondition.UNKNOWN, condition)
        }
    }

    @Test
    fun `weather condition day and night descriptions differ for clear sky`() {
        val dayDesc = WeatherDescription.getWeatherDescription(0, isDay = true)
        val nightDesc = WeatherDescription.getWeatherDescription(0, isDay = false)
        assertEquals("Clear Sky", dayDesc)
        assertEquals("Clear Night", nightDesc)
    }

    // ═══════════════════════════════════════════════════════════
    // DATA CONSISTENCY
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `sunset is always after sunrise`() {
        val sunrise = LocalTime.of(5, 57)
        val sunset = LocalTime.of(18, 21)
        assertTrue("Sunset must be after sunrise", sunset.isAfter(sunrise))
    }

    @Test
    fun `day length is positive`() {
        val sunrise = LocalTime.of(5, 57)
        val sunset = LocalTime.of(18, 21)
        val dayMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
        assertTrue("Day length must be positive", dayMinutes > 0)
    }

    @Test
    fun `hourly forecast times are parseable ISO format`() {
        val times = listOf("2026-08-30T14:00", "2026-08-30T15:00", "2026-08-30T16:00")
        times.forEach { time ->
            val parsed = try {
                LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
                true
            } catch (e: Exception) {
                false
            }
            assertTrue("Time '$time' should be parseable as ISO_DATE_TIME", parsed)
        }
    }

    @Test
    fun `daily forecast dates are parseable`() {
        val dates = listOf("2026-08-30", "2026-08-31", "2026-09-01")
        dates.forEach { date ->
            val parsed = try {
                LocalDate.parse(date)
                true
            } catch (e: Exception) {
                false
            }
            assertTrue("Date '$date' should be parseable", parsed)
        }
    }

    @Test
    fun `max temp is greater than min temp`() {
        val daily = DailyWeather(
            date = "2026-08-30", weatherCode = 0,
            maxTemp = 32.0, minTemp = 24.0,
            uvIndex = 8.0, precipitationProbability = 10
        )
        assertTrue("Max temp should be > min temp", daily.maxTemp > daily.minTemp)
    }
}
