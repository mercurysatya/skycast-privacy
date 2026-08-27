package com.vayu.weather.presentation

/**
 * Gate for the splash screen: MainActivity keeps the splash on screen until
 * VayuApp finished its initial navigation (or is otherwise ready to show UI),
 * hiding the first-frame pane flicker on cold start.
 */
object SplashGate {
    @Volatile
    var isReady: Boolean = true
}
