package com.vayu.weather.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.vayu.weather.MainActivity
import com.vayu.weather.R
import com.vayu.weather.data.local.WeatherDao
import com.vayu.weather.data.remote.OpenMeteoApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@HiltWorker
class WeatherNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherDao: WeatherDao,
    private val openMeteoApi: OpenMeteoApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_CURRENT

            // Get user's favorite city or default location
            val favorites = weatherDao.getFavoriteCities().first()
            val lat = favorites.firstOrNull()?.latitude ?: 16.5062 // default: Srikakulam
            val lon = favorites.firstOrNull()?.longitude ?: 80.6480
            val cityName = favorites.firstOrNull()?.name ?: "Your Location"

            val weather = openMeteoApi.getWeatherData(
                lat = lat,
                long = lon
            )

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channels
            createChannels(notificationManager)

            when (type) {
                TYPE_CURRENT -> sendCurrentWeatherNotification(notificationManager, cityName, weather)
                TYPE_MORNING -> sendMorningForecastNotification(notificationManager, cityName, weather)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendCurrentWeatherNotification(
        manager: NotificationManager,
        cityName: String,
        weather: com.vayu.weather.data.remote.dto.WeatherDto
    ) {
        val temp = weather.current?.temperature?.roundToInt() ?: return
        val code = weather.current?.weatherCode ?: 0
        val condition = getConditionText(code)
        val humidity = weather.current?.humidity?.roundToInt()
        val wind = weather.current?.windSpeed?.roundToInt()

        val title = "$temp° — $condition"
        val text = buildString {
            append("$cityName • $condition")
            humidity?.let { append(" • ${it}% humidity") }
            wind?.let { append(" • ${it} km/h wind") }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_CURRENT_WEATHER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        manager.notify(NOTIFICATION_ID_CURRENT, notification)
    }

    private fun sendMorningForecastNotification(
        manager: NotificationManager,
        cityName: String,
        weather: com.vayu.weather.data.remote.dto.WeatherDto
    ) {
        val currentTemp = weather.current?.temperature?.roundToInt() ?: return
        val code = weather.current?.weatherCode ?: 0
        val condition = getConditionText(code)

        val daily = weather.daily
        val high = daily?.maxTemperatures?.firstOrNull()?.roundToInt()
        val low = daily?.minTemperatures?.firstOrNull()?.roundToInt()
        val precip = daily?.precipitationProbabilities?.firstOrNull()

        val greeting = when {
            LocalTime.now().hour < 12 -> "Good morning"
            LocalTime.now().hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val title = "$greeting. $condition and ${currentTemp}°"
        val text = buildString {
            append("Today in $cityName: ")
            if (high != null && low != null) append("High ${high}°, Low ${low}°. ")
            append(condition)
            precip?.let { if (it > 0) append(". ${it}% chance of rain") }
            append(".")
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_MORNING_FORECAST)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        manager.notify(NOTIFICATION_ID_MORNING, notification)
    }

    private fun createChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CURRENT_WEATHER,
                    "Current Weather",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Persistent notification showing current weather"
                    setShowBadge(false)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MORNING_FORECAST,
                    "Morning Forecast",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily morning weather summary"
                }
            )
        }
    }

    private fun getConditionText(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzly"
        61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snowy"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Cloudy"
    }

    companion object {
        const val WORK_NAME_CURRENT = "WeatherNotificationCurrent"
        const val WORK_NAME_MORNING = "WeatherNotificationMorning"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val TYPE_CURRENT = "current"
        const val TYPE_MORNING = "morning"
        private const val CHANNEL_CURRENT_WEATHER = "current_weather"
        private const val CHANNEL_MORNING_FORECAST = "morning_forecast"
        private const val NOTIFICATION_ID_CURRENT = 9001
        private const val NOTIFICATION_ID_MORNING = 9002

        fun scheduleCurrentWeather(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                3, TimeUnit.HOURS
            ).setInputData(
                workDataOf(KEY_NOTIFICATION_TYPE to TYPE_CURRENT)
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_CURRENT,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleMorningForecast(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                24, TimeUnit.HOURS
            ).setInputData(
                workDataOf(KEY_NOTIFICATION_TYPE to TYPE_MORNING)
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).setInitialDelay(
                calculateDelayToMorning(),
                TimeUnit.MILLISECONDS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_MORNING,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayToMorning(): Long {
            val now = LocalTime.now()
            val morning = LocalTime.of(7, 0)
            val delayMinutes = if (now.isBefore(morning)) {
                java.time.Duration.between(now, morning).toMinutes()
            } else {
                java.time.Duration.between(now, morning).toMinutes() + 24 * 60
            }
            return delayMinutes.coerceAtLeast(1) * 60 * 1000L
        }
    }
}
