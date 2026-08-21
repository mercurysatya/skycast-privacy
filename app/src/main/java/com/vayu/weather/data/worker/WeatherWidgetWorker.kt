package com.vayu.weather.data.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import com.vayu.weather.presentation.widget.WeatherWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeatherWidgetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val locationTracker: LocationTracker,
    private val settingsManager: SettingsManager
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

        return getWeatherUseCase(location.latitude, location.longitude).fold(
            onSuccess = { weatherInfo ->
                Log.d(TAG, "Weather data received, updating preferences")
                val prefs = applicationContext.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putFloat("temperature", weatherInfo.current.temperature.toFloat())
                editor.putInt("weather_code", weatherInfo.current.weatherCode)
                editor.putBoolean("is_day", weatherInfo.current.isDay)
                editor.putFloat("wind_speed", weatherInfo.current.windSpeed?.toFloat() ?: 0f)
                editor.putFloat("humidity", weatherInfo.current.humidity?.toFloat() ?: 0f)
                editor.putBoolean("is_fahrenheit", settingsManager.getTemperatureUnit() == com.vayu.weather.presentation.weather.TemperatureUnit.FAHRENHEIT)
                editor.putString("wind_unit", settingsManager.getWindUnit().name)
                editor.putLong("last_updated", System.currentTimeMillis())
                editor.putBoolean("has_data", true)

                // Store 3-day forecast
                val daily = weatherInfo.daily.take(3)
                Log.d(TAG, "Storing ${daily.size} forecast days")
                daily.forEachIndexed { index, day ->
                    editor.putString("day_${index}_date", day.date)
                    editor.putFloat("day_${index}_min_temp", day.minTemp.toFloat())
                    editor.putFloat("day_${index}_max_temp", day.maxTemp.toFloat())
                    editor.putInt("day_${index}_weather_code", day.weatherCode)
                    editor.putInt("day_${index}_precipitation", day.precipitationProbability ?: 0)
                    Log.d(TAG, "Day $index: ${day.date} ${day.minTemp}/${day.maxTemp} code=${day.weatherCode}")
                }
                editor.putInt("forecast_days", daily.size)

                editor.commit()

                // Force widget to refresh by calling update on all widget instances
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

                Result.success()
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to fetch weather", e)
                Result.retry()
            }
        )
    }

    companion object {
        private const val TAG = "WeatherWidgetWorker"
        const val WORK_NAME = "WeatherWidgetUpdate"
    }
}
