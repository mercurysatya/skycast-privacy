package com.vayu.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vayu.weather.presentation.SplashGate
import com.vayu.weather.presentation.VayuApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Hold the splash until the app has picked its start destination,
        // so the Search pane never flashes before the dashboard is shown.
        // Only reset on a real cold start — on rotation/process recreation
        // the flag is already correct and re-holding it re-flashed the splash.
        splash.setKeepOnScreenCondition { !SplashGate.isReady }
        if (savedInstanceState == null) {
            SplashGate.isReady = false
        }
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VayuApp()
        }
    }
}
