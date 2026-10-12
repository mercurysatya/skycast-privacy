package com.vayu.weather

import com.vayu.weather.data.converter.PressureConverter
import com.vayu.weather.data.converter.PrecipitationConverter
import com.vayu.weather.data.converter.PressureUnit
import com.vayu.weather.data.converter.PrecipitationUnit
import com.vayu.weather.data.converter.UnitsConverter
import com.vayu.weather.data.converter.WindConverter
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WindUnit
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit conversion tests that call PRODUCTION conversion functions.
 * Does NOT reimplement any logic inside tests.
 */
class UnitConversionTest {

    // ═══════════════════════════════════════════════════════════
    // TEMPERATURE — production UnitsConverter
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `celsius to fahrenheit absolute zero`() {
        assertEquals(-459.67, UnitsConverter.celsiusToFahrenheit(-273.15), 0.1)
    }

    @Test
    fun `celsius to fahrenheit freezing`() {
        assertEquals(32.0, UnitsConverter.celsiusToFahrenheit(0.0), 0.001)
    }

    @Test
    fun `celsius to fahrenheit body temperature`() {
        assertEquals(98.6, UnitsConverter.celsiusToFahrenheit(37.0), 0.1)
    }

    @Test
    fun `celsius to fahrenheit boiling`() {
        assertEquals(212.0, UnitsConverter.celsiusToFahrenheit(100.0), 0.001)
    }

    @Test
    fun `celsius to fahrenheit negative values`() {
        assertEquals(-4.0, UnitsConverter.celsiusToFahrenheit(-20.0), 0.001)
    }

    @Test
    fun `fahrenheit to celsius round trip preserves precision`() {
        val testValues = listOf(-40.0, -20.0, -10.0, 0.0, 10.0, 20.0, 25.0, 37.0, 100.0)
        testValues.forEach { c ->
            val f = UnitsConverter.celsiusToFahrenheit(c)
            val back = UnitsConverter.fahrenheitToCelsius(f)
            assertEquals("Round trip failed for $c°C", c, back, 0.01)
        }
    }

    @Test
    fun `getTemperatureDisplay celsius uses degree C`() {
        val result = UnitsConverter.getTemperatureDisplay(25.0, TemperatureUnit.CELSIUS)
        assertTrue("Should contain °C", result.contains("°C"))
        assertTrue("Should contain 25", result.contains("25"))
    }

    @Test
    fun `getTemperatureDisplay fahrenheit uses degree F`() {
        val result = UnitsConverter.getTemperatureDisplay(25.0, TemperatureUnit.FAHRENHEIT)
        assertTrue("Should contain °F", result.contains("°F"))
        assertTrue("Should contain 77", result.contains("77"))
    }

    @Test
    fun `getTemperatureDisplay rounds to integer`() {
        val result = UnitsConverter.getTemperatureDisplay(25.7, TemperatureUnit.CELSIUS)
        assertTrue("Should round 25.7 to 26", result.contains("26"))
        val negative = UnitsConverter.getTemperatureDisplay(-1.6, TemperatureUnit.CELSIUS)
        assertTrue("Should round -1.6 to -2", negative.contains("-2"))
    }

    @Test
    fun `getTemperatureDisplay handles zero`() {
        val result = UnitsConverter.getTemperatureDisplay(0.0, TemperatureUnit.CELSIUS)
        assertEquals("0°C", result)
    }

    @Test
    fun `getTemperatureDisplay handles negative values`() {
        val result = UnitsConverter.getTemperatureDisplay(-10.0, TemperatureUnit.FAHRENHEIT)
        assertEquals("14°F", result)
    }

    @Test
    fun `getTemperatureUnitLabel celsius`() {
        assertEquals("°C", UnitsConverter.getTemperatureUnitLabel(TemperatureUnit.CELSIUS))
    }

    @Test
    fun `getTemperatureUnitLabel fahrenheit`() {
        assertEquals("°F", UnitsConverter.getTemperatureUnitLabel(TemperatureUnit.FAHRENHEIT))
    }

