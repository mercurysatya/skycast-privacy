package com.vayu.weather

import com.vayu.weather.data.mapper.toCity
import com.vayu.weather.data.mapper.toDailyWeather
import com.vayu.weather.data.mapper.toHourlyWeather
import com.vayu.weather.data.mapper.toWeatherInfo
import com.vayu.weather.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Test

class WeatherMapperTest {

    @Test
    fun `toWeatherInfo maps all fields correctly`() {
        val dto = WeatherDto(
            latitude = 40.7128,
            longitude = -74.0060,
            current = CurrentWeatherDto(
                time = "2024-01-15T14:00",
                temperature = 22.5,
                humidity = 65.0,
                weatherCode = 0,
                windSpeed = 15.0,
                windDirection = 180.0,
                apparentTemperature = 20.0,
                isDay = 1,
                visibility = 10000.0,
                surfacePressure = 1013.0,
                windGusts = 25.0,
                dewPoint = 15.0
            ),
            hourly = HourlyWeatherDto(
                time = listOf("2024-01-15T14:00", "2024-01-15T15:00"),
                temperatures = listOf(22.5, 21.0),
                weatherCodes = listOf(0, 1),
                humidities = listOf(65.0, 70.0),
                pressures = listOf(1013.0, 1012.0),
                windSpeeds = listOf(15.0, 12.0),
                visibility = listOf(10000.0, 8000.0)
            ),
            daily = DailyWeatherDto(
                time = listOf("2024-01-15"),
                weatherCodes = listOf(0),
                maxTemperatures = listOf(25.0),
                minTemperatures = listOf(18.0),
                uvIndices = listOf(5.0),
                precipitationProbabilities = listOf(10),
                sunrise = listOf("2024-01-15T07:15"),
                sunset = listOf("2024-01-15T16:45")
            )
        )

        val result = dto.toWeatherInfo()

        assertEquals(22.5, result.current.temperature, 0.001)
        assertEquals(65.0, result.current.humidity!!, 0.001)
        assertEquals(0, result.current.weatherCode)
        assertEquals(15.0, result.current.windSpeed!!, 0.001)
        assertEquals(180.0, result.current.windDirection!!, 0.001)
        assertEquals(20.0, result.current.apparentTemperature!!, 0.001)
        assertTrue(result.current.isDay)
        assertEquals(10000.0, result.current.visibility!!, 0.001)
        assertEquals(1013.0, result.current.surfacePressure!!, 0.001)
        assertEquals(25.0, result.current.windGusts!!, 0.001)
        assertEquals(15.0, result.current.dewPoint!!, 0.001)
    }

    @Test
    fun `toWeatherInfo handles null current gracefully`() {
        val dto = WeatherDto(
            latitude = 0.0,
            longitude = 0.0,
            current = null,
            hourly = HourlyWeatherDto(
                time = emptyList(),
                temperatures = emptyList(),
                weatherCodes = emptyList()
            ),
            daily = DailyWeatherDto(
                time = emptyList(),
                weatherCodes = emptyList(),
                maxTemperatures = emptyList(),
                minTemperatures = emptyList()
            )
        )

        val result = dto.toWeatherInfo()
        assertEquals("", result.current.time)
        assertEquals(0.0, result.current.temperature, 0.001)
        assertFalse(result.current.isDay)
        assertTrue(result.hourly.isEmpty())
        assertTrue(result.daily.isEmpty())
    }

    @Test
    fun `toHourlyWeather maps all fields`() {
        val dto = HourlyWeatherDto(
            time = listOf("2024-01-15T14:00"),
            temperatures = listOf(22.5),
            weatherCodes = listOf(0),
            humidities = listOf(65.0),
            pressures = listOf(1013.0),
            windSpeeds = listOf(15.0),
            visibility = listOf(10000.0)
        )

        val result = dto.toHourlyWeather()
        assertEquals(1, result.size)
        assertEquals("2024-01-15T14:00", result[0].time)
        assertEquals(22.5, result[0].temperature, 0.001)
        assertEquals(65.0, result[0].humidity!!, 0.001)
        assertEquals(1013.0, result[0].pressure!!, 0.001)
        assertEquals(15.0, result[0].windSpeed!!, 0.001)
        assertEquals(10000.0, result[0].visibility!!, 0.001)
    }

    @Test
    fun `toDailyWeather maps all fields`() {
        val dto = DailyWeatherDto(
            time = listOf("2024-01-15"),
            weatherCodes = listOf(0),
            maxTemperatures = listOf(25.0),
            minTemperatures = listOf(18.0),
            uvIndices = listOf(5.0),
            precipitationProbabilities = listOf(10),
            sunrise = listOf("2024-01-15T07:15"),
            sunset = listOf("2024-01-15T16:45")
        )

        val result = dto.toDailyWeather()
        assertEquals(1, result.size)
        assertEquals("2024-01-15", result[0].date)
        assertEquals(25.0, result[0].maxTemp, 0.001)
        assertEquals(18.0, result[0].minTemp, 0.001)
        assertEquals(5.0, result[0].uvIndex!!, 0.001)
        assertEquals(10, result[0].precipitationProbability)
        assertEquals("2024-01-15T07:15", result[0].sunrise)
        assertEquals("2024-01-15T16:45", result[0].sunset)
    }

    @Test
    fun `toDailyWeather handles null optional fields`() {
        val dto = DailyWeatherDto(
            time = listOf("2024-01-15"),
            weatherCodes = listOf(0),
            maxTemperatures = listOf(25.0),
            minTemperatures = listOf(18.0),
            uvIndices = null,
            precipitationProbabilities = null,
            sunrise = null,
            sunset = null
        )

        val result = dto.toDailyWeather()
        assertNull(result[0].uvIndex)
        assertNull(result[0].precipitationProbability)
        assertNull(result[0].sunrise)
        assertNull(result[0].sunset)
    }

    @Test
    fun `toCity maps all fields`() {
        val dto = GeocodingResultDto(
            id = 5128581,
            name = "New York",
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States",
            admin1 = "New York",
            countryCode = "US"
        )

        val result = dto.toCity()
        assertEquals(5128581L, result.id)
        assertEquals("New York", result.name)
        assertEquals(40.7128, result.latitude, 0.001)
        assertEquals(-74.0060, result.longitude, 0.001)
        assertEquals("United States", result.country)
        assertEquals("New York", result.admin1)
        assertEquals("US", result.countryCode)
    }
}