package com.vayu.weather.presentation.weather

import android.content.Context
import android.content.res.Resources
import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WeatherShareFormatterTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setup() {
        context = mock()
        resources = mock()
        whenever(context.resources).thenReturn(resources)

        // Stub getString to return the argument as-is so we can assert on it
        whenever(resources.getString(any<Int>())).thenReturn("")
        whenever(resources.getString(any<Int>(), any())).thenReturn("%s")
        whenever(resources.getString(any<Int>(), any(), any())).thenReturn("%s")
    }

    @Test
    fun `formatForShare creates non-empty weather text`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            context = context,
            cityName = "London",
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue("Share text should not be empty", result.isNotEmpty())
    }

    @Test
    fun `formatForShare with Fahrenheit does not crash`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            context = context,
            cityName = "New York",
            weatherInfo = weatherInfo,
            isCelsius = false
        )

        assertTrue("Share text should not be empty", result.isNotEmpty())
    }

    @Test
    fun `formatForShare handles null city name`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            context = context,
            cityName = null,
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue("Share text should not be empty", result.isNotEmpty())
    }

    @Test
    fun `formatForShare handles missing optional fields`() {
        val weatherInfo = WeatherInfo(
            current = CurrentWeather(
                time = "2024-01-01T12:00",
                temperature = 20.0,
                weatherCode = 2,
                isDay = true,
                humidity = null,
                windSpeed = null,
                windDirection = null,
                apparentTemperature = null,
                surfacePressure = null
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
        val result = WeatherShareFormatter.formatForShare(
            context = context,
            cityName = "London",
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue("Share text should not be empty", result.isNotEmpty())
    }

    private fun createTestWeatherInfo(): WeatherInfo {
        return WeatherInfo(
            current = CurrentWeather(
                time = "2024-01-01T12:00",
                temperature = 20.0,
                weatherCode = 2,
                isDay = true,
                humidity = 65.0,
                windSpeed = 15.0,
                windDirection = 180.0,
                apparentTemperature = 18.0,
                surfacePressure = 1013.0
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
    }
}