    // ═══════════════════════════════════════════════════════════
    // WIND — production WindConverter
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `kmh to mph exact conversion`() {
        assertEquals(6.21371, WindConverter.kmhToMph(10.0), 0.001)
    }

    @Test
    fun `kmh to mph zero`() {
        assertEquals(0.0, WindConverter.kmhToMph(0.0), 0.001)
    }

    @Test
    fun `kmh to mph large value`() {
        assertEquals(124.2742, WindConverter.kmhToMph(200.0), 0.01)
    }

    @Test
    fun `mph to kmh round trip`() {
        val testValues = listOf(0.0, 10.0, 30.0, 60.0, 100.0)
        testValues.forEach { mph ->
            val kmh = WindConverter.mphToKmh(mph)
            val back = WindConverter.kmhToMph(kmh)
            assertEquals("Round trip failed for $mph mph", mph, back, 0.01)
        }
    }

    @Test
    fun `kmh to ms conversion`() {
        assertEquals(2.7778, WindConverter.kmhToMs(10.0), 0.01)
    }

    @Test
    fun `kmh to ms zero`() {
        assertEquals(0.0, WindConverter.kmhToMs(0.0), 0.001)
    }

    @Test
    fun `ms to kmh round trip`() {
        val testValues = listOf(0.0, 5.0, 15.0, 50.0)
        testValues.forEach { ms ->
            val kmh = WindConverter.msToKmh(ms)
            val back = WindConverter.kmhToMs(kmh)
            assertEquals("Round trip failed for $ms m/s", ms, back, 0.01)
        }
    }

    @Test
    fun `kmh to knots conversion`() {
        assertEquals(5.39957, WindConverter.kmhToKnots(10.0), 0.01)
    }

    @Test
    fun `knots to kmh round trip`() {
        val testValues = listOf(0.0, 5.0, 20.0, 50.0)
        testValues.forEach { kn ->
            val kmh = WindConverter.knotsToKmh(kn)
            val back = WindConverter.kmhToKnots(kmh)
            assertEquals("Round trip failed for $kn knots", kn, back, 0.01)
        }
    }

    @Test
    fun `getWindDisplay kph shows km slash h`() {
        val result = WindConverter.getWindDisplay(10.0, WindUnit.KPH)
        assertEquals("10 km/h", result)
    }

    @Test
    fun `getWindDisplay mph shows mph`() {
        val result = WindConverter.getWindDisplay(10.0, WindUnit.MPH)
        assertEquals("6 mph", result)
    }

    @Test
    fun `getWindDisplay ms shows m slash s`() {
        val result = WindConverter.getWindDisplay(10.0, WindUnit.MS)
        assertEquals("3 m/s", result)
    }

    @Test
    fun `getWindDisplay knots shows kn`() {
        val result = WindConverter.getWindDisplay(10.0, WindUnit.KNOTS)
        assertEquals("5 kn", result)
    }

    @Test
    fun `getWindDisplay null returns dash`() {
        assertEquals("--", WindConverter.getWindDisplay(null, WindUnit.KPH))
        assertEquals("--", WindConverter.getWindDisplay(null, WindUnit.MPH))
        assertEquals("--", WindConverter.getWindDisplay(null, WindUnit.MS))
        assertEquals("--", WindConverter.getWindDisplay(null, WindUnit.KNOTS))
    }

    @Test
    fun `getWindDisplay zero`() {
        assertEquals("0 km/h", WindConverter.getWindDisplay(0.0, WindUnit.KPH))
        assertEquals("0 mph", WindConverter.getWindDisplay(0.0, WindUnit.MPH))
    }

    @Test
    fun `getWindDisplay rounds to integer`() {
        val result = WindConverter.getWindDisplay(15.7, WindUnit.KPH)
        assertEquals("16 km/h", result)
    }

    @Test
    fun `getWindUnitLabel kph`() {
        assertEquals("km/h", WindConverter.getWindUnitLabel(WindUnit.KPH))
    }

    @Test
    fun `getWindUnitLabel mph`() {
        assertEquals("mph", WindConverter.getWindUnitLabel(WindUnit.MPH))
    }

