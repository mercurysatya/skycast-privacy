package com.vayu.weather.domain.use_case

import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.model.WeatherAdvisoryFingerprint
import com.vayu.weather.domain.model.WeatherAdvisoryTextProvider
import com.vayu.weather.domain.model.WeatherAdvisoryType
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherAlertUpsertResult
import com.vayu.weather.domain.repository.WeatherRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * What happened to one advisory during an evaluation.
 */
sealed class AdvisoryChange {
    abstract val type: WeatherAdvisoryType

    /** The advisory did not exist before and was created. */
    data class Created(
        override val type: WeatherAdvisoryType,
        val alert: WeatherAlert
    ) : AdvisoryChange()

    /** The advisory existed and its content changed. */
    data class Updated(
        override val type: WeatherAdvisoryType,
        val alert: WeatherAlert,
        val escalated: Boolean
    ) : AdvisoryChange()

    /** The advisory existed and is unchanged — nothing was written. */
    data class Unchanged(
        override val type: WeatherAdvisoryType,
        val alert: WeatherAlert
    ) : AdvisoryChange()

    /** The condition is over — the advisory was closed (kept for history). */
    data class Resolved(
        override val type: WeatherAdvisoryType,
        val fingerprint: String
    ) : AdvisoryChange()
}

/**
 * Evaluates the weather advisories for a location and keeps the stored
 * advisories in sync with the forecast.
 *
 * The app receives no official alert objects — advisories are derived from the
 * forecast (rain probability, wind, UV, heat, cold) for the current location.
 * The intended product behaviour is therefore:
 *
 *  * there is **one current advisory per type and location** (a rain advisory
 *    is not repeated every time the forecast is refreshed),
 *  * a changed forecast **updates** that advisory instead of adding another
 *    card,
 *  * a condition that no longer holds **closes** the advisory,
 *  * a different forecast day or a different type is a **different** advisory.
 *
 * Identity is provided by [WeatherAdvisoryFingerprint]; every write is an
 * idempotent upsert, so repeated evaluation (worker cycles, app starts,
 * refreshes) can never create a second row for the same advisory.
 */
