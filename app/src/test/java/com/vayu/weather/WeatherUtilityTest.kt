package com.vayu.weather

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class WeatherUtilityTest {

    private fun convertTemp(temp: Double, isCelsius: Boolean): Int {
        return if (isCelsius) temp.roundToInt()
        else (temp * 9 / 5 + 32).roundToInt()
    }

    private fun convertWind(windKph: Double?, unit: String): String {
        val speed = windKph ?: return "--"
        return when (unit) {
            "KPH" -> "${speed.roundToInt()}"
            "MPH" -> "${(speed * 0.621371).roundToInt()}"
            "MS" -> "${(speed / 3.6).roundToInt()}"
            "KNOTS" -> "${(speed * 0.539957).roundToInt()}"
            else -> "--"
        }
    }

    @Test
    fun `convertTemp celsius`() {
        assertEquals(25, convertTemp(25.0, true))
        assertEquals(0, convertTemp(0.0, true))
        assertEquals(-5, convertTemp(-5.0, true))
    }

    @Test
    fun `convertTemp fahrenheit`() {
        assertEquals(77, convertTemp(25.0, false))
        assertEquals(32, convertTemp(0.0, false))
        assertEquals(23, convertTemp(-5.0, false))
    }

    @Test
    fun `convertTemp rounding`() {
        assertEquals(26, convertTemp(25.5, true))
        assertEquals(78, convertTemp(25.5, false))
    }

    @Test
    fun `convertWind kph`() {
        assertEquals("10", convertWind(10.0, "KPH"))
        assertEquals("0", convertWind(0.0, "KPH"))
    }

    @Test
    fun `convertWind mph`() {
        assertEquals("6", convertWind(10.0, "MPH"))
        assertEquals("62", convertWind(100.0, "MPH"))
    }

    @Test
    fun `convertWind ms`() {
        assertEquals("3", convertWind(10.0, "MS"))
    }

    @Test
    fun `convertWind knots`() {
        assertEquals("5", convertWind(10.0, "KNOTS"))
    }

    @Test
    fun `convertWind null returns dash`() {
        assertEquals("--", convertWind(null, "KPH"))
    }

    @Test
    fun `temperature at absolute zero`() {
        assertEquals(-273, convertTemp(-273.15, true))
        assertEquals(-460, convertTemp(-273.15, false))
    }
}