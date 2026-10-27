package com.vayu.weather.data.location

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.vayu.weather.domain.location.LocationNameResolver
import com.vayu.weather.domain.location.ResolvedPlaceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Reverse-geocodes coordinates into a place name with the platform
 * [Geocoder] — no extra dependency, the device's own geocoder backend does
 * the lookup.
 *
 * The lookup is blocking on the platform service, so it always runs on
 * [Dispatchers.IO] and is bounded by a timeout. Results are cached per
 * coordinate (≈100 m grid) so repeatedly refreshing the dashboard does not
 * trigger a new round trip for the same spot.
 */
class DefaultLocationNameResolver @Inject constructor(
    private val application: Application
) : LocationNameResolver {

    private val cacheLock = Any()
    private var cachedKey: String? = null
    private var cachedName: ResolvedPlaceName? = null

    override suspend fun resolveName(latitude: Double, longitude: Double): ResolvedPlaceName? {
        val key = cacheKey(latitude, longitude)
        synchronized(cacheLock) {
            if (key == cachedKey) return cachedName
        }

        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) { reverseGeocode(latitude, longitude) }
        }

        if (resolved == null) {
            Log.w(TAG, "No place name for $latitude,$longitude")
            return null
        }

        synchronized(cacheLock) {
            cachedKey = key
            cachedName = resolved
        }
        return resolved
    }

    @SuppressLint("MissingPermission")
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): ResolvedPlaceName? {
        return try {
            val geocoder = Geocoder(application, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder backend unavailable")
                return null
            }
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeAsync(geocoder, latitude, longitude)
            } else {
                geocodeBlocking(geocoder, latitude, longitude)
            }
            toResolvedName(addresses)
        } catch (e: Exception) {
            // A missing backend, a network hiccup or a malformed address must
            // never take the dashboard down — the caller falls back gracefully.
            Log.w(TAG, "Reverse geocoding failed for $latitude,$longitude: ${e.message}")
            null
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): List<Address>? = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (continuation.isActive) continuation.resume(addresses)
            }

            override fun onError(errorMessage: String?) {
                Log.w(TAG, "Geocoder error: $errorMessage")
                if (continuation.isActive) continuation.resume(null)
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun geocodeBlocking(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): List<Address>? = runCatching {
        geocoder.getFromLocation(latitude, longitude, 1)
    }.getOrNull()

    /**
     * Prefers the city/locality, then the county, then the state, and only as
     * a last resort the feature name — a street-level name would be worse
     * than none.
     */
    private fun toResolvedName(addresses: List<Address>?): ResolvedPlaceName? {
        val address = addresses?.firstOrNull() ?: return null
        val city = firstNonBlank(
            address.locality,
            address.subAdminArea,
            address.adminArea,
            address.featureName
        ) ?: return null
        val region = firstNonBlank(
            address.adminArea,
            address.subAdminArea,
            address.countryName
        )?.takeIf { !it.equals(city, ignoreCase = true) }
        return ResolvedPlaceName(city = city, region = region)
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    /** ≈100 m grid — small GPS jitter must not trigger a new lookup. */
    private fun cacheKey(latitude: Double, longitude: Double): String =
        "${"%.3f".format(latitude)}:${"%.3f".format(longitude)}"

    private companion object {
        const val TAG = "LocationNameResolver"
        const val RESOLVE_TIMEOUT_MS = 5_000L
    }
}
