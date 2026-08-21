package com.vayu.weather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherDescriptionTest {

    @Test
    fun `code 0 daytime returns Clear Sky`() {
        assertEquals("Clear Sky", WeatherDescription.getWeatherDescription(0, isDay = true))
    }

    @Test
    fun `code 0 nighttime returns Clear Night`() {
        assertEquals("Clear Night", WeatherDescription.getWeatherDescription(0, isDay = false))
    }

    @Test
    fun `code 1 returns Mainly Clear`() {
        assertEquals("Mainly Clear", WeatherDescription.getWeatherDescription(1, isDay = true))
    }

    @Test
    fun `code 2 returns Partly Cloudy`() {
        assertEquals("Partly Cloudy", WeatherDescription.getWeatherDescription(2, isDay = true))
    }

    @Test
    fun `code 3 returns Overcast`() {
        assertEquals("Overcast", WeatherDescription.getWeatherDescription(3, isDay = true))
    }

    @Test
    fun `code 45 returns Fog`() {
        assertEquals("Fog", WeatherDescription.getWeatherDescription(45, isDay = true))
    }

    @Test
    fun `code 48 returns Fog`() {
        assertEquals("Fog", WeatherDescription.getWeatherDescription(48, isDay = true))
    }

    @Test
    fun `code 51 returns Light Drizzle`() {
        assertEquals("Light Drizzle", WeatherDescription.getWeatherDescription(51, isDay = true))
    }

    @Test
    fun `code 53 returns Moderate Drizzle`() {
        assertEquals("Moderate Drizzle", WeatherDescription.getWeatherDescription(53, isDay = true))
    }

    @Test
    fun `code 55 returns Dense Drizzle`() {
        assertEquals("Dense Drizzle", WeatherDescription.getWeatherDescription(55, isDay = true))
    }

    @Test
    fun `code 61 returns Slight Rain`() {
        assertEquals("Slight Rain", WeatherDescription.getWeatherDescription(61, isDay = true))
    }

    @Test
    fun `code 63 returns Moderate Rain`() {
        assertEquals("Moderate Rain", WeatherDescription.getWeatherDescription(63, isDay = true))
    }

    @Test
    fun `code 65 returns Heavy Rain`() {
        assertEquals("Heavy Rain", WeatherDescription.getWeatherDescription(65, isDay = true))
    }

    @Test
    fun `code 71 returns Slight Snow`() {
        assertEquals("Slight Snow", WeatherDescription.getWeatherDescription(71, isDay = true))
    }

    @Test
    fun `code 73 returns Moderate Snow`() {
        assertEquals("Moderate Snow", WeatherDescription.getWeatherDescription(73, isDay = true))
    }

    @Test
    fun `code 75 returns Heavy Snow`() {
        assertEquals("Heavy Snow", WeatherDescription.getWeatherDescription(75, isDay = true))
    }

    @Test
    fun `code 80 returns Slight Rain Showers`() {
        assertEquals("Slight Rain Showers", WeatherDescription.getWeatherDescription(80, isDay = true))
    }

    @Test
    fun `code 81 returns Moderate Rain Showers`() {
        assertEquals("Moderate Rain Showers", WeatherDescription.getWeatherDescription(81, isDay = true))
    }

    @Test
    fun `code 82 returns Violent Rain Showers`() {
        assertEquals("Violent Rain Showers", WeatherDescription.getWeatherDescription(82, isDay = true))
    }

    @Test
    fun `code 95 returns Thunderstorm`() {
        assertEquals("Thunderstorm", WeatherDescription.getWeatherDescription(95, isDay = true))
    }

    @Test
    fun `code 96 returns Thunderstorm with Hail`() {
        assertEquals("Thunderstorm with Hail", WeatherDescription.getWeatherDescription(96, isDay = true))
    }

    @Test
    fun `code 99 returns Thunderstorm with Heavy Hail`() {
        assertEquals("Thunderstorm with Heavy Hail", WeatherDescription.getWeatherDescription(99, isDay = true))
    }

    @Test
    fun `unknown code returns Cloudy`() {
        assertEquals("Cloudy", WeatherDescription.getWeatherDescription(999, isDay = true))
    }

    @Test
    fun `negative code returns Cloudy`() {
        assertEquals("Cloudy", WeatherDescription.getWeatherDescription(-1, isDay = true))
    }
}
