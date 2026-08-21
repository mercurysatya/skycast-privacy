package com.vayu.weather.data.repository

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi,
    private val airQualityApi: OpenMeteoAirQualityApi,
    private val geocodingApi: GeocodingApi,
    private val dao: WeatherDao,
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
    }

    override suspend fun getWeatherData(lat: Double, long: Double): Result<WeatherInfo> {
        val locationId = "$lat,$long"

        // Check cache first
        val cached = dao.getWeatherCache(locationId)
        val cacheAge = System.currentTimeMillis() - (cached?.lastUpdated ?: 0)
        if (cached != null && cacheAge < CACHE_TTL_MS) {
            return try {
                Result.success(json.decodeFromString<WeatherInfo>(cached.weatherDataJson))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
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
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("No internet connection"))
            }
            val response = airQualityApi.getAirQuality(lat, long)
            val airQuality = response.current?.toAirQuality()
                ?: return Result.failure(Exception("No air quality data available"))
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
        dao.insertFavoriteCity(
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

    override suspend fun isFavoriteCity(cityId: Long): Boolean {
        return dao.getFavoriteCities().map { list -> list.any { it.id == cityId } }.first()
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
    }

    override suspend fun deleteRecentSearch(id: Long) {
        dao.deleteRecentSearch(id)
    }

    override suspend fun clearRecentSearches() {
        dao.clearRecentSearches()
    }

    override fun getWeatherAlerts(limit: Int): Flow<List<WeatherAlert>> {
        return dao.getWeatherAlerts(limit).map { entities ->
            entities.map { entity ->
                WeatherAlert(
                    id = entity.id,
                    title = entity.title,
                    message = entity.message,
                    severity = entity.severity,
                    timestamp = entity.timestamp,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    cityName = entity.cityName
                )
            }
        }
    }

    override suspend fun addWeatherAlert(alert: WeatherAlert) {
        dao.insertWeatherAlert(
            WeatherAlertEntity(
                id = alert.id,
                title = alert.title,
                message = alert.message,
                severity = alert.severity,
                timestamp = alert.timestamp,
                latitude = alert.latitude,
                longitude = alert.longitude,
                cityName = alert.cityName
            )
        )
        // Keep only the most recent 100 alerts
        val count = dao.getWeatherAlerts(999).first().size
        if (count > 100) {
            val oldest = dao.getWeatherAlerts(999).first().takeLast(count - 100)
            oldest.forEach { dao.deleteWeatherAlert(it.id) }
        }
    }

    override suspend fun deleteWeatherAlert(id: Long) {
        dao.deleteWeatherAlert(id)
    }

    override suspend fun clearWeatherAlerts() {
        dao.clearWeatherAlerts()
    }

    override suspend fun clearWeatherCache() {
        dao.clearWeatherCache()
    }

    override suspend fun deleteStaleWeatherCache(maxAgeMs: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        dao.deleteStaleCache(cutoff)
    }

    override suspend fun deleteAllLocalData() {
        dao.clearWeatherCache()
        dao.clearRecentSearches()
        dao.clearWeatherAlerts()
        dao.clearWeatherHistory()
        dao.getFavoriteCities().first().forEach { dao.deleteFavoriteCity(it) }
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
}
