package com.vayu.weather.presentation.weather

import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherShareFormatterTest {

    @Test
    fun `formatForShare creates proper weather text in Celsius`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            cityName = "London",
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue(result.contains("SkyCast Weather"))
        assertTrue(result.contains("Location: London"))
        assertTrue(result.contains("Temperature: 20°C"))
        assertTrue(result.contains("Condition: Partly Cloudy"))
        assertTrue(result.contains("Humidity: 65%"))
        assertTrue(result.contains("Wind: 15 km/h"))
        assertTrue(result.contains("Feels Like: 18°C"))
        assertTrue(result.contains("Pressure: 1013 hPa"))
    }

    @Test
    fun `formatForShare creates proper weather text in Fahrenheit`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            cityName = "New York",
            weatherInfo = weatherInfo,
            isCelsius = false
        )

        assertTrue(result.contains("Temperature: 68°F"))
        assertTrue(result.contains("Feels Like: 64°F"))
    }

    @Test
    fun `formatForShare handles null city name`() {
        val weatherInfo = createTestWeatherInfo()
        val result = WeatherShareFormatter.formatForShare(
            cityName = null,
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue(result.contains("Location: Current Location"))
    }

    @Test
    fun `formatForShare handles missing optional fields`() {
        val weatherInfo = WeatherInfo(
            current = CurrentWeather(
                temperature = 20.0,
                weatherCode = 2,
                isDay = true,
                humidity = null,
                windSpeed = null,
                apparentTemperature = null,
                surfacePressure = null
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
        val result = WeatherShareFormatter.formatForShare(
            cityName = "London",
            weatherInfo = weatherInfo,
            isCelsius = true
        )

        assertTrue(result.contains("Humidity: --"))
        assertTrue(result.contains("Wind: --"))
    }

    private fun createTestWeatherInfo(): WeatherInfo {
        return WeatherInfo(
            current = CurrentWeather(
                temperature = 20.0,
                weatherCode = 2,
                isDay = true,
                humidity = 65.0,
                windSpeed = 15.0,
                apparentTemperature = 18.0,
                surfacePressure = 1013.0
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
    }
}
