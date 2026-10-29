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
import com.vayu.weather.data.local.DataDeletionGuard
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.location.LocationTracker
import com.vayu.weather.domain.use_case.AdvisoryChange
import com.vayu.weather.domain.use_case.EvaluateWeatherAdvisoriesUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeatherAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val evaluateWeatherAdvisories: EvaluateWeatherAdvisoriesUseCase,
    private val locationTracker: LocationTracker,
    private val settingsManager: SettingsManager,
    private val deletionGuard: DataDeletionGuard
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "WeatherAlertWork"

        /** Stable notification id derived from the advisory fingerprint. */
        fun notificationIdFor(fingerprint: String): Int = fingerprint.hashCode()
    }

    override suspend fun doWork(): Result {
        val deletionToken = deletionGuard.currentGenerationOrNull() ?: return Result.success()
        if (!settingsManager.getNotificationsEnabled()) return Result.success()

        // A fresh fused location fix is not guaranteed while the app is in the
        // background (no ACCESS_BACKGROUND_LOCATION by design), so fall back to
        // the last persisted coordinates instead of retrying forever.
        val fix = locationTracker.getCurrentLocation()
        val lat = fix?.latitude ?: settingsManager.getLastLat()
        val lon = fix?.longitude ?: settingsManager.getLastLon()
        if (lat == null || lon == null) {
            // Nothing actionable for this cycle — retrying cannot help.
            return Result.success()
        }

        val result = getWeatherUseCase(lat, lon)
        val weatherInfo = result.getOrNull()
            ?: return if (runAttemptCount >= 3) Result.failure() else Result.retry()

        // The advisory evaluation is the single source of truth for what an
        // advisory is; it upserts idempotently, so running it again for the
        // same forecast never creates a second row. It is guarded so a wipe
        // cannot be followed by a late insert or notification.
        deletionGuard.tryWrite(deletionToken) {
            val changes = evaluateWeatherAdvisories(
                weatherInfo = weatherInfo,
                latitude = lat,
                longitude = lon,
                cityName = settingsManager.getLastCity()
            )

            // Notification policy: a *new* advisory notifies, and so does an
            // escalation (severity raised). An unchanged advisory and a
            // de-escalation stay silent — that is what kept the user from being
            // notified about the same rain every three hours.
            changes.forEach { change ->
                when (change) {
                    is AdvisoryChange.Created -> showNotification(
                        change.alert.title,
                        change.alert.message,
                        notificationIdFor(change.alert.fingerprint.orEmpty())
                    )
                    is AdvisoryChange.Updated -> if (change.escalated) {
                        showNotification(
                            change.alert.title,
                            change.alert.message,
                            notificationIdFor(change.alert.fingerprint.orEmpty())
                        )
                    }
                    else -> Unit
                }
            }
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
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

        // A stable id per advisory: an updated advisory replaces its own
        // notification instead of stacking another one next to it.
        notificationManager.notify(notificationId, notification)
    }
}
