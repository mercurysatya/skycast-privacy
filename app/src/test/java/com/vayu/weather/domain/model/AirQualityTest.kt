package com.vayu.weather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AirQualityTest {

    @Test
    fun `europeanAqi returns correct labels`() {
        val testCases = mapOf(
            10 to "Good",
            30 to "Fair",
            50 to "Moderate",
            70 to "Poor",
            90 to "Very Poor",
            110 to "Extremely Poor"
        )

        testCases.forEach { (aqi, expectedLabel) ->
            val airQuality = AirQuality(europeanAqi = aqi, usAqi = null)
            assertEquals(expectedLabel, airQuality.aqiLabel)
        }
    }

    @Test
    fun `usAqi returns correct labels`() {
        val testCases = mapOf(
            25 to "Good",
            75 to "Moderate",
            125 to "Unhealthy for Sensitive Groups",
            175 to "Unhealthy",
            250 to "Very Unhealthy",
            350 to "Hazardous"
        )

        testCases.forEach { (aqi, expectedLabel) ->
            val airQuality = AirQuality(europeanAqi = null, usAqi = aqi)
            assertEquals(expectedLabel, airQuality.aqiLabel)
        }
    }

    @Test
    fun `europeanAqi takes precedence over usAqi`() {
        val airQuality = AirQuality(europeanAqi = 30, usAqi = 75)
        assertEquals("Fair", airQuality.aqiLabel)
    }

    @Test
    fun `returns dashes when both aqi values are null`() {
        val airQuality = AirQuality(europeanAqi = null, usAqi = null)
        assertEquals("--", airQuality.aqiLabel)
        assertEquals(0, airQuality.aqiColorIndex)
    }

    @Test
    fun `europeanAqi color index returns correct values`() {
        val testCases = mapOf(
            10 to 1,
            30 to 2,
            50 to 3,
            70 to 4,
            90 to 5,
            110 to 6
        )

        testCases.forEach { (aqi, expectedIndex) ->
            val airQuality = AirQuality(europeanAqi = aqi, usAqi = null)
            assertEquals(expectedIndex, airQuality.aqiColorIndex)
        }
    }

    @Test
    fun `usAqi color index returns correct values`() {
        val testCases = mapOf(
            25 to 1,
            75 to 2,
            125 to 3,
            175 to 4,
            250 to 5,
            350 to 6
        )

        testCases.forEach { (aqi, expectedIndex) ->
            val airQuality = AirQuality(europeanAqi = null, usAqi = aqi)
            assertEquals(expectedIndex, airQuality.aqiColorIndex)
        }
    }
}
