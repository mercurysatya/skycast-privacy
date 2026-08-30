package com.vayu.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vayu.weather.presentation.weather.AlertSeverity
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WidgetSize
import com.vayu.weather.presentation.weather.WindUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    /** Single-shot load of all preferences — reads DataStore once instead of 37 separate .first() calls. */
    suspend fun loadAllPreferences(): SettingsState {
        val prefs = dataStore.data.first()
        return SettingsState(
            temperatureUnit = if (prefs[Keys.IS_FAHRENHEIT] == true) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS,
            windUnit = try { WindUnit.valueOf(prefs[Keys.WIND_UNIT] ?: WindUnit.KPH.name) } catch (_: Exception) { WindUnit.KPH },
            themeMode = try { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) } catch (_: Exception) { ThemeMode.SYSTEM },
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            rainAlertThreshold = prefs[Keys.RAIN_ALERT_THRESHOLD] ?: 50,
            checkIntervalHours = prefs[Keys.CHECK_INTERVAL_HOURS] ?: 3,
            severityFilter = try { AlertSeverity.valueOf((prefs[Keys.SEVERITY_FILTER] ?: "all").uppercase()) } catch (_: Exception) { AlertSeverity.ALL },
            widgetSize = try { WidgetSize.valueOf((prefs[Keys.WIDGET_SIZE] ?: "MEDIUM").uppercase()) } catch (_: Exception) { WidgetSize.MEDIUM },
            windAlertThreshold = prefs[Keys.WIND_ALERT_THRESHOLD] ?: 60,
            enableWindAlerts = prefs[Keys.ENABLE_WIND_ALERTS] ?: true,
            uvAlertThreshold = prefs[Keys.UV_ALERT_THRESHOLD] ?: 8,
            enableUvAlerts = prefs[Keys.ENABLE_UV_ALERTS] ?: true,
            heatAlertThreshold = prefs[Keys.HEAT_ALERT_THRESHOLD] ?: 40,
            enableHeatAlerts = prefs[Keys.ENABLE_HEAT_ALERTS] ?: true,
            coldAlertThreshold = prefs[Keys.COLD_ALERT_THRESHOLD] ?: 0,
            enableColdAlerts = prefs[Keys.ENABLE_COLD_ALERTS] ?: true,
            use24hClock = prefs[Keys.USE_24H_CLOCK] ?: true,
            pressureUnit = prefs[Keys.PRESSURE_UNIT] ?: "hPa",
            precipitationUnit = prefs[Keys.PRECIPITATION_UNIT] ?: "mm",
            showHourlyForecast = prefs[Keys.SHOW_HOURLY_FORECAST] ?: true,
            showSunMoon = prefs[Keys.SHOW_SUN_MOON] ?: true,
            showAirQuality = prefs[Keys.SHOW_AIR_QUALITY] ?: true,
            showWeatherDetails = prefs[Keys.SHOW_WEATHER_DETAILS] ?: true,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartHour = prefs[Keys.QUIET_HOURS_START_HOUR] ?: 22,
            quietHoursStartMinute = prefs[Keys.QUIET_HOURS_START_MINUTE] ?: 0,
            quietHoursEndHour = prefs[Keys.QUIET_HOURS_END_HOUR] ?: 7,
            quietHoursEndMinute = prefs[Keys.QUIET_HOURS_END_MINUTE] ?: 0,
            notificationTime1Enabled = prefs[Keys.NOTIFICATION_TIME_1_ENABLED] ?: true,
            notificationTime1Hour = prefs[Keys.NOTIFICATION_TIME_1_HOUR] ?: 7,
            notificationTime1Minute = prefs[Keys.NOTIFICATION_TIME_1_MINUTE] ?: 0,
            notificationTime2Enabled = prefs[Keys.NOTIFICATION_TIME_2_ENABLED] ?: true,
            notificationTime2Hour = prefs[Keys.NOTIFICATION_TIME_2_HOUR] ?: 12,
            notificationTime2Minute = prefs[Keys.NOTIFICATION_TIME_2_MINUTE] ?: 0,
            notificationTime3Enabled = prefs[Keys.NOTIFICATION_TIME_3_ENABLED] ?: true,
            notificationTime3Hour = prefs[Keys.NOTIFICATION_TIME_3_HOUR] ?: 18,
            notificationTime3Minute = prefs[Keys.NOTIFICATION_TIME_3_MINUTE] ?: 0
        )
    }

