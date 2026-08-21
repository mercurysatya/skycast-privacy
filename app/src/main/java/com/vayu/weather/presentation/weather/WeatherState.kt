package com.vayu.weather.presentation.weather

import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.WeatherInfo

data class WeatherState(
    val weatherInfo: WeatherInfo? = null,
    val airQuality: AirQuality? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
    val lastUpdatedTime: String? = null
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class WindUnit { KPH, MPH, MS, KNOTS }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KPH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val rainAlertThreshold: Int = 50
)
