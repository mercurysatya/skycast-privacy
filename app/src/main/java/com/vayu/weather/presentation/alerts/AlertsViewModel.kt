package com.vayu.weather.presentation.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.use_case.ClearWeatherAlertsUseCase
import com.vayu.weather.domain.use_case.DeleteWeatherAlertUseCase
import com.vayu.weather.domain.use_case.GetWeatherAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
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

    private val snoozeDuration = mutableLongStateOf(3600000L)
    private val pushFrequencyHours = mutableIntStateOf(24)

    init {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            getWeatherAlertsUseCase()
                .catch {
                    // A failed query must not leave the history view on an
                    // endless spinner; show the empty state instead.
                    state = state.copy(isLoading = false)
                }
                .collectLatest { alerts ->
                    state = state.copy(alerts = alerts, isLoading = false)
                }
        }
        // Safety net for a slow first emission (the history entry point has no
        // other content until loading ends).
        viewModelScope.launch {
            delay(5_000)
            if (state.isLoading) {
                state = state.copy(isLoading = false)
            }
        }
        // Load snooze duration and push frequency from settings
        viewModelScope.launch {
            snoozeDuration.value = settingsManager.getSnoozeDurationMs().toLong()
            pushFrequencyHours.value = settingsManager.getPushAlertFrequencyHours()
            state = state.copy(
                snoozeDurationMinutes = (settingsManager.getSnoozeDurationMs() / 60_000)
                    .coerceIn(5, 1440)
            )
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
        val clamped = durationMs.coerceIn(5 * 60_000, 1440 * 60_000)
        snoozeDuration.value = clamped.toLong()
        state = state.copy(
            snoozeDurationMinutes = clamped / 60_000
        )
        viewModelScope.launch {
            settingsManager.setSnoozeDurationMs(clamped)
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
        // Push notification scheduling is handled by WeatherAlertWorker periodic worker
    }
}
