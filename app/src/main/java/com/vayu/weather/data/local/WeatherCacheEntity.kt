package com.vayu.weather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationId: String, // lat,long
    val weatherDataJson: String,
    val lastUpdated: Long
)
