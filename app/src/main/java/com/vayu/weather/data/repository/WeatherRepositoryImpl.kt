package com.vayu.weather.data.repository

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.vayu.weather.data.local.FavoriteCityEntity
import com.vayu.weather.data.local.RecentSearchEntity
import com.vayu.weather.data.local.WeatherAlertEntity
import com.vayu.weather.data.local.WeatherCacheEntity
import com.vayu.weather.data.local.WeatherDao
import com.vayu.weather.data.local.WeatherHistoryEntity
import com.vayu.weather.data.mapper.toAirQuality
import com.vayu.weather.data.mapper.toCity
import com.vayu.weather.data.mapper.toWeatherInfo
import com.vayu.weather.data.remote.GeocodingApi
import com.vayu.weather.data.remote.OpenMeteoApi
import com.vayu.weather.data.remote.OpenMeteoAirQualityApi
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherAlertUpsertResult
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi,
    private val airQualityApi: OpenMeteoAirQualityApi,
    private val geocodingApi: GeocodingApi,
    private val dao: WeatherDao,
    private val db: com.vayu.weather.data.local.VayuDatabase,
    private val application: Application
) : WeatherRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private fun isNetworkAvailable(): Boolean {
        val cm = application.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L

        /** Cached rows older than this are pruned; the offline fallback only
         * needs the most recent state per location. */
        private const val CACHE_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L

        /** In-memory cache for air quality: key = "lat,long", value = Pair(timestamp, AirQuality). */
        private val aqiCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, AirQuality>>()

        internal fun cacheKeyFor(lat: Double, lon: Double): String =
            java.util.Locale.US.let { "%.3f,%.3f".format(it, lat, lon) }
    }

    override suspend fun getWeatherData(lat: Double, long: Double): Result<WeatherInfo> {
        // Quantize the key: raw GPS coordinates differ on every fix, so the
        // exact-coordinate key never hit the 30-min TTL and inserted a new row
        // on every refresh. 3 decimals ≈ 110 m.
        val locationId = cacheKeyFor(lat, long)

        // Check cache first
        val cached = dao.getWeatherCache(locationId)
        val cacheAge = System.currentTimeMillis() - (cached?.lastUpdated ?: 0)
        if (cached != null && cacheAge < CACHE_TTL_MS) {
            try {
                val decoded = json.decodeFromString<WeatherInfo>(cached.weatherDataJson)
                return Result.success(decoded)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Corrupt or schema-stale cache row: drop it and fall through to network refresh
                dao.deleteWeatherCacheById(locationId)
            }
        }

        // Check network
        if (!isNetworkAvailable()) {
            // Try to use stale cache as fallback
            if (cached != null) {
                return try {
                    Result.success(json.decodeFromString<WeatherInfo>(cached.weatherDataJson))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Result.failure(Exception("No internet connection"))
                }
            }
            return Result.failure(Exception("No internet connection"))
        }

        // Fetch from network
        return try {
            val response = api.getWeatherData(lat, long)
            val weatherInfo = response.toWeatherInfo()
            val cacheEntity = WeatherCacheEntity(
                locationId = locationId,
                weatherDataJson = json.encodeToString(weatherInfo),
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertWeatherCache(cacheEntity)
            // Opportunistically prune rows the exact-coordinate bug left behind
            // (and any genuinely stale entries).
            dao.deleteStaleCache(System.currentTimeMillis() - CACHE_MAX_AGE_MS)
            Result.success(weatherInfo)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Fallback to stale cache
            if (cached != null) {
                try {
                    return Result.success(json.decodeFromString<WeatherInfo>(cached.weatherDataJson))
                } catch (_: Exception) {}
            }
            Result.failure(e)
        }
    }

    override suspend fun getAirQuality(lat: Double, long: Double): Result<AirQuality> {
        return try {
            val cacheKey = "%.2f,%.2f".format(lat, long)
            val cached = aqiCache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL_MS) {
                return Result.success(cached.second)
            }
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("No internet connection"))
            }
            val response = airQualityApi.getAirQuality(lat, long)
            val airQuality = response.current?.toAirQuality()
                ?: return Result.failure(Exception("No air quality data available"))
            aqiCache[cacheKey] = System.currentTimeMillis() to airQuality
            Result.success(airQuality)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun searchCity(query: String): Result<List<City>> {
        return try {
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("No internet connection"))
            }
            val response = geocodingApi.searchCity(query)
            Result.success(response.results?.map { it.toCity() } ?: emptyList())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun getFavoriteCities(): Flow<List<City>> {
        return dao.getFavoriteCities().map { entities ->
            entities.map { entity ->
                City(
                    id = entity.id,
                    name = entity.name,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    country = entity.country,
                    admin1 = entity.admin1,
                    countryCode = entity.countryCode
                )
            }
        }
    }

    override suspend fun addFavoriteCity(city: City) {
        // Append to the end of the user's existing order.
        val nextIndex = dao.getMaxFavoriteSortIndex() + 1
        dao.insertFavoriteCity(
            FavoriteCityEntity(
                id = city.id,
                name = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                country = city.country,
                admin1 = city.admin1,
                countryCode = city.countryCode,
                sortIndex = nextIndex
            )
        )
    }

    override suspend fun removeFavoriteCity(city: City) {
        dao.deleteFavoriteCity(
            FavoriteCityEntity(
                id = city.id,
                name = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                country = city.country,
                admin1 = city.admin1,
                countryCode = city.countryCode
            )
        )
    }

    override suspend fun reorderFavoriteCities(cities: List<City>) {
        db.withTransaction {
            cities.forEachIndexed { index, city ->
                dao.updateFavoriteSortIndex(city.id, index)
            }
        }
    }

    override suspend fun isFavoriteCity(cityId: Long): Boolean {
        return dao.isCityFavorite(cityId)
    }

    override fun getRecentSearches(limit: Int): Flow<List<City>> {
        return dao.getRecentSearches(limit).map { entities ->
            entities.map { entity ->
                City(
                    id = entity.id,
                    name = entity.name,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    country = entity.country,
                    admin1 = entity.admin1,
                    countryCode = entity.countryCode
                )
            }
        }
    }

    override suspend fun addRecentSearch(city: City) {
        // Dedup: re-searching the same place moves it to the top instead of piling up rows
        dao.findRecentSearch(city.name, city.latitude, city.longitude)?.let { existing ->
            dao.deleteRecentSearch(existing.id)
        }
        dao.insertRecentSearch(
            RecentSearchEntity(
                id = 0,
                name = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                country = city.country,
                admin1 = city.admin1,
                countryCode = city.countryCode
            )
        )
        // Keep only the 10 most recent searches so the table cannot grow unbounded
        dao.trimRecentSearches()
    }

    override suspend fun deleteRecentSearch(id: Long) {
        dao.deleteRecentSearch(id)
    }

    override suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }

    override suspend fun dedupeRecentSearches() {
        dao.dedupeRecentSearches()
    }

    override fun getWeatherAlerts(limit: Int): Flow<List<WeatherAlert>> {
        return dao.getWeatherAlerts(limit).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override fun getActiveWeatherAlerts(limit: Int, now: Long): Flow<List<WeatherAlert>> {
        return dao.getActiveWeatherAlerts(limit, now).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    /**
     * Idempotent advisory write.
     *
     * The advisory is identified by its fingerprint, so ingesting the same
     * advisory again updates the existing row instead of inserting a second
     * one. The unique index on the fingerprint is the last line of defence
     * against concurrent writers: if two coroutines race, the second insert
     * fails with a constraint violation and is retried as an update.
     */
    override suspend fun upsertWeatherAlert(alert: WeatherAlert): WeatherAlertUpsertResult {
        val fingerprint = alert.fingerprint.orEmpty()
        if (fingerprint.isEmpty()) {
            // Without an identity there is nothing to deduplicate on — keep the
            // old insert behaviour for callers that do not have one.
            val id = dao.insertWeatherAlert(alert.toEntity())
            dao.trimWeatherAlerts()
            return WeatherAlertUpsertResult.Created(id)
        }

        val existing = dao.getWeatherAlertByFingerprint(fingerprint)
        if (existing == null) {
            try {
                val id = dao.insertWeatherAlert(alert.toEntity())
                dao.trimWeatherAlerts()
                return WeatherAlertUpsertResult.Created(id)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                // Another writer inserted the same advisory first — fall
                // through and update that row instead.
                return updateExisting(fingerprint, alert)
            }
        }
        return updateExisting(fingerprint, alert)
    }

    private suspend fun updateExisting(
        fingerprint: String,
        alert: WeatherAlert
    ): WeatherAlertUpsertResult {
        val existing = dao.getWeatherAlertByFingerprint(fingerprint)
            ?: return WeatherAlertUpsertResult.Unchanged(0L)
        val contentChanged = existing.message != alert.message ||
            existing.severity != alert.severity
        if (contentChanged) {
            dao.updateWeatherAlertContent(
                id = existing.id,
                message = alert.message,
                severity = alert.severity,
                timestamp = alert.timestamp,
                latitude = alert.latitude,
                longitude = alert.longitude,
                cityName = alert.cityName,
                expiresAt = alert.expiresAt
            )
            return WeatherAlertUpsertResult.Updated(
                id = existing.id,
                escalated = severityRank(alert.severity) > severityRank(existing.severity)
            )
        }
        // Same content: only the validity may need to be extended (the
        // forecast day rolled over). No new row, no new timestamp.
        if (existing.expiresAt != alert.expiresAt) {
            dao.updateWeatherAlertExpiry(existing.id, alert.expiresAt)
        }
        return WeatherAlertUpsertResult.Unchanged(existing.id)
    }

    override suspend fun resolveWeatherAlert(fingerprint: String, resolvedAt: Long): Boolean {
        val existing = dao.getWeatherAlertByFingerprint(fingerprint) ?: return false
        if (existing.expiresAt <= resolvedAt) return false
        // The advisory is over — close its validity window. The row itself is
        // kept so the alert history stays complete.
        dao.updateWeatherAlertExpiry(existing.id, resolvedAt)
        return true
    }

    private fun severityRank(severity: String): Int = when (severity.lowercase()) {
        "high" -> 3
        "medium" -> 2
        "low" -> 1
        else -> 0
    }

    private fun WeatherAlertEntity.toDomain() = WeatherAlert(
        id = id,
        title = title,
        message = message,
        severity = severity,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        cityName = cityName,
        fingerprint = fingerprint.ifEmpty { null },
        expiresAt = expiresAt
    )

    private fun WeatherAlert.toEntity() = WeatherAlertEntity(
        id = id,
        title = title,
        message = message,
        severity = severity,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        cityName = cityName,
        fingerprint = fingerprint.orEmpty(),
        expiresAt = expiresAt
    )

    override suspend fun addWeatherAlert(alert: WeatherAlert) {
        dao.insertWeatherAlert(alert.toEntity())
        // Keep only the most recent 100 alerts
        dao.trimWeatherAlerts()
    }

    override suspend fun deleteWeatherAlert(id: Long) {
        dao.deleteWeatherAlert(id)
    }

    override suspend fun clearWeatherAlerts() {
        dao.clearWeatherAlerts()
    }

    override suspend fun findRecentWeatherAlert(title: String, severity: String, sinceTimestamp: Long): WeatherAlert? {
        val entity = dao.findRecentWeatherAlert(title, severity, sinceTimestamp) ?: return null
        return WeatherAlert(
            id = entity.id,
            title = entity.title,
            message = entity.message,
            severity = entity.severity,
            timestamp = entity.timestamp,
            latitude = entity.latitude,
            longitude = entity.longitude,
            cityName = entity.cityName,
            fingerprint = entity.fingerprint.ifEmpty { null },
            expiresAt = entity.expiresAt
        )
    }

    override suspend fun clearWeatherCache() {
        dao.clearWeatherCache()
    }

    override suspend fun deleteStaleWeatherCache(maxAgeMs: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        dao.deleteStaleCache(cutoff)
    }

    override suspend fun deleteAllLocalData() {
        // Atomic so process death cannot leave a half-wiped database
        db.withTransaction {
            dao.clearWeatherCache()
            dao.clearRecentSearches()
            dao.clearWeatherAlerts()
            dao.clearWeatherHistory()
            dao.deleteAllFavoriteCities()
        }
        // Also wipe the non-Room stores the privacy wipe used to miss:
        // the in-memory AQI cache, the widget snapshot and the streak prefs.
        aqiCache.clear()
        application.deleteSharedPreferences("weather_widget_prefs")
        application.deleteSharedPreferences("weather_streak")
    }

    // ---- Weather History ----

    override suspend fun saveWeatherSnapshot(snapshot: com.vayu.weather.domain.model.WeatherHistorySnapshot) {
        dao.insertWeatherHistory(
            WeatherHistoryEntity(
                id = snapshot.id,
                timestamp = snapshot.timestamp,
                cityName = snapshot.cityName,
                latitude = snapshot.latitude,
                longitude = snapshot.longitude,
                temperature = snapshot.temperature,
                weatherCode = snapshot.weatherCode,
                humidity = snapshot.humidity,
                windSpeed = snapshot.windSpeed,
                apparentTemperature = snapshot.apparentTemperature,
                isDay = snapshot.isDay,
                surfacePressure = snapshot.surfacePressure,
                uvIndex = snapshot.uvIndex,
                precipitationProbability = snapshot.precipitationProbability
            )
        )
        // Keep only the most recent 2000 snapshots (~2 weeks at hourly saves)
        val count = dao.getWeatherHistoryCount()
        if (count > 2000) {
            dao.deleteOldestWeatherHistory(count - 2000)
        }
    }

    override fun getWeatherHistory(limit: Int): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>> {
        return dao.getWeatherHistory(limit).map { entities ->
            entities.map { it.toSnapshot() }
        }
    }

    override fun getWeatherHistorySince(since: Long): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>> {
        return dao.getWeatherHistorySince(since).map { entities ->
            entities.map { it.toSnapshot() }
        }
    }

    override suspend fun clearWeatherHistory() {
        dao.clearWeatherHistory()
    }

    private fun WeatherHistoryEntity.toSnapshot() = com.vayu.weather.domain.model.WeatherHistorySnapshot(
        id = id,
        timestamp = timestamp,
        cityName = cityName,
        latitude = latitude,
        longitude = longitude,
        temperature = temperature,
        weatherCode = weatherCode,
        humidity = humidity,
        windSpeed = windSpeed,
        apparentTemperature = apparentTemperature,
        isDay = isDay,
        surfacePressure = surfacePressure,
        uvIndex = uvIndex,
        precipitationProbability = precipitationProbability
    )

    // On this day - get history for a specific month/day
    override fun getWeatherHistoryForMonthDay(monthDay: String): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>> {
        return dao.getWeatherHistoryForMonthDay(monthDay).map { entities ->
            entities.map { it.toSnapshot() }
        }
    }

    // On this day for specific location
    override fun getWeatherHistoryForLocationAndMonthDay(lat: Double, lon: Double, monthDay: String): Flow<List<com.vayu.weather.domain.model.WeatherHistorySnapshot>> {
        return dao.getWeatherHistoryForLocationAndMonthDay(lat, lon, monthDay).map { entities ->
            entities.map { it.toSnapshot() }
        }
    }

    override suspend fun getYesterdaySnapshot(lat: Double, lon: Double): com.vayu.weather.domain.model.WeatherHistorySnapshot? {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60L * 60L * 1000L
        val window = 3L * 60L * 60L * 1000L
        val target = now - oneDay
        val entity = dao.getSnapshotNearTimestamp(lat, lon, target, target - window, target + window)
        return entity?.toSnapshot()
    }
}
