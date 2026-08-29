package com.vayu.weather.presentation.components.skycast

import androidx.compose.ui.graphics.Color

/**
 * SkyCast dynamic background palette.
 *
 * Picks a vertical gradient that subtly reflects the current weather
 * condition and time of day. The gradients are deliberately quiet — strong
 * enough to communicate "what kind of day is it" without competing with the
 * forecast data sitting on top.
 */
object SkyCastBackground {

    /** Vertical gradient colors for a given WMO weather code + day/night. */
    fun gradientFor(weatherCode: Int, isDay: Boolean): List<Color> = when {
        // Clear night
        !isDay && (weatherCode == 0 || weatherCode == 1) -> listOf(
            Color(0xFF0B1226),
            Color(0xFF1B2347)
        )

        // Thunderstorm
        weatherCode in 95..99 -> listOf(
            Color(0xFF0E1326),
            Color(0xFF1F1F3D)
        )

        // Heavy rain / showers
        weatherCode in 65..82 -> listOf(
            Color(0xFF0B1726),
            Color(0xFF1A2540)
        )

        // Rain / drizzle
        weatherCode in 51..67 -> listOf(
            Color(0xFF0F1B2D),
            Color(0xFF1C2C45)
        )

        // Snow
        weatherCode in 71..86 -> listOf(
            Color(0xFF13243A),
            Color(0xFF243A55)
        )

        // Fog
        weatherCode in 45..48 -> listOf(
            Color(0xFF1B2530),
            Color(0xFF2A3540)
        )

        // Overcast / cloudy
        weatherCode in 2..3 -> listOf(
            Color(0xFF0F1A2E),
            Color(0xFF20294A)
        )

        // Partly cloudy (with day tint)
        weatherCode == 1 && isDay -> listOf(
            Color(0xFF142B45),
            Color(0xFF25416A)
        )

        // Clear day → warm daylight gradient
        weatherCode == 0 && isDay -> listOf(
            Color(0xFF1B3A6B),
            Color(0xFF2C5C9E)
        )

        // Fallback
        else -> listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B)
        )
    }
}
