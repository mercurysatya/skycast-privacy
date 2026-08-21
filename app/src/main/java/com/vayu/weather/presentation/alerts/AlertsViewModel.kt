package com.vayu.weather.presentation.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.use_case.ClearWeatherAlertsUseCase
import com.vayu.weather.domain.use_case.DeleteWeatherAlertUseCase
import com.vayu.weather.domain.use_case.GetWeatherAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getWeatherAlertsUseCase: GetWeatherAlertsUseCase,
    private val deleteWeatherAlertUseCase: DeleteWeatherAlertUseCase,
    private val clearWeatherAlertsUseCase: ClearWeatherAlertsUseCase
) : ViewModel() {

    var state by mutableStateOf(AlertsState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            getWeatherAlertsUseCase().collectLatest { alerts ->
                state = state.copy(alerts = alerts, isLoading = false)
            }
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

    fun toggleExpandAlert(alertId: Long) {
        state = state.copy(
            expandedAlertId = if (state.expandedAlertId == alertId) null else alertId
        )
    }
}
