package com.vayu.weather.presentation.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.use_case.ClearWeatherAlertsUseCase
import com.vayu.weather.domain.use_case.DeleteWeatherAlertUseCase
import com.vayu.weather.domain.use_case.GetWeatherAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getWeatherAlertsUseCase: GetWeatherAlertsUseCase,
    private val deleteWeatherAlertUseCase: DeleteWeatherAlertUseCase,
    private val clearWeatherAlertsUseCase: ClearWeatherAlertsUseCase,
    private val settingsManager: SettingsManager
) : ViewModel() {

    var state by mutableStateOf(AlertsState())
        private set

    private val snoozeDuration = mutableStateOf(3600000L)
    private val pushFrequencyHours = mutableStateOf(24)

    init {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            getWeatherAlertsUseCase().collectLatest { alerts ->
                state = state.copy(alerts = alerts, isLoading = false)
            }
        }
        // Load snooze duration and push frequency from settings
        viewModelScope.launch {
            snoozeDuration.value = settingsManager.getSnoozeDurationMs().toLong()
            pushFrequencyHours.value = settingsManager.getPushAlertFrequencyHours()
        }
    }

    fun deleteAlert(alert: WeatherAlert) {
        viewModelScope.launch {
            deleteWeatherAlertUseCase(alert.id)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            clearWeatherAlertsUseCase()
        }
    }

    fun setSeverityFilter(filter: SeverityFilter) {
        state = state.copy(severityFilter = filter)
    }

    fun setWeatherTypeFilter(filter: WeatherTypeFilter) {
        state = state.copy(weatherTypeFilter = filter)
    }

    fun toggleExpandAlert(alertId: Long) {
        state = state.copy(
            expandedAlertId = if (state.expandedAlertId == alertId) null else alertId
        )
    }

    fun snoozeAlert(alert: WeatherAlert, durationMs: Long) {
        val newSnoozedIds = state.snoozedAlertIds + alert.id
        state = state.copy(snoozedAlertIds = newSnoozedIds)

        // Auto-unsnooze after duration
        viewModelScope.launch {
            delay(durationMs)
            val currentSnoozed = state.snoozedAlertIds
            if (currentSnoozed.contains(alert.id)) {
                state = state.copy(snoozedAlertIds = currentSnoozed - alert.id)
            }
        }
    }

    fun unsnoozeAlert(alert: WeatherAlert) {
        state = state.copy(snoozedAlertIds = state.snoozedAlertIds - alert.id)
    }

    fun updateSnoozeDuration(durationMs: Int) {
        snoozeDuration.value = durationMs.toLong()
        viewModelScope.launch {
            settingsManager.setSnoozeDurationMs(durationMs)
        }
    }

    fun updatePushFrequency(frequencyHours: Int) {
        pushFrequencyHours.value = frequencyHours
        viewModelScope.launch {
            settingsManager.setPushAlertFrequencyHours(frequencyHours)
            // Schedule next push notification
            scheduleNextPushNotification(frequencyHours)
        }
    }

    private fun scheduleNextPushNotification(frequencyHours: Int) {
        if (frequencyHours <= 0) return

        viewModelScope.launch {
            delay(frequencyHours * 60 * 60 * 1000L)
            // Trigger push notification cycle via worker
            CoroutineScope(Dispatchers.IO).launch {
                // This will be handled by the WeatherAlertWorker scheduled periodically
            }
        }
    }
}
