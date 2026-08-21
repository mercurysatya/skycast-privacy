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
}
