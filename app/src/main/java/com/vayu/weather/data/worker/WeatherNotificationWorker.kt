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
    private val openMeteoApi: OpenMeteoApi,
    private val settingsManager: com.vayu.weather.data.local.SettingsManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_CURRENT

            // Check notifications enabled
            if (!settingsManager.getNotificationsEnabled()) return Result.success()

            // Check quiet hours
            if (isQuietHours()) return Result.success()

            // Get user's location — prefer favorites, then last known, never hard-code
            val favorites = weatherDao.getFavoriteCities().first()
            val lat = favorites.firstOrNull()?.latitude ?: settingsManager.getLastLat() ?: return Result.success()
            val lon = favorites.firstOrNull()?.longitude ?: settingsManager.getLastLon() ?: return Result.success()
            val cityName = favorites.firstOrNull()?.name ?: settingsManager.getLastCity() ?: "Your Location"

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

    private suspend fun sendCurrentWeatherNotification(
        manager: NotificationManager,
        cityName: String,
        weather: com.vayu.weather.data.remote.dto.WeatherDto
    ) {
        val tempCelsius = weather.current?.temperature ?: return
        val isCelsius = settingsManager.getTemperatureUnit() == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS
        val temp = if (isCelsius) tempCelsius.roundToInt() else ((tempCelsius * 9 / 5) + 32).roundToInt()
        val tempUnit = if (isCelsius) "°C" else "°F"
        val code = weather.current?.weatherCode ?: 0
        val condition = getConditionText(code)
        val humidity = weather.current?.humidity?.roundToInt()
        val windKmh = weather.current?.windSpeed
        val windUnit = settingsManager.getWindUnit()
        val windDisplay = windKmh?.let {
            when (windUnit) {
                com.vayu.weather.presentation.weather.WindUnit.KPH -> "${it.roundToInt()} km/h"
                com.vayu.weather.presentation.weather.WindUnit.MPH -> "${(it * 0.621371).toInt()} mph"
                com.vayu.weather.presentation.weather.WindUnit.MS -> "${(it / 3.6).toInt()} m/s"
                com.vayu.weather.presentation.weather.WindUnit.KNOTS -> "${(it * 0.539957).toInt()} kn"
            }
        }

        val title = "$temp$tempUnit — $condition"
        val text = buildString {
            append("$cityName • $condition")
            humidity?.let { append(" • ${it}% humidity") }
            windDisplay?.let { append(" • $it wind") }
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

    private suspend fun sendMorningForecastNotification(
        manager: NotificationManager,
        cityName: String,
        weather: com.vayu.weather.data.remote.dto.WeatherDto
    ) {
        val tempCelsius = weather.current?.temperature ?: return
        val isCelsius = settingsManager.getTemperatureUnit() == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS
        val currentTemp = if (isCelsius) tempCelsius.roundToInt() else ((tempCelsius * 9 / 5) + 32).roundToInt()
        val tempUnit = if (isCelsius) "°C" else "°F"
        val code = weather.current?.weatherCode ?: 0
        val condition = getConditionText(code)

        val daily = weather.daily
        val highC = daily?.maxTemperatures?.firstOrNull()
        val lowC = daily?.minTemperatures?.firstOrNull()
        val high = highC?.let { if (isCelsius) it.roundToInt() else ((it * 9 / 5) + 32).roundToInt() }
        val low = lowC?.let { if (isCelsius) it.roundToInt() else ((it * 9 / 5) + 32).roundToInt() }
        val precip = daily?.precipitationProbabilities?.firstOrNull()

        val greeting = when {
            LocalTime.now().hour < 12 -> "Good morning"
            LocalTime.now().hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val title = "$greeting. $condition and ${currentTemp}$tempUnit"
        val text = buildString {
            append("Today in $cityName: ")
            if (high != null && low != null) append("High ${high}$tempUnit, Low ${low}$tempUnit. ")
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

    private suspend fun isQuietHours(): Boolean {
        if (!settingsManager.getQuietHoursEnabled()) return false
        val now = java.time.LocalTime.now()
        val startHour = settingsManager.getQuietHoursStartHour()
        val startMin = settingsManager.getQuietHoursStartMinute()
        val endHour = settingsManager.getQuietHoursEndHour()
        val endMin = settingsManager.getQuietHoursEndMinute()
        val start = java.time.LocalTime.of(startHour, startMin)
        val end = java.time.LocalTime.of(endHour, endMin)
        return if (start.isBefore(end) || start == end) {
            !now.isBefore(start) && !now.isAfter(end)
        } else {
            // Overnight quiet hours (e.g. 22:00 → 07:00)
            !now.isBefore(start) || !now.isAfter(end)
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
