package com.vayu.weather.presentation.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.vayu.weather.R
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@HiltWorker
class PushNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsManager: SettingsManager,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "PushNotificationWorker"
        const val DEFAULT_FREQUENCY_HOURS = 24
    }

    override suspend fun doWork(): Result {
        // Check if push notifications are enabled
        if (!settingsManager.getEnablePushAlerts()) return Result.success()

        val frequencyHours = settingsManager.getPushAlertFrequencyHours()
        if (frequencyHours <= 0) return Result.success()

        // Fetch and send alerts
        try {
            val alerts = weatherRepository.getWeatherAlerts().first()
            if (alerts.isNotEmpty()) {
                sendPushAlerts(applicationContext, alerts)
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private suspend fun sendPushAlerts(context: Context, alerts: List<WeatherAlert>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weather_alerts",
                context.getString(R.string.notification_channel_weather),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_weather)
            }
            notificationManager.createNotificationChannel(channel)
        }

        alerts.forEach { alert ->
            val notification = NotificationCompat.Builder(context, "weather_alerts")
                .setSmallIcon(R.drawable.ic_vayu_icon_fg)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(alert.id.toInt(), notification)
        }
    }
}

// Broadcast receiver to trigger first push notification on app start
class PushNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.vayu.weather.START_PUSH") {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<PushNotificationWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}