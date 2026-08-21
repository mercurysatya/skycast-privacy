package com.vayu.weather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_alerts")
data class WeatherAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double?,
    val longitude: Double?,
    val cityName: String?
)
