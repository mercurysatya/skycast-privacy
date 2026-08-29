package com.vayu.weather.presentation.weather

import com.vayu.weather.data.local.WeatherAlertEntity
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.WeatherInfo

data class WeatherState(
    val weatherInfo: WeatherInfo? = null,
    val airQuality: AirQuality? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val refreshError: String? = null,
    val lastUpdatedTime: String? = null,
    /** Temperature from a snapshot ~24h ago, used for the "vs yesterday" pill. */
    val previousDayTempC: Double? = null,
    /** Region/state name (e.g. "Andhra Pradesh") for richer location display. */
    val regionName: String? = null,
    /** Currently active severe-weather alerts for this location. */
    val alerts: List<WeatherAlertEntity> = emptyList()
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class WindUnit { KPH, MPH, MS, KNOTS }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AlertSeverity { ALL, HIGH, HIGH_MEDIUM }
enum class WidgetSize { SMALL, MEDIUM, LARGE }

data class SettingsState(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KPH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val rainAlertThreshold: Int = 50,
    val checkIntervalHours: Int = 3,
    val severityFilter: AlertSeverity = AlertSeverity.ALL,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val windAlertThreshold: Int = 60,
    val enableWindAlerts: Boolean = true,
    val uvAlertThreshold: Int = 8,
    val enableUvAlerts: Boolean = true,
    val heatAlertThreshold: Int = 40,
    val enableHeatAlerts: Boolean = true,
    val coldAlertThreshold: Int = 0,
    val enableColdAlerts: Boolean = true,
    val use24hClock: Boolean = true,
    val pressureUnit: String = "hPa",
    val precipitationUnit: String = "mm",
    val showHourlyForecast: Boolean = true,
    val showSunMoon: Boolean = true,
    val showAirQuality: Boolean = true,
    val showWeatherDetails: Boolean = true,
    // Quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    // Per-day notification times
    val notificationTime1Enabled: Boolean = true,
    val notificationTime1Hour: Int = 7,
    val notificationTime1Minute: Int = 0,
    val notificationTime2Enabled: Boolean = true,
    val notificationTime2Hour: Int = 12,
    val notificationTime2Minute: Int = 0,
    val notificationTime3Enabled: Boolean = true,
    val notificationTime3Hour: Int = 18,
    val notificationTime3Minute: Int = 0
)
