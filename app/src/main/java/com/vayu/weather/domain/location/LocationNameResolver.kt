package com.vayu.weather.domain.location

/**
 * A human readable place for a coordinate pair.
 *
 * @param city the locality/city name shown as the location title
 * @param region the state/region name, when the platform could provide one
 */
data class ResolvedPlaceName(
    val city: String,
    val region: String? = null
)

/**
 * Resolves the name of the place a coordinate pair points at.
 *
 * Implementations must do their work off the main thread and return `null`
 * when no meaningful name can be determined — callers then keep the
 * localized "current location" fallback.
 */
interface LocationNameResolver {
    suspend fun resolveName(latitude: Double, longitude: Double): ResolvedPlaceName?
}
