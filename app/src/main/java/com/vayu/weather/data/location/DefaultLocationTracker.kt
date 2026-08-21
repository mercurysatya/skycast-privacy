package com.vayu.weather.data.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vayu.weather.domain.location.LocationTracker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

class DefaultLocationTracker @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application
) : LocationTracker {

    override suspend fun getCurrentLocation(): Location? {
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if ((!hasAccessCoarseLocationPermission && !hasAccessFineLocationPermission) || !isGpsEnabled) {
            return null
        }

        return withTimeoutOrNull(TimeUnit.SECONDS.toMillis(15)) {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, TimeUnit.SECONDS.toMillis(5))
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        LocationServices.getFusedLocationProviderClient(application)
                            .removeLocationUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(result.lastLocation)
                        }
                    }
                }

                LocationServices.getFusedLocationProviderClient(application)
                    .requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

                continuation.invokeOnCancellation {
                    LocationServices.getFusedLocationProviderClient(application)
                        .removeLocationUpdates(callback)
                }
            }
        }
    }
}
