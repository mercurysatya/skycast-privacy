package com.vayu.weather.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherDto(
    @Json(name = "latitude")
    val latitude: Double,
    @Json(name = "longitude")
    val longitude: Double,
    @Json(name = "current")
    val current: CurrentWeatherDto? = null,
    @Json(name = "hourly")
    val hourly: HourlyWeatherDto? = null,
    @Json(name = "daily")
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "time")
    val time: String,
    @Json(name = "temperature_2m")
    val temperature: Double,
    @Json(name = "relative_humidity_2m")
    val humidity: Double? = null,
    @Json(name = "weather_code")
    val weatherCode: Int,
    @Json(name = "wind_speed_10m")
    val windSpeed: Double? = null,
    @Json(name = "wind_direction_10m")
    val windDirection: Double? = null,
    @Json(name = "apparent_temperature")
    val apparentTemperature: Double? = null,
    @Json(name = "is_day")
    val isDay: Int? = null,
    @Json(name = "visibility")
    val visibility: Double? = null,
    @Json(name = "surface_pressure")
    val surfacePressure: Double? = null,
    @Json(name = "wind_gusts_10m")
    val windGusts: Double? = null,
    @Json(name = "dew_point_2m")
    val dewPoint: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @Json(name = "time")
    val time: List<String?>,
    @Json(name = "temperature_2m")
    val temperatures: List<Double?>,
    @Json(name = "weather_code")
    val weatherCodes: List<Int?>,
    @Json(name = "relative_humidity_2m")
    val humidities: List<Double?>? = null,
    @Json(name = "pressure_msl")
    val pressures: List<Double?>? = null,
    @Json(name = "wind_speed_10m")
    val windSpeeds: List<Double?>? = null,
    @Json(name = "visibility")
    val visibility: List<Double?>? = null,
    @Json(name = "precipitation_probability")
    val precipitationProbabilities: List<Int?>? = null,
    @Json(name = "wind_direction_10m")
    val windDirections: List<Double?>? = null,
    @Json(name = "precipitation")
    val precipitations: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "time")
    val time: List<String?>,
    @Json(name = "weather_code")
    val weatherCodes: List<Int?>,
    @Json(name = "temperature_2m_max")
    val maxTemperatures: List<Double?>,
    @Json(name = "temperature_2m_min")
    val minTemperatures: List<Double?>,
    @Json(name = "uv_index_max")
    val uvIndices: List<Double?>? = null,
    @Json(name = "precipitation_probability_max")
    val precipitationProbabilities: List<Int?>? = null,
    @Json(name = "sunrise")
    val sunrise: List<String?>? = null,
    @Json(name = "sunset")
    val sunset: List<String?>? = null
)

/** Lightweight grid data response for temperature heatmap */
data class GridWeatherResponse(
    val temperature_2m: List<List<Double>>? = null,
    val wind_speed_10m: List<List<Double>>? = null,
    val wind_direction_10m: List<List<Double>>? = null
)
