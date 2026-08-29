package com.vayu.weather.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherInfo(
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

@Serializable
data class CurrentWeather(
    val time: String,
    val temperature: Double,
    val humidity: Double?,
    val weatherCode: Int,
    val windSpeed: Double?,
    val windDirection: Double?,
    val apparentTemperature: Double?,
    val isDay: Boolean,
    val visibility: Double? = null,
    val surfacePressure: Double? = null,
    val windGusts: Double? = null,
    val dewPoint: Double? = null
)

@Serializable
data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Double?,
    val pressure: Double?,
    val windSpeed: Double?,
    val visibility: Double? = null,
    val precipitationProbability: Int? = null,
    val windDirection: Double? = null,
    val precipitation: Double? = null,
    val apparentTemperature: Double? = null
)

@Serializable
data class DailyWeather(
    val date: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val uvIndex: Double?,
    val precipitationProbability: Int?,
    val sunrise: String? = null,
    val sunset: String? = null
)