private object Keys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val IS_FAHRENHEIT = booleanPreferencesKey("is_fahrenheit")
    val WIND_UNIT = stringPreferencesKey("wind_unit")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val RAIN_ALERT_THRESHOLD = intPreferencesKey("rain_alert_threshold")
    val CHECK_INTERVAL_HOURS = intPreferencesKey("check_interval_hours")
    val SEVERITY_FILTER = stringPreferencesKey("severity_filter")
    val WIDGET_SIZE = stringPreferencesKey("widget_size")
    val WIND_ALERT_THRESHOLD = intPreferencesKey("wind_alert_threshold")
    val UV_ALERT_THRESHOLD = intPreferencesKey("uv_alert_threshold")
    val HEAT_ALERT_THRESHOLD = intPreferencesKey("heat_alert_threshold")
    val COLD_ALERT_THRESHOLD = intPreferencesKey("cold_alert_threshold")
    val ENABLE_WIND_ALERTS = booleanPreferencesKey("enable_wind_alerts")
    val ENABLE_UV_ALERTS = booleanPreferencesKey("enable_uv_alerts")
    val ENABLE_HEAT_ALERTS = booleanPreferencesKey("enable_heat_alerts")
    val ENABLE_COLD_ALERTS = booleanPreferencesKey("enable_cold_alerts")
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    // Clock format
    val USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")
    // Pressure unit
    val PRESSURE_UNIT = stringPreferencesKey("pressure_unit")
    // Precipitation unit
    val PRECIPITATION_UNIT = stringPreferencesKey("precipitation_unit")
    // Section visibility
    val SHOW_HOURLY_FORECAST = booleanPreferencesKey("show_hourly_forecast")
    val SHOW_SUN_MOON = booleanPreferencesKey("show_sun_moon")
    val SHOW_AIR_QUALITY = booleanPreferencesKey("show_air_quality")
    val SHOW_WEATHER_DETAILS = booleanPreferencesKey("show_weather_details")
    // Snooze settings
    val ENABLE_SNOOZING = booleanPreferencesKey("enable_snoozing")

    // === Last loaded location (used as a fallback when GPS is unavailable) ===
    val LAST_LAT = doublePreferencesKey("last_lat")
    val LAST_LON = doublePreferencesKey("last_lon")
    val LAST_CITY = stringPreferencesKey("last_city")
    val SNOOZE_DURATION_MS = intPreferencesKey("snooze_duration_ms")
    // Push notification settings
    val ENABLE_PUSH_ALERTS = booleanPreferencesKey("enable_push_alerts")
    val PUSH_ALERT_FREQUENCY_HOURS = intPreferencesKey("push_alert_frequency_hours")
    // Quiet hours for notifications
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
    val QUIET_HOURS_START_MINUTE = intPreferencesKey("quiet_hours_start_minute")
    val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
    val QUIET_HOURS_END_MINUTE = intPreferencesKey("quiet_hours_end_minute")
    // Per-day notification times (07:00, 12:00, 18:00)
    val NOTIFICATION_TIME_1_HOUR = intPreferencesKey("notification_time_1_hour")
    val NOTIFICATION_TIME_1_MINUTE = intPreferencesKey("notification_time_1_minute")
    val NOTIFICATION_TIME_2_HOUR = intPreferencesKey("notification_time_2_hour")
    val NOTIFICATION_TIME_2_MINUTE = intPreferencesKey("notification_time_2_minute")
    val NOTIFICATION_TIME_3_HOUR = intPreferencesKey("notification_time_3_hour")
    val NOTIFICATION_TIME_3_MINUTE = intPreferencesKey("notification_time_3_minute")
    val NOTIFICATION_TIME_1_ENABLED = booleanPreferencesKey("notification_time_1_enabled")
    val NOTIFICATION_TIME_2_ENABLED = booleanPreferencesKey("notification_time_2_enabled")
    val NOTIFICATION_TIME_3_ENABLED = booleanPreferencesKey("notification_time_3_enabled")
}

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val temperatureUnitFlow: Flow<TemperatureUnit> = dataStore.data.map { prefs ->
        if (prefs[Keys.IS_FAHRENHEIT] == true) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
    }

    val windUnitFlow: Flow<WindUnit> = dataStore.data.map { prefs ->
        try {
            WindUnit.valueOf(prefs[Keys.WIND_UNIT] ?: WindUnit.KPH.name)
        } catch (e: Exception) {
            WindUnit.KPH
        }
    }

    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val useDynamicColorFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.USE_DYNAMIC_COLOR] ?: true
    }

    val rainAlertThresholdFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.RAIN_ALERT_THRESHOLD] ?: 50
    }

    suspend fun getNotificationsEnabled(): Boolean =
        dataStore.data.first()[Keys.NOTIFICATIONS_ENABLED] ?: true

    suspend fun setNotificationsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun getTemperatureUnit(): TemperatureUnit =
        try {
            if (dataStore.data.first()[Keys.IS_FAHRENHEIT] == true) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
        } catch (e: Exception) { TemperatureUnit.CELSIUS }

    suspend fun setTemperatureUnit(value: TemperatureUnit) {
        dataStore.edit { it[Keys.IS_FAHRENHEIT] = value == TemperatureUnit.FAHRENHEIT }
    }

    suspend fun getWindUnit(): WindUnit =
        try {
            WindUnit.valueOf(dataStore.data.first()[Keys.WIND_UNIT] ?: WindUnit.KPH.name)
        } catch (e: Exception) { WindUnit.KPH }

    suspend fun setWindUnit(value: WindUnit) {
        dataStore.edit { it[Keys.WIND_UNIT] = value.name }
    }

    suspend fun getThemeMode(): ThemeMode =
        try {
            ThemeMode.valueOf(dataStore.data.first()[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) { ThemeMode.SYSTEM }

    suspend fun setThemeMode(value: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = value.name }
    }

    suspend fun getUseDynamicColor(): Boolean =
        dataStore.data.first()[Keys.USE_DYNAMIC_COLOR] ?: true

    suspend fun setUseDynamicColor(value: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = value }
    }

    suspend fun getRainAlertThreshold(): Int =
        dataStore.data.first()[Keys.RAIN_ALERT_THRESHOLD] ?: 50

    suspend fun setRainAlertThreshold(value: Int) {
        dataStore.edit { it[Keys.RAIN_ALERT_THRESHOLD] = value }
    }

    val checkIntervalHoursFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CHECK_INTERVAL_HOURS] ?: 3
    }

    suspend fun getCheckIntervalHours(): Int =
        dataStore.data.first()[Keys.CHECK_INTERVAL_HOURS] ?: 3

    suspend fun setCheckIntervalHours(value: Int) {
        dataStore.edit { it[Keys.CHECK_INTERVAL_HOURS] = value }
    }

    val severityFilterFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SEVERITY_FILTER] ?: "all"
    }

    suspend fun getSeverityFilter(): String =
        dataStore.data.first()[Keys.SEVERITY_FILTER] ?: "all"

    suspend fun setSeverityFilter(value: String) {
        dataStore.edit { it[Keys.SEVERITY_FILTER] = value }
    }

    val enableSnoozingFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_SNOOZING] ?: false
    }

    suspend fun getEnableSnoozing(): Boolean =
        dataStore.data.first()[Keys.ENABLE_SNOOZING] ?: false

    suspend fun setEnableSnoozing(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_SNOOZING] = value }
    }

    val snoozeDurationMsFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SNOOZE_DURATION_MS] ?: 3600000
    }

    suspend fun getSnoozeDurationMs(): Int =
        dataStore.data.first()[Keys.SNOOZE_DURATION_MS] ?: 3600000

    suspend fun setSnoozeDurationMs(value: Int) {
        dataStore.edit { it[Keys.SNOOZE_DURATION_MS] = value }
    }

    val enablePushAlertsFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ENABLE_PUSH_ALERTS] ?: true
    }

    suspend fun getEnablePushAlerts(): Boolean =
        dataStore.data.first()[Keys.ENABLE_PUSH_ALERTS] ?: true

    suspend fun setEnablePushAlerts(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_PUSH_ALERTS] = value }
    }

    val pushAlertFrequencyHoursFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.PUSH_ALERT_FREQUENCY_HOURS] ?: 24
    }

    suspend fun getPushAlertFrequencyHours(): Int =
        dataStore.data.first()[Keys.PUSH_ALERT_FREQUENCY_HOURS] ?: 24

    suspend fun setPushAlertFrequencyHours(value: Int) {
        dataStore.edit { it[Keys.PUSH_ALERT_FREQUENCY_HOURS] = value }
    }

    val widgetSizeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_SIZE] ?: "MEDIUM"
    }

    suspend fun getWidgetSize(): String =
        dataStore.data.first()[Keys.WIDGET_SIZE] ?: "MEDIUM"

    suspend fun setWidgetSize(value: String) {
        dataStore.edit { it[Keys.WIDGET_SIZE] = value }
    }

    // Last resolved city / region (used by the widget snapshot)
    suspend fun getLastCityName(): String? = getLastCity()

    // Wind alert
    suspend fun getWindAlertThreshold(): Int =
        dataStore.data.first()[Keys.WIND_ALERT_THRESHOLD] ?: 60

    suspend fun setWindAlertThreshold(value: Int) {
        dataStore.edit { it[Keys.WIND_ALERT_THRESHOLD] = value }
    }

    suspend fun getEnableWindAlerts(): Boolean =
        dataStore.data.first()[Keys.ENABLE_WIND_ALERTS] ?: true

    suspend fun setEnableWindAlerts(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_WIND_ALERTS] = value }
    }

    // UV alert
    suspend fun getUvAlertThreshold(): Int =
        dataStore.data.first()[Keys.UV_ALERT_THRESHOLD] ?: 8

    suspend fun setUvAlertThreshold(value: Int) {
        dataStore.edit { it[Keys.UV_ALERT_THRESHOLD] = value }
    }

    suspend fun getEnableUvAlerts(): Boolean =
        dataStore.data.first()[Keys.ENABLE_UV_ALERTS] ?: true

    suspend fun setEnableUvAlerts(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_UV_ALERTS] = value }
    }

    // Heat alert
    suspend fun getHeatAlertThreshold(): Int =
        dataStore.data.first()[Keys.HEAT_ALERT_THRESHOLD] ?: 40

    suspend fun setHeatAlertThreshold(value: Int) {
        dataStore.edit { it[Keys.HEAT_ALERT_THRESHOLD] = value }
    }

    suspend fun getEnableHeatAlerts(): Boolean =
        dataStore.data.first()[Keys.ENABLE_HEAT_ALERTS] ?: true

    suspend fun setEnableHeatAlerts(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_HEAT_ALERTS] = value }
    }

    // Cold alert
    suspend fun getColdAlertThreshold(): Int =
        dataStore.data.first()[Keys.COLD_ALERT_THRESHOLD] ?: 0

    suspend fun setColdAlertThreshold(value: Int) {
        dataStore.edit { it[Keys.COLD_ALERT_THRESHOLD] = value }
    }

    suspend fun getEnableColdAlerts(): Boolean =
        dataStore.data.first()[Keys.ENABLE_COLD_ALERTS] ?: true

    suspend fun setEnableColdAlerts(value: Boolean) {
        dataStore.edit { it[Keys.ENABLE_COLD_ALERTS] = value }
    }

    suspend fun isOnboardingComplete(): Boolean =
        dataStore.data.first()[Keys.ONBOARDING_COMPLETE] ?: false

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    // === Clock Format ===
    suspend fun getUse24hClock(): Boolean =
        dataStore.data.first()[Keys.USE_24H_CLOCK] ?: true

    suspend fun setUse24hClock(value: Boolean) {
        dataStore.edit { it[Keys.USE_24H_CLOCK] = value }
    }

    // === Pressure Unit ===
    suspend fun getPressureUnit(): String =
        dataStore.data.first()[Keys.PRESSURE_UNIT] ?: "hPa"

    suspend fun setPressureUnit(value: String) {
        dataStore.edit { it[Keys.PRESSURE_UNIT] = value }
    }

    // === Precipitation Unit ===
    suspend fun getPrecipitationUnit(): String =
        dataStore.data.first()[Keys.PRECIPITATION_UNIT] ?: "mm"

    suspend fun setPrecipitationUnit(value: String) {
        dataStore.edit { it[Keys.PRECIPITATION_UNIT] = value }
    }

    // === Section Visibility ===
    suspend fun getShowHourlyForecast(): Boolean =
        dataStore.data.first()[Keys.SHOW_HOURLY_FORECAST] ?: true

    suspend fun setShowHourlyForecast(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_HOURLY_FORECAST] = value }
    }

    suspend fun getShowSunMoon(): Boolean =
        dataStore.data.first()[Keys.SHOW_SUN_MOON] ?: true

    suspend fun setShowSunMoon(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_SUN_MOON] = value }
    }

    suspend fun getShowAirQuality(): Boolean =
        dataStore.data.first()[Keys.SHOW_AIR_QUALITY] ?: true

    suspend fun setShowAirQuality(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_AIR_QUALITY] = value }
    }

    suspend fun getShowWeatherDetails(): Boolean =
        dataStore.data.first()[Keys.SHOW_WEATHER_DETAILS] ?: true

    suspend fun setShowWeatherDetails(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_WEATHER_DETAILS] = value }
    }

    // === Last loaded location (persisted across launches) ===
    suspend fun getLastLat(): Double? = dataStore.data.first()[Keys.LAST_LAT]
    suspend fun getLastLon(): Double? = dataStore.data.first()[Keys.LAST_LON]
    suspend fun getLastCity(): String? = dataStore.data.first()[Keys.LAST_CITY]

    suspend fun setLastLocation(lat: Double, lon: Double, city: String?) {
        dataStore.edit {
            it[Keys.LAST_LAT] = lat
            it[Keys.LAST_LON] = lon
            if (!city.isNullOrBlank()) it[Keys.LAST_CITY] = city
        }
    }

    // === Quiet Hours ===
    suspend fun getQuietHoursEnabled(): Boolean = dataStore.data.first()[Keys.QUIET_HOURS_ENABLED] ?: false
    suspend fun setQuietHoursEnabled(value: Boolean) { dataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = value } }
    suspend fun getQuietHoursStartHour(): Int = dataStore.data.first()[Keys.QUIET_HOURS_START_HOUR] ?: 22
    suspend fun setQuietHoursStartHour(value: Int) { dataStore.edit { it[Keys.QUIET_HOURS_START_HOUR] = value } }
    suspend fun getQuietHoursStartMinute(): Int = dataStore.data.first()[Keys.QUIET_HOURS_START_MINUTE] ?: 0
    suspend fun setQuietHoursStartMinute(value: Int) { dataStore.edit { it[Keys.QUIET_HOURS_START_MINUTE] = value } }
    suspend fun getQuietHoursEndHour(): Int = dataStore.data.first()[Keys.QUIET_HOURS_END_HOUR] ?: 7
    suspend fun setQuietHoursEndHour(value: Int) { dataStore.edit { it[Keys.QUIET_HOURS_END_HOUR] = value } }
    suspend fun getQuietHoursEndMinute(): Int = dataStore.data.first()[Keys.QUIET_HOURS_END_MINUTE] ?: 0
    suspend fun setQuietHoursEndMinute(value: Int) { dataStore.edit { it[Keys.QUIET_HOURS_END_MINUTE] = value } }

    // === Per-day Notification Times ===
    suspend fun getNotificationTime1Hour(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_1_HOUR] ?: 7
    suspend fun setNotificationTime1Hour(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_1_HOUR] = value } }
    suspend fun getNotificationTime1Minute(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_1_MINUTE] ?: 0
    suspend fun setNotificationTime1Minute(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_1_MINUTE] = value } }
    suspend fun getNotificationTime1Enabled(): Boolean = dataStore.data.first()[Keys.NOTIFICATION_TIME_1_ENABLED] ?: true
    suspend fun setNotificationTime1Enabled(value: Boolean) { dataStore.edit { it[Keys.NOTIFICATION_TIME_1_ENABLED] = value } }

    suspend fun getNotificationTime2Hour(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_2_HOUR] ?: 12
    suspend fun setNotificationTime2Hour(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_2_HOUR] = value } }
    suspend fun getNotificationTime2Minute(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_2_MINUTE] ?: 0
    suspend fun setNotificationTime2Minute(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_2_MINUTE] = value } }
    suspend fun getNotificationTime2Enabled(): Boolean = dataStore.data.first()[Keys.NOTIFICATION_TIME_2_ENABLED] ?: true
    suspend fun setNotificationTime2Enabled(value: Boolean) { dataStore.edit { it[Keys.NOTIFICATION_TIME_2_ENABLED] = value } }

    suspend fun getNotificationTime3Hour(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_3_HOUR] ?: 18
    suspend fun setNotificationTime3Hour(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_3_HOUR] = value } }
    suspend fun getNotificationTime3Minute(): Int = dataStore.data.first()[Keys.NOTIFICATION_TIME_3_MINUTE] ?: 0
    suspend fun setNotificationTime3Minute(value: Int) { dataStore.edit { it[Keys.NOTIFICATION_TIME_3_MINUTE] = value } }
    suspend fun getNotificationTime3Enabled(): Boolean = dataStore.data.first()[Keys.NOTIFICATION_TIME_3_ENABLED] ?: true
    suspend fun setNotificationTime3Enabled(value: Boolean) { dataStore.edit { it[Keys.NOTIFICATION_TIME_3_ENABLED] = value } }
}
