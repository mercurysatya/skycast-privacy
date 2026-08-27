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
        // so the Search pane never flashes before the dashboard is shown
        splash.setKeepOnScreenCondition { !SplashGate.isReady }
        SplashGate.isReady = false
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VayuApp()
        }
    }
}
