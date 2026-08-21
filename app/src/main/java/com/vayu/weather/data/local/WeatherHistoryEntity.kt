package com.vayu.weather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_history")
data class WeatherHistoryEntity(
    @PrimaryKey(autoGenerate = true)
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
