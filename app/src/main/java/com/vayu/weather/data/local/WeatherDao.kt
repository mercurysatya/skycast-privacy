package com.vayu.weather.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM favorite_cities")
    fun getFavoriteCities(): Flow<List<FavoriteCityEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cities WHERE id = :cityId)")
    suspend fun isCityFavorite(cityId: Long): Boolean

    @Query("DELETE FROM favorite_cities")
    suspend fun deleteAllFavoriteCities()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCity(city: FavoriteCityEntity)

    @Delete
    suspend fun deleteFavoriteCity(city: FavoriteCityEntity)

    @Query("SELECT * FROM weather_cache WHERE locationId = :locationId")
    suspend fun getWeatherCache(locationId: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache")
    suspend fun clearWeatherCache()

    @Query("DELETE FROM weather_cache WHERE locationId = :locationId")
    suspend fun deleteWeatherCacheById(locationId: String)

    @Query("DELETE FROM weather_cache WHERE lastUpdated < :timestamp")
    suspend fun deleteStaleCache(timestamp: Long)

    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches WHERE name = :name AND latitude = :lat AND longitude = :lon LIMIT 1")
    suspend fun findRecentSearch(name: String, lat: Double, lon: Double): RecentSearchEntity?

    @Query("DELETE FROM recent_searches WHERE id NOT IN (SELECT id FROM recent_searches ORDER BY timestamp DESC LIMIT 10)")
    suspend fun trimRecentSearches()

    @Query("DELETE FROM recent_searches WHERE id = :id")
    suspend fun deleteRecentSearch(id: Long)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    @Query("SELECT * FROM weather_alerts ORDER BY timestamp DESC LIMIT :limit")
    fun getWeatherAlerts(limit: Int = 50): Flow<List<WeatherAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherAlert(alert: WeatherAlertEntity)

    @Query("DELETE FROM weather_alerts WHERE id = :id")
    suspend fun deleteWeatherAlert(id: Long)

    @Query("DELETE FROM weather_alerts")
    suspend fun clearWeatherAlerts()

    @Query("DELETE FROM weather_alerts WHERE id NOT IN (SELECT id FROM weather_alerts ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimWeatherAlerts()

    // ---- Weather History ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherHistory(snapshot: WeatherHistoryEntity)

    @Query("SELECT * FROM weather_history ORDER BY timestamp DESC LIMIT :limit")
    fun getWeatherHistory(limit: Int = 500): Flow<List<WeatherHistoryEntity>>

    @Query("SELECT * FROM weather_history WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getWeatherHistorySince(since: Long): Flow<List<WeatherHistoryEntity>>

    @Query("SELECT * FROM weather_history WHERE latitude = :lat AND longitude = :lon AND timestamp >= :since ORDER BY timestamp ASC")
    fun getWeatherHistoryForLocation(lat: Double, lon: Double, since: Long): Flow<List<WeatherHistoryEntity>>

    @Query("DELETE FROM weather_history")
    suspend fun clearWeatherHistory()

    @Query("SELECT COUNT(*) FROM weather_history")
    suspend fun getWeatherHistoryCount(): Int

    @Query("DELETE FROM weather_history WHERE id IN (SELECT id FROM weather_history ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestWeatherHistory(count: Int)
}
