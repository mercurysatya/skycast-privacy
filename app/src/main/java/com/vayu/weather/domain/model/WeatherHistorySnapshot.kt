package com.vayu.weather.domain.model

/**
 * A single weather reading stored at a point in time for historical trends.
 */
data class WeatherHistorySnapshot(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Double?,
    val windSpeed: Double?,
    val apparentTemperature: Double?,
    val isDay: Boolean,
    val surfacePressure: Double?,
    val uvIndex: Double?,
    val precipitationProbability: Int?
)

/** Aggregated stats for a set of snapshots (e.g. daily summaries). */
data class WeatherHistoryDay(
    val date: String,
    val minTemp: Double,
    val maxTemp: Double,
    val avgTemp: Double,
    val weatherCode: Int,
    val humidity: Double?,
    val windSpeed: Double?,
    val snapshotCount: Int
)
