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
    /** Persists a new ordering for the given favorite cities (0 = first). */
    suspend fun reorderFavoriteCities(cities: List<City>)
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
    suspend fun findRecentWeatherAlert(title: String, severity: String, sinceTimestamp: Long): WeatherAlert?

    /**
     * Inserts the advisory or updates the existing one with the same
     * fingerprint. Repeated ingestion of the same logical advisory is
     * idempotent — it never creates a second row.
     */
    suspend fun upsertWeatherAlert(alert: WeatherAlert): WeatherAlertUpsertResult

    /** Marks the advisory with this fingerprint as no longer valid (kept for history). */
    suspend fun resolveWeatherAlert(
        fingerprint: String,
        resolvedAt: Long = System.currentTimeMillis()
    ): Boolean

    /** All advisories that are still valid right now. */
    fun getActiveWeatherAlerts(limit: Int = 50, now: Long = System.currentTimeMillis()): Flow<List<WeatherAlert>>
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

    /**
     * Returns the most recent snapshot from ≥18h ago for the given location,
     * or null if none has been recorded. Used for the "vs yesterday" pill.
     */
    suspend fun getYesterdaySnapshot(lat: Double, lon: Double): com.vayu.weather.domain.model.WeatherHistorySnapshot?
}

data class WeatherAlert(
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cityName: String? = null,
    /**
     * Stable identity of the advisory this row represents. Empty for rows that
     * predate the fingerprint (those are history, never active).
     */
    val fingerprint: String? = null,
    /**
     * Point in time until which the advisory is valid. `0` means "no validity
     * known" — such a row counts as expired.
     */
    val expiresAt: Long = 0L
) {
    /** An advisory is active while its validity window is open. */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = expiresAt > now
}

/** Outcome of [WeatherRepository.upsertWeatherAlert]. */
sealed class WeatherAlertUpsertResult {
    /** The advisory did not exist yet — a new row was created. */
    data class Created(val id: Long) : WeatherAlertUpsertResult()

    /**
     * The advisory existed and its content changed. [escalated] is true when
     * the severity went up — that is the only change that may notify.
     */
    data class Updated(val id: Long, val escalated: Boolean) : WeatherAlertUpsertResult()

    /** The advisory existed and is identical — nothing was written. */
    data class Unchanged(val id: Long) : WeatherAlertUpsertResult()
}