class EvaluateWeatherAdvisoriesUseCase @Inject constructor(
    private val repository: WeatherRepository,
    private val settingsManager: SettingsManager,
    private val texts: WeatherAdvisoryTextProvider
) {

    suspend operator fun invoke(
        weatherInfo: WeatherInfo,
        latitude: Double,
        longitude: Double,
        cityName: String? = null,
        now: Long = System.currentTimeMillis()
    ): List<AdvisoryChange> {
        val daily = weatherInfo.daily.firstOrNull() ?: return emptyList()
        val forecastDate = daily.date
        val expiresAt = endOfForecastDay(forecastDate, weatherInfo.timezone)
        val locationPrefix = WeatherAdvisoryFingerprint.locationKey(latitude, longitude) + "|"

        val severityFilter = settingsManager.getSeverityFilter()
        val changes = mutableListOf<AdvisoryChange>()

        // ===== RAIN =====
        val rainProbability = daily.precipitationProbability
        if (rainProbability != null && rainProbability >= settingsManager.getRainAlertThreshold()) {
            val severity = if (rainProbability >= 80) "high" else "medium"
            if (passesFilter(severity, severityFilter)) {
                changes += evaluate(
                    type = WeatherAdvisoryType.RAIN,
                    title = texts.titleFor(WeatherAdvisoryType.RAIN),
                    message = texts.messageFor(WeatherAdvisoryType.RAIN, rainProbability),
                    severity = severity,
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName,
                    forecastDate = forecastDate,
                    expiresAt = expiresAt,
                    now = now
                )
            }
        } else {
            resolve(WeatherAdvisoryType.RAIN, locationPrefix, forecastDate, now)?.let { changes += it }
        }

        // ===== WIND =====
        if (settingsManager.getEnableWindAlerts()) {
            val windSpeed = weatherInfo.current.windSpeed
            val windThreshold = settingsManager.getWindAlertThreshold()
            if (windSpeed != null && windSpeed >= windThreshold) {
                val severity = if (windSpeed >= windThreshold * 1.5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    val windKph = Math.round(windSpeed).toInt()
                    changes += evaluate(
                        type = WeatherAdvisoryType.WIND,
                        title = texts.titleFor(WeatherAdvisoryType.WIND),
                        message = texts.messageFor(WeatherAdvisoryType.WIND, windKph),
                        severity = severity,
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName,
                        forecastDate = forecastDate,
                        expiresAt = expiresAt,
                        now = now
                    )
                }
            } else {
                resolve(WeatherAdvisoryType.WIND, locationPrefix, forecastDate, now)?.let { changes += it }
            }
        }

        // ===== UV =====
        if (settingsManager.getEnableUvAlerts()) {
            val uvIndex = daily.uvIndex?.toInt()
            val uvThreshold = settingsManager.getUvAlertThreshold()
            if (uvIndex != null && uvIndex >= uvThreshold) {
                val severity = if (uvIndex >= 11) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    changes += evaluate(
                        type = WeatherAdvisoryType.UV,
                        title = texts.titleFor(WeatherAdvisoryType.UV),
                        message = texts.messageFor(WeatherAdvisoryType.UV, uvIndex),
                        severity = severity,
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName,
                        forecastDate = forecastDate,
                        expiresAt = expiresAt,
                        now = now
                    )
                }
            } else {
                resolve(WeatherAdvisoryType.UV, locationPrefix, forecastDate, now)?.let { changes += it }
            }
        }

        // ===== HEAT =====
        if (settingsManager.getEnableHeatAlerts()) {
            val maxTemp = daily.maxTemp.toInt()
            val heatThreshold = settingsManager.getHeatAlertThreshold()
            if (maxTemp >= heatThreshold) {
                val severity = if (maxTemp >= heatThreshold + 5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    changes += evaluate(
                        type = WeatherAdvisoryType.HEAT,
                        title = texts.titleFor(WeatherAdvisoryType.HEAT),
                        message = texts.messageFor(WeatherAdvisoryType.HEAT, maxTemp),
                        severity = severity,
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName,
                        forecastDate = forecastDate,
                        expiresAt = expiresAt,
                        now = now
                    )
                }
            } else {
                resolve(WeatherAdvisoryType.HEAT, locationPrefix, forecastDate, now)?.let { changes += it }
            }
        }

        // ===== COLD =====
        if (settingsManager.getEnableColdAlerts()) {
            val minTemp = daily.minTemp.toInt()
            val coldThreshold = settingsManager.getColdAlertThreshold()
            if (minTemp <= coldThreshold) {
                val severity = if (minTemp <= coldThreshold - 5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    changes += evaluate(
                        type = WeatherAdvisoryType.COLD,
                        title = texts.titleFor(WeatherAdvisoryType.COLD),
                        message = texts.messageFor(WeatherAdvisoryType.COLD, minTemp),
                        severity = severity,
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName,
                        forecastDate = forecastDate,
                        expiresAt = expiresAt,
                        now = now
                    )
                }
            } else {
                resolve(WeatherAdvisoryType.COLD, locationPrefix, forecastDate, now)?.let { changes += it }
            }
        }

        return changes
    }

    private suspend fun evaluate(
        type: WeatherAdvisoryType,
        title: String,
        message: String,
        severity: String,
        latitude: Double,
        longitude: Double,
        cityName: String?,
        forecastDate: String,
        expiresAt: Long,
        now: Long
    ): AdvisoryChange {
        val fingerprint = WeatherAdvisoryFingerprint.of(type, latitude, longitude, forecastDate)
        val alert = WeatherAlert(
            title = title,
            message = message,
            severity = severity,
            timestamp = now,
            latitude = latitude,
            longitude = longitude,
            cityName = cityName,
            fingerprint = fingerprint,
            expiresAt = expiresAt
        )
        return when (val result = repository.upsertWeatherAlert(alert)) {
            is WeatherAlertUpsertResult.Created -> AdvisoryChange.Created(type, alert.copy(id = result.id))
            is WeatherAlertUpsertResult.Updated -> AdvisoryChange.Updated(type, alert.copy(id = result.id), result.escalated)
            is WeatherAlertUpsertResult.Unchanged -> AdvisoryChange.Unchanged(type, alert.copy(id = result.id))
        }
    }

    private suspend fun resolve(
        type: WeatherAdvisoryType,
        locationPrefix: String,
        forecastDate: String,
        now: Long
    ): AdvisoryChange? {
        // Same layout as WeatherAdvisoryFingerprint.of: location | type | day.
        val fingerprint = locationPrefix + type.slug + "|" + forecastDate
        val resolved = repository.resolveWeatherAlert(fingerprint, now)
        return if (resolved) AdvisoryChange.Resolved(type, fingerprint) else null
    }

    /**
     * End of the forecast day in the location's own time zone — the advisory
     * is about *that* day, so it must not outlive it.
     */
    internal fun endOfForecastDay(forecastDate: String, timezone: String?): Long {
        val zone = timezone?.let {
            runCatching { ZoneId.of(it) }.getOrNull()
        } ?: ZoneId.systemDefault()
        val date = runCatching { LocalDate.parse(forecastDate) }.getOrNull()
            ?: return Long.MAX_VALUE
        return date.atTime(LocalTime.of(23, 59, 59)).atZone(zone).toInstant().toEpochMilli()
    }

    private fun passesFilter(severity: String, filter: String): Boolean {
        return when (filter) {
            "HIGH" -> severity == "high"
            "HIGH_MEDIUM" -> severity == "high" || severity == "medium"
            else -> true
        }
    }
}
