package com.vayu.weather.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vayu.weather.R

/**
 * Returns a human-readable condition label for an Open-Meteo weather code.
 *
 * Used by the dashboard hero, hourly timeline, daily forecast, and many other
 * SkyCast components. Kept in a non-internal package so that any composable
 * can call it without leaking the weather package.
 */
@Composable
fun localizedWeatherDescription(weatherCode: Int, isDay: Boolean): String {
    val resId = when (weatherCode) {
        0 -> if (isDay) R.string.weather_clear_sky else R.string.weather_clear_night
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_fog
        51 -> R.string.weather_light_drizzle
        53 -> R.string.weather_moderate_drizzle
        55 -> R.string.weather_dense_drizzle
        61 -> R.string.weather_slight_rain
        63 -> R.string.weather_moderate_rain
        65 -> R.string.weather_heavy_rain
        71 -> R.string.weather_slight_snow
        73 -> R.string.weather_moderate_snow
        75 -> R.string.weather_heavy_snow
        80 -> R.string.weather_slight_rain_showers
        81 -> R.string.weather_moderate_rain_showers
        82 -> R.string.weather_violent_rain_showers
        95 -> R.string.weather_thunderstorm
        96 -> R.string.weather_thunderstorm_hail
        99 -> R.string.weather_thunderstorm_heavy_hail
        else -> R.string.weather_cloudy
    }
    return stringResource(resId)
}
