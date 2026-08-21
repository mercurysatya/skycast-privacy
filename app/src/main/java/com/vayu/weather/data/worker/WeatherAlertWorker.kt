package com.vayu.weather.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayu.weather.R
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherRepository
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.math.roundToInt

@HiltWorker
class WeatherAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val locationTracker: LocationTracker,
    private val settingsManager: SettingsManager,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "WeatherAlertWork"
    }

    override suspend fun doWork(): Result {
        if (!settingsManager.getNotificationsEnabled()) return Result.success()

        val location = locationTracker.getCurrentLocation() ?: return Result.retry()

        val result = getWeatherUseCase(location.latitude, location.longitude)
        val weatherInfo = result.getOrNull() ?: return Result.retry()

        val severityFilter = settingsManager.getSeverityFilter()

        // ===== RAIN ALERT =====
        val nextRainProb = weatherInfo.daily.firstOrNull()?.precipitationProbability ?: 0
        if (nextRainProb >= settingsManager.getRainAlertThreshold()) {
            val severity = if (nextRainProb >= 80) "high" else "medium"
            if (passesFilter(severity, severityFilter)) {
                val title = applicationContext.getString(R.string.rain_alert_title)
                val message = applicationContext.getString(R.string.rain_alert_message, nextRainProb)
                storeAndNotify(title, message, severity, location.latitude, location.longitude)
            }
        }

        // ===== WIND ALERT =====
        if (settingsManager.getEnableWindAlerts()) {
            val windSpeed = weatherInfo.current.windSpeed ?: 0.0
            val windThreshold = settingsManager.getWindAlertThreshold()
            if (windSpeed >= windThreshold) {
                val severity = if (windSpeed >= windThreshold * 1.5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    val windKph = windSpeed.roundToInt()
                    val title = applicationContext.getString(R.string.wind_alert_title)
                    val message = applicationContext.getString(R.string.wind_alert_message, windKph)
                    storeAndNotify(title, message, severity, location.latitude, location.longitude)
                }
            }
        }

        // ===== UV ALERT =====
        if (settingsManager.getEnableUvAlerts()) {
            val uvIndex = weatherInfo.daily.firstOrNull()?.uvIndex?.toInt() ?: 0
            val uvThreshold = settingsManager.getUvAlertThreshold()
            if (uvIndex >= uvThreshold) {
                val severity = if (uvIndex >= 11) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    val title = applicationContext.getString(R.string.uv_alert_title)
                    val message = applicationContext.getString(R.string.uv_alert_message, uvIndex)
                    storeAndNotify(title, message, severity, location.latitude, location.longitude)
                }
            }
        }

        // ===== HEAT ALERT =====
        if (settingsManager.getEnableHeatAlerts()) {
            val maxTemp = weatherInfo.daily.firstOrNull()?.maxTemp?.toInt() ?: 0
            val heatThreshold = settingsManager.getHeatAlertThreshold()
            if (maxTemp >= heatThreshold) {
                val severity = if (maxTemp >= heatThreshold + 5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    val title = applicationContext.getString(R.string.heat_alert_title)
                    val message = applicationContext.getString(R.string.heat_alert_message, maxTemp)
                    storeAndNotify(title, message, severity, location.latitude, location.longitude)
                }
            }
        }

        // ===== COLD ALERT =====
        if (settingsManager.getEnableColdAlerts()) {
            val minTemp = weatherInfo.daily.firstOrNull()?.minTemp?.toInt() ?: 0
            val coldThreshold = settingsManager.getColdAlertThreshold()
            if (minTemp <= coldThreshold) {
                val severity = if (minTemp <= coldThreshold - 5) "high" else "medium"
                if (passesFilter(severity, severityFilter)) {
                    val title = applicationContext.getString(R.string.cold_alert_title)
                    val message = applicationContext.getString(R.string.cold_alert_message, minTemp)
                    storeAndNotify(title, message, severity, location.latitude, location.longitude)
                }
            }
        }

        return Result.success()
    }

    private fun passesFilter(severity: String, filter: String): Boolean {
        return when (filter) {
            "HIGH" -> severity == "high"
            "HIGH_MEDIUM" -> severity == "high" || severity == "medium"
            else -> true
        }
    }

    private suspend fun storeAndNotify(
        title: String,
        message: String,
        severity: String,
        latitude: Double,
        longitude: Double
    ) {
        weatherRepository.addWeatherAlert(
            WeatherAlert(
                title = title,
                message = message,
                severity = severity,
                latitude = latitude,
                longitude = longitude
            )
        )
        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.notification_channel_weather),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_weather)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_vayu_icon_fg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
