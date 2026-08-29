package com.vayu.weather.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Open-Meteo WMO weather code → Material icon mapping.
 *
 * Shared by every screen that needs to display a weather glyph.
 */
fun getWeatherIcon(weatherCode: Int, isDay: Boolean): ImageVector = when (weatherCode) {
    0 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    1 -> Icons.Rounded.WbSunny
    2 -> Icons.Rounded.Cloud
    3 -> Icons.Rounded.CloudQueue
    45, 48 -> Icons.Rounded.CloudQueue
    51, 53, 55, 56, 57 -> Icons.Rounded.WaterDrop
    61, 63, 65, 66, 67 -> Icons.Rounded.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    80, 81, 82 -> Icons.Rounded.Umbrella
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.CloudOff
}

/** Plain English description that is safe for non-translated contexts (logs, analytics). */
fun plainWeatherDescription(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "clear sky" else "clear night"
    1 -> "mainly clear"
    2 -> "partly cloudy"
    3 -> "overcast"
    45, 48 -> "fog"
    51 -> "light drizzle"
    53 -> "moderate drizzle"
    55 -> "dense drizzle"
    61 -> "slight rain"
    63 -> "moderate rain"
    65 -> "heavy rain"
    71 -> "slight snow"
    73 -> "moderate snow"
    75 -> "heavy snow"
    80 -> "rain showers"
    81 -> "moderate rain showers"
    82 -> "violent rain showers"
    95 -> "thunderstorm"
    96 -> "thunderstorm with hail"
    99 -> "thunderstorm with heavy hail"
    else -> "cloudy"
}
