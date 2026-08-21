package com.vayu.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WindUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

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

    val widgetSizeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_SIZE] ?: "MEDIUM"
    }

    suspend fun getWidgetSize(): String =
        dataStore.data.first()[Keys.WIDGET_SIZE] ?: "MEDIUM"

    suspend fun setWidgetSize(value: String) {
        dataStore.edit { it[Keys.WIDGET_SIZE] = value }
    }

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
}
