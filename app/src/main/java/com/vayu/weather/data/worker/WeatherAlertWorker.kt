package com.vayu.weather.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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

@HiltWorker
class WeatherAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val locationTracker: LocationTracker,
    private val settingsManager: SettingsManager,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!settingsManager.getNotificationsEnabled()) return Result.success()

        val location = locationTracker.getCurrentLocation() ?: return Result.retry()

        val result = getWeatherUseCase(location.latitude, location.longitude)
        val weatherInfo = result.getOrNull() ?: return Result.retry()

        val nextRainProb = weatherInfo.daily.firstOrNull()?.precipitationProbability ?: 0
        if (nextRainProb >= settingsManager.getRainAlertThreshold()) {
            val title = applicationContext.getString(R.string.rain_alert_title)
            val message = applicationContext.getString(R.string.rain_alert_message, nextRainProb)

            // Store alert in database
            weatherRepository.addWeatherAlert(
                WeatherAlert(
                    title = title,
                    message = message,
                    severity = if (nextRainProb >= 80) "high" else "medium",
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )

            showNotification(title, message)
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_vayu_icon_fg)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
