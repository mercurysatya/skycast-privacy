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
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class VayuApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    override fun onCreate() {
        super.onCreate()

        // Configure ads (initialization is deferred until UMP consent allows it)
        configureAds()

        // Initialize Firebase (non-blocking, graceful failure)
        initializeFirebase()

        setupWeatherAlerts()
        setupWidgetUpdate()
    }

    private fun configureAds() {
        val requestConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(
                if (BuildConfig.DEBUG) listOf(
                    "EMULATOR"
                ) else emptyList()
            )
            .build()
        MobileAds.setRequestConfiguration(requestConfig)
    }

    private fun initializeFirebase() {
        try {
            // Initialize Firebase App (must be first)
            FirebaseApp.initializeApp(this)
            Log.d("VayuApplication", "Firebase App initialized")

            // Initialize Firebase Analytics
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            Log.d("VayuApplication", "Firebase Analytics initialized")

            // Initialize Firebase Remote Config
            firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            Log.d("VayuApplication", "Firebase Remote Config initialized")

            // Crashlytics and App Check are initialized automatically
            // by their respective SDKs on first use. They do not need
            // synchronous initialization here.

            Log.d("VayuApplication", "Firebase services initialized successfully")
        } catch (e: Exception) {
            Log.e("VayuApplication", "Firebase initialization failed, continuing without Firebase", e)
            // Firebase failure does not block app startup - weather functionality continues
        }
    }

    private fun setupWeatherAlerts() {
        val workRequest = PeriodicWorkRequestBuilder<com.vayu.weather.data.worker.WeatherAlertWorker>(
            3, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.vayu.weather.data.worker.WeatherAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun setupWidgetUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<com.vayu.weather.data.worker.WeatherWidgetWorker>(
            1, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.vayu.weather.data.worker.WeatherWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}