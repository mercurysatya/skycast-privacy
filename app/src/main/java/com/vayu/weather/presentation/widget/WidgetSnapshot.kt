package com.vayu.weather.presentation.widget

import com.vayu.weather.domain.model.WeatherInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Snapshot of the data the widget needs to render.
 *
 * This is intentionally a *flat* representation of the data already in
 * [WeatherInfo]: we don't fabricate, we don't enrich. If a field is null
 * in the source, it stays null here.
 *
 * @param info The latest [WeatherInfo] (may be null if no data has been
 *   loaded yet for the current location).
 * @param cityName The user-visible city name; falls back to "Current
 *   location" if missing.
 * @param region Optional region/state label.
 * @param isCelsius User-selected temperature unit.
 * @param windUnitLabel User-selected wind unit label (km/h, mph, m/s, kn).
 * @param lastUpdatedMillis When the snapshot was produced (epoch millis).
 */
@Serializable
data class WidgetSnapshot(
    val info: WeatherInfo? = null,
    val cityName: String = "",
    val region: String? = null,
    val isCelsius: Boolean = true,
    val windUnitLabel: String = "km/h",
    val lastUpdatedMillis: Long = 0L
) {
    /** True if the snapshot has actual weather data to render. */
    val hasData: Boolean get() = info != null

    /** True if the last update is more than 30 minutes old. */
    val isStale: Boolean
        get() = lastUpdatedMillis == 0L ||
            System.currentTimeMillis() - lastUpdatedMillis > STALE_THRESHOLD_MS

    /** True if the last update is more than 6 hours old (or missing). */
    val isOffline: Boolean
        get() = lastUpdatedMillis == 0L ||
            System.currentTimeMillis() - lastUpdatedMillis > OFFLINE_THRESHOLD_MS

    /** A human-readable freshness label for the widget footer. */
    fun freshnessLabel(): String {
        if (lastUpdatedMillis == 0L) return "No data"
        val mins = ((System.currentTimeMillis() - lastUpdatedMillis) / 60_000L).coerceAtLeast(0)
        return when {
            isOffline -> "Updated ${humanAge(mins)} ago · Offline"
            isStale -> "Updated ${humanAge(mins)} ago"
            mins < 1 -> "Just now"
            else -> "Updated ${humanAge(mins)} ago"
        }
    }

    private fun humanAge(mins: Long): String = when {
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }

    companion object {
        private const val STALE_THRESHOLD_MS = 30L * 60_000L
        private const val OFFLINE_THRESHOLD_MS = 6L * 60_000L

        val EMPTY = WidgetSnapshot()

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(snapshot: WidgetSnapshot): String =
            json.encodeToString(serializer(), snapshot)

        fun decode(payload: String?): WidgetSnapshot {
            if (payload.isNullOrBlank()) return EMPTY
            return try {
                json.decodeFromString(serializer(), payload)
            } catch (e: Exception) {
                EMPTY
            }
        }

        fun lastUpdatedAsLocalDateTime(millis: Long): LocalDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }
}
