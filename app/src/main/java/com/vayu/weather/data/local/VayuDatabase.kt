package com.vayu.weather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteCityEntity::class,
        WeatherCacheEntity::class,
        RecentSearchEntity::class,
        WeatherAlertEntity::class,
        WeatherHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VayuDatabase : RoomDatabase() {
    abstract val weatherDao: WeatherDao
}