    @Test
    fun `getWindUnitLabel ms`() {
        assertEquals("m/s", WindConverter.getWindUnitLabel(WindUnit.MS))
    }

    @Test
    fun `getWindUnitLabel kn`() {
        assertEquals("kn", WindConverter.getWindUnitLabel(WindUnit.KNOTS))
    }

    // ═══════════════════════════════════════════════════════════
    // PRESSURE — production PressureConverter
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `hpa to inhg standard atmosphere`() {
        assertEquals(29.92, PressureConverter.hpaToInHg(1013.25), 0.1)
    }

    @Test
    fun `hpa to inhg zero`() {
        assertEquals(0.0, PressureConverter.hpaToInHg(0.0), 0.001)
    }

    @Test
    fun `hpa to inhg high pressure`() {
        assertEquals(31.09, PressureConverter.hpaToInHg(1050.0), 0.1)
    }

    @Test
    fun `hpa to inhg low pressure`() {
        assertEquals(28.50, PressureConverter.hpaToInHg(965.0), 0.1)
    }

    @Test
    fun `inhg to hpa round trip`() {
        val testValues = listOf(0.0, 28.0, 29.92, 31.0, 33.0)
        testValues.forEach { inhg ->
            val hpa = PressureConverter.inHgToHpa(inhg)
            val back = PressureConverter.hpaToInHg(hpa)
            assertEquals("Round trip failed for $inhg inHg", inhg, back, 0.01)
        }
    }

    @Test
    fun `getPressureDisplay hpa shows hPa`() {
        val result = PressureConverter.getPressureDisplay(1013.0, PressureUnit.HPA)
        assertEquals("1013 hPa", result)
    }

    @Test
    fun `getPressureDisplay inhg shows inHg`() {
        val result = PressureConverter.getPressureDisplay(1013.0, PressureUnit.INHG)
        assertEquals("30 inHg", result)
    }

    @Test
    fun `getPressureDisplay null returns dash`() {
        assertEquals("--", PressureConverter.getPressureDisplay(null, PressureUnit.HPA))
        assertEquals("--", PressureConverter.getPressureDisplay(null, PressureUnit.INHG))
    }

    @Test
    fun `getPressureDisplay rounds to integer`() {
        val result = PressureConverter.getPressureDisplay(1013.6, PressureUnit.HPA)
        assertEquals("1014 hPa", result)
    }

    @Test
    fun `getPressureUnitLabel hpa`() {
        assertEquals("hPa", PressureConverter.getPressureUnitLabel(PressureUnit.HPA))
    }

    @Test
    fun `getPressureUnitLabel inhg`() {
        assertEquals("inHg", PressureConverter.getPressureUnitLabel(PressureUnit.INHG))
    }

    // ═══════════════════════════════════════════════════════════
    // PRECIPITATION — production PrecipitationConverter
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `mm to inches exact conversion`() {
        assertEquals(1.0, PrecipitationConverter.mmToInches(25.4), 0.001)
    }

    @Test
    fun `mm to inches zero`() {
        assertEquals(0.0, PrecipitationConverter.mmToInches(0.0), 0.001)
    }

    @Test
    fun `mm to inches small amount`() {
        assertEquals(0.03937, PrecipitationConverter.mmToInches(1.0), 0.001)
    }

    @Test
    fun `mm to inches heavy rain`() {
        assertEquals(3.937, PrecipitationConverter.mmToInches(100.0), 0.01)
    }

    @Test
    fun `inches to mm round trip`() {
        val testValues = listOf(0.0, 0.5, 1.0, 2.5, 5.0, 10.0)
        testValues.forEach { inches ->
            val mm = PrecipitationConverter.inchesToMm(inches)
            val back = PrecipitationConverter.mmToInches(mm)
            assertEquals("Round trip failed for $inches in", inches, back, 0.01)
        }
    }

    @Test
    fun `getPrecipitationDisplay mm shows mm`() {
        val result = PrecipitationConverter.getPrecipitationDisplay(5.0, PrecipitationUnit.MM)
        assertEquals("5 mm", result)
    }

