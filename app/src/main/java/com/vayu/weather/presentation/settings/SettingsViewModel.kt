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

            _state.value = SettingsState(
                temperatureUnit = settingsManager.getTemperatureUnit(),
                windUnit = settingsManager.getWindUnit(),
                themeMode = settingsManager.getThemeMode(),
                useDynamicColor = settingsManager.getUseDynamicColor(),
                notificationsEnabled = settingsManager.getNotificationsEnabled(),
                rainAlertThreshold = settingsManager.getRainAlertThreshold(),
                checkIntervalHours = settingsManager.getCheckIntervalHours(),
                severityFilter = severity
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

    fun clearWeatherCache() {
        viewModelScope.launch {
            clearWeatherCacheUseCase()
        }
    }
}
