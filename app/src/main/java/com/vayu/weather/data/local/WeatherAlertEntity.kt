package com.vayu.weather.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A weather advisory row.
 *
 * [fingerprint] is the stable identity of the advisory (type + bucketed
 * location + forecast day, see `WeatherAdvisoryFingerprint`). The unique index
 * on it is what makes repeated ingestion idempotent: the same logical
 * advisory can never be stored twice.
 *
 * [expiresAt] is the end of the advisory's validity. Rows with `0` have no
 * validity (rows that predate the fingerprint) and count as expired.
 */
@Entity(
    tableName = "weather_alerts",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class WeatherAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double?,
    val longitude: Double?,
    val cityName: String?,
    val fingerprint: String = "",
    val expiresAt: Long = 0L
) {
    /** The advisory is active while its validity window is open. */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = expiresAt > now
}
