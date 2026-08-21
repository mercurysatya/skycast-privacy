package com.vayu.weather.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WindUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsState(
                temperatureUnit = settingsManager.getTemperatureUnit(),
                windUnit = settingsManager.getWindUnit(),
                themeMode = settingsManager.getThemeMode(),
                useDynamicColor = settingsManager.getUseDynamicColor(),
                notificationsEnabled = settingsManager.getNotificationsEnabled(),
                rainAlertThreshold = settingsManager.getRainAlertThreshold()
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
}
