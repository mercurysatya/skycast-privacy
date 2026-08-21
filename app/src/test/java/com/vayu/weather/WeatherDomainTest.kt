package com.vayu.weather

import com.vayu.weather.domain.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WeatherDomainTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `WeatherInfo serialization roundtrip`() {
        val original = WeatherInfo(
            current = CurrentWeather(
                time = "2024-01-15T14:00",
                temperature = 22.5,
                humidity = 65.0,
                weatherCode = 0,
                windSpeed = 15.0,
                windDirection = 180.0,
                apparentTemperature = 20.0,
                isDay = true,
                visibility = 10000.0,
                surfacePressure = 1013.0,
                windGusts = 25.0,
                dewPoint = 15.0
            ),
            hourly = listOf(
                HourlyWeather(
                    time = "2024-01-15T14:00",
                    temperature = 22.5,
                    weatherCode = 0,
                    humidity = 65.0,
                    pressure = 1013.0,
                    windSpeed = 15.0,
                    visibility = 10000.0
                )
            ),
            daily = listOf(
                DailyWeather(
                    date = "2024-01-15",
                    weatherCode = 0,
                    maxTemp = 25.0,
                    minTemp = 18.0,
                    uvIndex = 5.0,
                    precipitationProbability = 10,
                    sunrise = "2024-01-15T07:15",
                    sunset = "2024-01-15T16:45"
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WeatherInfo>(encoded)

        assertEquals(original.current.temperature, decoded.current.temperature, 0.001)
        assertEquals(original.current.humidity, decoded.current.humidity)
        assertEquals(original.current.visibility, decoded.current.visibility)
        assertEquals(original.current.surfacePressure, decoded.current.surfacePressure)
        assertEquals(original.current.isDay, decoded.current.isDay)
        assertEquals(original.current.windGusts, decoded.current.windGusts)
        assertEquals(original.current.dewPoint, decoded.current.dewPoint)
        assertEquals(original.hourly.size, decoded.hourly.size)
        assertEquals(original.hourly[0].visibility, decoded.hourly[0].visibility)
        assertEquals(original.daily.size, decoded.daily.size)
        assertEquals(original.daily[0].sunrise, decoded.daily[0].sunrise)
        assertEquals(original.daily[0].sunset, decoded.daily[0].sunset)
    }

    @Test
    fun `CurrentWeather defaults are correct`() {
        val weather = CurrentWeather(
            time = "2024-01-15T14:00",
            temperature = 0.0,
            humidity = null,
            weatherCode = 0,
            windSpeed = null,
            windDirection = null,
            apparentTemperature = null,
            isDay = true
        )
        assertNull(weather.visibility)
        assertNull(weather.surfacePressure)
        assertNull(weather.windGusts)
        assertNull(weather.dewPoint)
    }

    @Test
    fun `HourlyWeather defaults are correct`() {
        val hourly = HourlyWeather(
            time = "2024-01-15T14:00",
            temperature = 22.5,
            weatherCode = 0,
            humidity = null,
            pressure = null,
            windSpeed = null
        )
        assertNull(hourly.visibility)
    }

    @Test
    fun `DailyWeather defaults are correct`() {
        val daily = DailyWeather(
            date = "2024-01-15",
            weatherCode = 0,
            maxTemp = 25.0,
            minTemp = 18.0,
            uvIndex = null,
            precipitationProbability = null
        )
        assertNull(daily.sunrise)
        assertNull(daily.sunset)
    }

    @Test
    fun `WeatherInfo with null windGusts and dewPoint deserializes correctly`() {
        val weather = WeatherInfo(
            current = CurrentWeather(
                time = "2024-01-15T14:00",
                temperature = 22.5,
                humidity = 65.0,
                weatherCode = 0,
                windSpeed = 15.0,
                windDirection = 180.0,
                apparentTemperature = 20.0,
                isDay = true,
                visibility = 10000.0,
                surfacePressure = 1013.0,
                windGusts = null,
                dewPoint = null
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
        val encoded = json.encodeToString(weather)
        val decoded = json.decodeFromString<WeatherInfo>(encoded)
        assertNull(decoded.current.windGusts)
        assertNull(decoded.current.dewPoint)
    }
}