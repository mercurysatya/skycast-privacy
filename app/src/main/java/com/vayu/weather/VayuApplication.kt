package com.vayu.weather

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.vayu.weather.data.worker.WeatherAlertWorker
import com.vayu.weather.data.worker.WeatherWidgetWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VayuApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initializeAds()
        setupWeatherAlerts()
        setupWidgetUpdate()
    }

    private fun initializeAds() {
        val requestConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(
                if (BuildConfig.DEBUG) listOf(
                    RequestConfiguration.DEVICE_ID_EMULATOR
                ) else emptyList()
            )
            .build()
        MobileAds.setRequestConfiguration(requestConfig)

        MobileAds.initialize(this) { initializationStatus ->
            Log.d("VayuApplication", "AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }

    private fun setupWeatherAlerts() {
        val workRequest = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
            3, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherAlertWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun setupWidgetUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(
            1, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeatherWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