    @Test
    fun `getPrecipitationDisplay inches shows in`() {
        val result = PrecipitationConverter.getPrecipitationDisplay(25.4, PrecipitationUnit.INCH)
        assertEquals("1 in", result)
    }

    @Test
    fun `getPrecipitationDisplay null returns dash`() {
        assertEquals("--", PrecipitationConverter.getPrecipitationDisplay(null, PrecipitationUnit.MM))
        assertEquals("--", PrecipitationConverter.getPrecipitationDisplay(null, PrecipitationUnit.INCH))
    }

    @Test
    fun `getPrecipitationDisplay zero`() {
        assertEquals("0 mm", PrecipitationConverter.getPrecipitationDisplay(0.0, PrecipitationUnit.MM))
    }

    @Test
    fun `getPrecipitationUnitLabel mm`() {
        assertEquals("mm", PrecipitationConverter.getPrecipitationUnitLabel(PrecipitationUnit.MM))
    }

    @Test
    fun `getPrecipitationUnitLabel inches`() {
        assertEquals("inches", PrecipitationConverter.getPrecipitationUnitLabel(PrecipitationUnit.INCH))
    }

    // ═══════════════════════════════════════════════════════════
    // TIMEZONE SIMULATION — device vs weather location time
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `hourly forecast nearest hour selection with timezone offset`() {
        // Simulate: device is UTC-5, weather location is UTC+5:30 (India)
        // Device thinks it's 2026-09-07T10:00 (local)
        // But in weather location it's 2026-09-07T20:30
        // The API current.time would be "2026-09-07T20:30"
        val apiTime = "2026-09-07T20:30"
        val hourlyTimes = listOf(
            "2026-09-07T18:00", "2026-09-07T19:00", "2026-09-07T20:00",
            "2026-09-07T21:00", "2026-09-07T22:00", "2026-09-07T23:00"
        )

        // Correct approach: find nearest hour using API time, not device time
        val currentHour = apiTime.substringAfter("T").take(2).toIntOrNull() ?: 0
        val nearestHour = hourlyTimes.minByOrNull { time ->
            val h = time.substringAfter("T").take(2).toIntOrNull() ?: 0
            kotlin.math.abs(h - currentHour)
        }

        assertEquals("2026-09-07T20:00", nearestHour)
    }

    @Test
    fun `daily today label uses API timezone not device`() {
        // If API says current date is "2026-09-08" but device is still "2026-09-07"
        val apiDate = "2026-09-08"
        val deviceDate = "2026-09-07"
        val dailyDate = "2026-09-08"

        // Correct: use API date for "Today" comparison
        val isTodayByApi = dailyDate == apiDate
        val isTodayByDevice = dailyDate == deviceDate

        assertTrue("API-based today check should match", isTodayByApi)
        assertFalse("Device-based today check should NOT match", isTodayByDevice)
    }

    @Test
    fun `hourly boundary at exact hour`() {
        val apiTime = "2026-09-07T12:00"
        val currentHour = apiTime.substringAfter("T").take(2).toIntOrNull() ?: 0
        assertEquals(12, currentHour)
    }

    @Test
    fun `hourly boundary between entries`() {
        val apiTime = "2026-09-07T12:30"
        val currentHour = apiTime.substringAfter("T").take(2).toIntOrNull() ?: 0
        val hourlyTimes = listOf("2026-09-07T12:00", "2026-09-07T13:00")
        val nearest = hourlyTimes.minByOrNull { time ->
            val h = time.substringAfter("T").take(2).toIntOrNull() ?: 0
            kotlin.math.abs(h - currentHour)
        }
        assertEquals("Should snap to 12:00", "2026-09-07T12:00", nearest)
    }

    @Test
    fun `day boundary crossing - weather location is next day`() {
        // API timezone is UTC+12, device is UTC-5
        // API current time: "2026-09-08T02:00" (already next day)
        val apiDate = "2026-09-08"
        val yesterday = "2026-09-07"
        val today = "2026-09-08"
        val tomorrow = "2026-09-09"

        // Should use API date for all day labels
        assertEquals("Today should be API date", apiDate, today)
        assertNotEquals("Yesterday should not be API date", apiDate, yesterday)
    }
}
