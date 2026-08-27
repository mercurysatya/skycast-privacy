package com.vayu.weather.domain.repository

import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getWeatherData(lat: Double, long: Double): Result<WeatherInfo>
    suspend fun getAirQuality(lat: Double, long: Double): Result<AirQuality>
    suspend fun searchCity(query: String): Result<List<City>>
    fun getFavoriteCities(): Flow<List<City>>
    suspend fun addFavoriteCity(city: City)
    suspend fun removeFavoriteCity(city: City)
    suspend fun isFavoriteCity(cityId: Long): Boolean
    fun getRecentSearches(limit: Int = 10): Flow<List<City>>
    suspend fun addRecentSearch(city: City)
    suspend fun deleteRecentSearch(id: Long)
    suspend fun clearRecentSearches()

    suspend fun dedupeRecentSearches()
    fun getWeatherAlerts(limit: Int = 50): Flow<List<WeatherAlert>>
    suspend fun addWeatherAlert(alert: WeatherAlert)
    suspend fun deleteWeatherAlert(id: Long)
    suspend fun clearWeatherAlerts()
    suspend fun clearWeatherCache()
    suspend fun deleteStaleWeatherCache(maxAgeMs: Long)
    suspend fun deleteAllLocalData()

    // Weather History
    suspend fun saveWeatherSnapshot(snapshot: com.vayu.weather.domain.model.WeatherHistorySnapshot)
    fun getWeatherHistory(limit: Int = 500): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>>
    fun getWeatherHistorySince(since: Long): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>>
    suspend fun clearWeatherHistory()

    // On this day
    fun getWeatherHistoryForMonthDay(monthDay: String): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>>
    fun getWeatherHistoryForLocationAndMonthDay(lat: Double, lon: Double, monthDay: String): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>>
}

data class WeatherAlert(
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cityName: String? = null
)
