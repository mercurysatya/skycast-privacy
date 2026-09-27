package com.vayu.weather.data.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.vayu.weather.domain.location.LocationTracker
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

class DefaultLocationTracker @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application
) : LocationTracker {

    private val locationManager =
        application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun getCurrentLocation(): Location? {
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAccessCoarseLocationPermission && !hasAccessFineLocationPermission) {
            return null
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled) {
            return cachedLocation()
        }

        // Fast path — a recent cached fix avoids a multi-second GPS wait on every
        // app open. Stale entries (> 15 min) are ignored so we still refresh.
        (cachedLocation() ?: fusedLastLocation())?.takeIf { isFresh(it) }?.let { return it }

        val fresh = withTimeoutOrNull(TimeUnit.SECONDS.toMillis(10)) {
            requestFusedUpdates()
        }
        fresh?.let { return it }

        // Fused provider can stay silent on devices without a working GMS fix —
        // fall back to the platform LocationManager before giving up.
        withTimeoutOrNull(TimeUnit.SECONDS.toMillis(5)) {
            requestPlatformUpdate()
        }?.let { return it }

        // Last resort: any cached fix beats an error screen.
        return cachedLocation() ?: fusedLastLocation()
    }

    private suspend fun requestFusedUpdates(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, TimeUnit.SECONDS.toMillis(5))
                    .setMaxUpdates(1)
                    .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    locationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (continuation.isActive && location != null) {
                        continuation.resume(location)
                    }
                }
            }

            locationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { continuation.safeResume(null) }

            continuation.invokeOnCancellation {
                locationClient.removeLocationUpdates(callback)
            }
        }
    }

    private suspend fun requestPlatformUpdate(): Location? {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { locationManager.removeUpdates(this) }
                    continuation.safeResume(location)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }

            try {
                if (ContextCompat.checkSelfPermission(
                        application, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        application, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } else {
                    continuation.safeResume(null)
                    return@suspendCancellableCoroutine
                }
            } catch (e: SecurityException) {
                continuation.safeResume(null)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                runCatching { locationManager.removeUpdates(listener) }
            }
        }
    }

    private fun cachedLocation(): Location? {
        var best: Location? = null
        for (provider in locationManager.allProviders) {
            val candidate = runCatching {
                if (ContextCompat.checkSelfPermission(
                        application, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        application, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.getLastKnownLocation(provider)
                } else null
            }.getOrNull() ?: continue
            if (best == null || candidate.time > best.time) best = candidate
        }
        return best
    }

    private suspend fun fusedLastLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            locationClient.lastLocation
                .addOnSuccessListener { location -> continuation.safeResume(location) }
                .addOnFailureListener { continuation.safeResume(null) }
        }

    private fun isFresh(location: Location): Boolean =
        System.currentTimeMillis() - location.time <= TimeUnit.MINUTES.toMillis(15)

    private fun <T> CancellableContinuation<T>.safeResume(value: T) {
        if (isActive) resume(value)
    }
}
