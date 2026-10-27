package com.vayu.weather.domain.model

/**
 * The kind of weather advisory an alert row represents.
 *
 * The app does not receive official alert objects from the provider — the
 * advisories are derived from the forecast for the current location (rain
 * probability, wind, UV, heat, cold). The type is therefore part of the
 * advisory's identity: a rain advisory and a wind advisory for the same place
 * are two different pieces of information and must never be merged.
 */
enum class WeatherAdvisoryType(val slug: String) {
    RAIN("rain"),
    WIND("wind"),
    UV("uv"),
    HEAT("heat"),
    COLD("cold");

    companion object {
        fun fromSlug(slug: String): WeatherAdvisoryType? =
            entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) }
    }
}

/**
 * Stable identity for a weather advisory.
 *
 * An advisory is identified by
 *  * its type (rain / wind / uv / heat / cold),
 *  * the forecast location, bucketed to a grid, and
 *  * the forecast day it was issued for.
 *
 * The location grid is deliberately coarse (0.1° ≈ 11 km): the provider's
 * forecast grid has the same resolution, and a phone's GPS fix jitters by
 * hundreds of metres between samples. Without the bucketing, every slightly
 * different fix would look like a new place and produce a new row.
 *
 * The forecast day is part of the identity so that two distinct forecast
 * periods for the same location stay two distinct advisories, while a refresh
 * of the *same* period always maps onto the same row.
 */
object WeatherAdvisoryFingerprint {

    /** Grid size in degrees (~11 km) — matches the forecast provider's resolution. */
    const val LOCATION_GRID_DEGREES = 0.1

    fun of(
        type: WeatherAdvisoryType,
        latitude: Double,
        longitude: Double,
        forecastDate: String
    ): String {
        val latBucket = bucket(latitude)
        val lonBucket = bucket(longitude)
        // Location first, so every advisory of one place shares a prefix and
        // can be found with a single query.
        return "$latBucket|$lonBucket|${type.slug}|$forecastDate"
    }

    /**
     * The location part of a fingerprint, i.e. everything before the advisory
     * type. Used to find the advisories that belong to one place, regardless of
     * their type or forecast day.
     */
    fun locationKey(latitude: Double, longitude: Double): String =
        "${bucket(latitude)}|${bucket(longitude)}"

    private fun bucket(value: Double): String {
        val bucketed = Math.round(value / LOCATION_GRID_DEGREES) * LOCATION_GRID_DEGREES
        // "%.1f" keeps the key stable across platforms (no "-0.0").
        return String.format(java.util.Locale.US, "%.1f", bucketed)
    }
}

/** The advisory types an alert row can belong to, derived from its title. */
fun advisoryTypeOf(title: String): WeatherAdvisoryType? = when {
    title.contains("Rain", ignoreCase = true) -> WeatherAdvisoryType.RAIN
    title.contains("Wind", ignoreCase = true) -> WeatherAdvisoryType.WIND
    title.contains("UV", ignoreCase = true) -> WeatherAdvisoryType.UV
    title.contains("Heat", ignoreCase = true) -> WeatherAdvisoryType.HEAT
    title.contains("Cold", ignoreCase = true) -> WeatherAdvisoryType.COLD
    else -> null
}
