package com.vayu.weather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AirQualityTest {

    // ==================== European AQI Labels ====================

    @Test
    fun `european AQI 0-20 returns Good`() {
        val aqi = createAirQuality(europeanAqi = 10)
        assertEquals("Good", aqi.aqiLabel)
    }

    @Test
    fun `european AQI 21-40 returns Fair`() {
        val aqi = createAirQuality(europeanAqi = 30)
        assertEquals("Fair", aqi.aqiLabel)
    }

    @Test
    fun `european AQI 41-60 returns Moderate`() {
        val aqi = createAirQuality(europeanAqi = 50)
        assertEquals("Moderate", aqi.aqiLabel)
    }

    @Test
    fun `european AQI 61-80 returns Poor`() {
        val aqi = createAirQuality(europeanAqi = 70)
        assertEquals("Poor", aqi.aqiLabel)
    }

    @Test
    fun `european AQI 81-100 returns Very Poor`() {
        val aqi = createAirQuality(europeanAqi = 90)
        assertEquals("Very Poor", aqi.aqiLabel)
    }

    @Test
    fun `european AQI above 100 returns Extremely Poor`() {
        val aqi = createAirQuality(europeanAqi = 150)
        assertEquals("Extremely Poor", aqi.aqiLabel)
    }

    // ==================== European AQI Color Indices ====================

    @Test
    fun `european AQI 0-20 returns color index 1`() {
        assertEquals(1, createAirQuality(europeanAqi = 15).aqiColorIndex)
    }

    @Test
    fun `european AQI 21-40 returns color index 2`() {
        assertEquals(2, createAirQuality(europeanAqi = 35).aqiColorIndex)
    }

    @Test
    fun `european AQI 41-60 returns color index 3`() {
        assertEquals(3, createAirQuality(europeanAqi = 55).aqiColorIndex)
    }

    @Test
    fun `european AQI 61-80 returns color index 4`() {
        assertEquals(4, createAirQuality(europeanAqi = 75).aqiColorIndex)
    }

    @Test
    fun `european AQI 81-100 returns color index 5`() {
        assertEquals(5, createAirQuality(europeanAqi = 95).aqiColorIndex)
    }

    @Test
    fun `european AQI above 100 returns color index 6`() {
        assertEquals(6, createAirQuality(europeanAqi = 120).aqiColorIndex)
    }

    // ==================== US AQI Labels ====================

    @Test
    fun `us AQI 0-50 returns Good`() {
        assertEquals("Good", createAirQuality(usAqi = 25).aqiLabel)
    }

    @Test
    fun `us AQI 51-100 returns Moderate`() {
        assertEquals("Moderate", createAirQuality(usAqi = 75).aqiLabel)
    }

    @Test
    fun `us AQI 101-150 returns Unhealthy for Sensitive Groups`() {
        assertEquals("Unhealthy for Sensitive Groups", createAirQuality(usAqi = 125).aqiLabel)
    }

    @Test
    fun `us AQI 151-200 returns Unhealthy`() {
        assertEquals("Unhealthy", createAirQuality(usAqi = 175).aqiLabel)
    }

    @Test
    fun `us AQI 201-300 returns Very Unhealthy`() {
        assertEquals("Very Unhealthy", createAirQuality(usAqi = 250).aqiLabel)
    }

    @Test
    fun `us AQI above 300 returns Hazardous`() {
        assertEquals("Hazardous", createAirQuality(usAqi = 400).aqiLabel)
    }

    // ==================== US AQI Color Indices ====================

    @Test
    fun `us AQI 0-50 returns color index 1`() {
        assertEquals(1, createAirQuality(usAqi = 30).aqiColorIndex)
    }

    @Test
    fun `us AQI 51-100 returns color index 2`() {
        assertEquals(2, createAirQuality(usAqi = 80).aqiColorIndex)
    }

    @Test
    fun `us AQI 101-150 returns color index 3`() {
        assertEquals(3, createAirQuality(usAqi = 130).aqiColorIndex)
    }

    @Test
    fun `us AQI 151-200 returns color index 4`() {
        assertEquals(4, createAirQuality(usAqi = 180).aqiColorIndex)
    }

    @Test
    fun `us AQI 201-300 returns color index 5`() {
        assertEquals(5, createAirQuality(usAqi = 260).aqiColorIndex)
    }

    @Test
    fun `us AQI above 300 returns color index 6`() {
        assertEquals(6, createAirQuality(usAqi = 350).aqiColorIndex)
    }

    // ==================== Fallback behavior ====================

    @Test
    fun `both AQI null returns default label`() {
        val aqi = AirQuality(
            europeanAqi = null, usAqi = null,
            pm25 = null, pm10 = null, nitrogenDioxide = null,
            ozone = null, sulphurDioxide = null, carbonMonoxide = null
        )
        assertEquals("--", aqi.aqiLabel)
    }

    @Test
    fun `both AQI null returns color index 0`() {
        val aqi = AirQuality(
            europeanAqi = null, usAqi = null,
            pm25 = null, pm10 = null, nitrogenDioxide = null,
            ozone = null, sulphurDioxide = null, carbonMonoxide = null
        )
        assertEquals(0, aqi.aqiColorIndex)
    }

    @Test
    fun `european AQI takes priority over US when both present`() {
        val aqi = createAirQuality(europeanAqi = 15, usAqi = 250)
        assertEquals("Good", aqi.aqiLabel)
        assertEquals(1, aqi.aqiColorIndex)
    }

    // ==================== Boundary values ====================

    @Test
    fun `european AQI exactly 20 is Good`() {
        assertEquals("Good", createAirQuality(europeanAqi = 20).aqiLabel)
    }

    @Test
    fun `european AQI exactly 21 is Fair`() {
        assertEquals("Fair", createAirQuality(europeanAqi = 21).aqiLabel)
    }

    @Test
    fun `us AQI exactly 50 is Good`() {
        assertEquals("Good", createAirQuality(usAqi = 50).aqiLabel)
    }

    @Test
    fun `us AQI exactly 51 is Moderate`() {
        assertEquals("Moderate", createAirQuality(usAqi = 51).aqiLabel)
    }

    private fun createAirQuality(
        europeanAqi: Int? = null,
        usAqi: Int? = null
    ) = AirQuality(
        europeanAqi = europeanAqi,
        usAqi = usAqi,
        pm25 = null, pm10 = null, nitrogenDioxide = null,
        ozone = null, sulphurDioxide = null, carbonMonoxide = null
    )
}
