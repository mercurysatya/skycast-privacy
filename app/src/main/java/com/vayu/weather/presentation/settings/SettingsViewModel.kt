package com.vayu.weather.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.use_case.ClearWeatherCacheUseCase
import com.vayu.weather.domain.use_case.DeleteAllLocalDataUseCase
import com.vayu.weather.presentation.weather.AlertSeverity
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WidgetSize
import com.vayu.weather.presentation.weather.WindUnit
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vayu.weather.data.worker.WeatherAlertWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsManager: SettingsManager,
    private val deleteAllLocalDataUseCase: DeleteAllLocalDataUseCase,
    private val clearWeatherCacheUseCase: ClearWeatherCacheUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val severity = try {
                AlertSeverity.valueOf(settingsManager.getSeverityFilter().uppercase())
            } catch (_: Exception) { AlertSeverity.ALL }
            val widgetSz = try {
                WidgetSize.valueOf(settingsManager.getWidgetSize().uppercase())
            } catch (_: Exception) { WidgetSize.MEDIUM }

            _state.value = SettingsState(
                temperatureUnit = settingsManager.getTemperatureUnit(),
                windUnit = settingsManager.getWindUnit(),
                themeMode = settingsManager.getThemeMode(),
                useDynamicColor = settingsManager.getUseDynamicColor(),
                notificationsEnabled = settingsManager.getNotificationsEnabled(),
                rainAlertThreshold = settingsManager.getRainAlertThreshold(),
                checkIntervalHours = settingsManager.getCheckIntervalHours(),
                severityFilter = severity,
                widgetSize = widgetSz,
                windAlertThreshold = settingsManager.getWindAlertThreshold(),
                enableWindAlerts = settingsManager.getEnableWindAlerts(),
                uvAlertThreshold = settingsManager.getUvAlertThreshold(),
                enableUvAlerts = settingsManager.getEnableUvAlerts(),
                heatAlertThreshold = settingsManager.getHeatAlertThreshold(),
                enableHeatAlerts = settingsManager.getEnableHeatAlerts(),
                coldAlertThreshold = settingsManager.getColdAlertThreshold(),
                enableColdAlerts = settingsManager.getEnableColdAlerts()
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
            _state.update { it.copy(themeMode = mode) }
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val newUnit = if (_state.value.temperatureUnit == TemperatureUnit.CELSIUS)
                TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
            settingsManager.setTemperatureUnit(newUnit)
            _state.update { it.copy(temperatureUnit = newUnit) }
        }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch {
            settingsManager.setWindUnit(unit)
            _state.update { it.copy(windUnit = unit) }
        }
    }

    fun setUseDynamicColor(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setUseDynamicColor(value)
            _state.update { it.copy(useDynamicColor = value) }
        }
    }

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationsEnabled(value)
            _state.update { it.copy(notificationsEnabled = value) }
        }
    }

    fun setRainAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setRainAlertThreshold(value)
            _state.update { it.copy(rainAlertThreshold = value) }
        }
    }

    fun setWindAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setWindAlertThreshold(value)
            _state.update { it.copy(windAlertThreshold = value) }
        }
    }

    fun setEnableWindAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableWindAlerts(value)
            _state.update { it.copy(enableWindAlerts = value) }
        }
    }

    fun setUvAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setUvAlertThreshold(value)
            _state.update { it.copy(uvAlertThreshold = value) }
        }
    }

    fun setEnableUvAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableUvAlerts(value)
            _state.update { it.copy(enableUvAlerts = value) }
        }
    }

    fun setHeatAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setHeatAlertThreshold(value)
            _state.update { it.copy(heatAlertThreshold = value) }
        }
    }

    fun setEnableHeatAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableHeatAlerts(value)
            _state.update { it.copy(enableHeatAlerts = value) }
        }
    }

    fun setColdAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setColdAlertThreshold(value)
            _state.update { it.copy(coldAlertThreshold = value) }
        }
    }

    fun setEnableColdAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableColdAlerts(value)
            _state.update { it.copy(enableColdAlerts = value) }
        }
    }

    suspend fun isOnboardingComplete(): Boolean =
        settingsManager.isOnboardingComplete()

    fun setOnboardingComplete() {
        viewModelScope.launch {
            settingsManager.setOnboardingComplete(true)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            deleteAllLocalDataUseCase()
        }
    }

    fun setCheckIntervalHours(hours: Int) {
        viewModelScope.launch {
            settingsManager.setCheckIntervalHours(hours)
            _state.update { it.copy(checkIntervalHours = hours) }
            rescheduleAlertWorker(hours)
        }
    }

    private fun rescheduleAlertWorker(intervalHours: Int) {
        val workRequest = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WeatherAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun setSeverityFilter(severity: AlertSeverity) {
        viewModelScope.launch {
            settingsManager.setSeverityFilter(severity.name)
            _state.update { it.copy(severityFilter = severity) }
        }
    }

    fun setWidgetSize(size: WidgetSize) {
        viewModelScope.launch {
            settingsManager.setWidgetSize(size.name)
            _state.update { it.copy(widgetSize = size) }
            // Write to widget SharedPreferences so the widget can read it
            appContext.getSharedPreferences("weather_widget_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("widget_size", size.name).apply()
            refreshAllWidgets()
        }
    }

    private fun refreshAllWidgets() {
        viewModelScope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(com.vayu.weather.presentation.widget.WeatherWidget::class.java)
                glanceIds.forEach { glanceId ->
                    com.vayu.weather.presentation.widget.WeatherWidget().update(appContext, glanceId)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to refresh widgets", e)
            }
        }
    }

    fun clearWeatherCache() {
        viewModelScope.launch {
            clearWeatherCacheUseCase()
        }
    }
}
