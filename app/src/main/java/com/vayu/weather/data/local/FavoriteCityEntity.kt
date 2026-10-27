package com.vayu.weather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cities")
data class FavoriteCityEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
    val countryCode: String?,
    /** Stable, user-controlled ordering (0 = first). Set to rowid by MIGRATION_4_5. */
    val sortIndex: Int = 0
)
