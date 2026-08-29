package com.vayu.weather.data.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WindUnit
import com.vayu.weather.presentation.widget.WidgetSnapshot
import com.vayu.weather.presentation.widget.WeatherWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Fetches the latest weather for the user's current location and writes a
 * single [WidgetSnapshot] blob to SharedPreferences for the Glance widget
 * to render.
 *
 * Uses the existing [GetWeatherUseCase] so the widget reuses the same
 * cache + repository pipeline as the in-app dashboard.
 */
@HiltWorker
class WeatherWidgetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val locationTracker: LocationTracker,
    private val settingsManager: com.vayu.weather.data.local.SettingsManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Widget worker starting (attempt $runAttemptCount)")

        if (runAttemptCount >= 3) {
            Log.w(TAG, "Max retries reached, giving up")
            return Result.failure()
        }

        val location = locationTracker.getCurrentLocation()
        if (location == null) {
            Log.w(TAG, "Location unavailable, retrying (attempt $runAttemptCount)")
            return Result.retry()
        }

        val cityName = settingsManager.getLastCity()
        val isCelsius = settingsManager.getTemperatureUnit() == TemperatureUnit.CELSIUS
        val windUnit = settingsManager.getWindUnit()

        return getWeatherUseCase(location.latitude, location.longitude).fold(
            onSuccess = { weatherInfo ->
                Log.d(TAG, "Weather data received, writing widget snapshot")
                writeSnapshot(weatherInfo, cityName, isCelsius, windUnit)
                Result.success()
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to fetch weather", e)
                // Even on failure, keep the existing snapshot in place so the
                // widget can still show the last-known state.
                Result.retry()
            }
        )
    }

    private suspend fun writeSnapshot(
        info: WeatherInfo,
        cityName: String?,
        isCelsius: Boolean,
        windUnit: WindUnit
    ) {
        val snapshot = WidgetSnapshot(
            info = info,
            cityName = cityName?.takeIf { it.isNotBlank() } ?: "",
            region = null,
            isCelsius = isCelsius,
            windUnitLabel = windUnitLabel(windUnit),
            lastUpdatedMillis = System.currentTimeMillis()
        )
        val prefs = applicationContext.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SNAPSHOT, WidgetSnapshot.encode(snapshot))
            .putBoolean(KEY_HAS_DATA, true)
            .putLong(KEY_LAST_UPDATED, snapshot.lastUpdatedMillis)
            .apply()

        try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(WeatherWidget::class.java)
            Log.d(TAG, "Found ${glanceIds.size} widget instances, updating")
            glanceIds.forEach { glanceId ->
                WeatherWidget().update(applicationContext, glanceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget", e)
        }
    }

    private fun windUnitLabel(unit: WindUnit): String = when (unit) {
        WindUnit.KPH -> "km/h"
        WindUnit.MPH -> "mph"
        WindUnit.MS -> "m/s"
        WindUnit.KNOTS -> "kn"
    }

    companion object {
        private const val TAG = "WeatherWidgetWorker"
        const val WORK_NAME = "WeatherWidgetUpdate"
        const val KEY_SNAPSHOT = "snapshot"
        const val KEY_HAS_DATA = "has_data"
        const val KEY_LAST_UPDATED = "last_updated"
    }
}
