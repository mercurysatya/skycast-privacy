package com.vayu.weather.presentation.alerts

import com.vayu.weather.domain.repository.WeatherAlert

data class AlertsState(
    val alerts: List<WeatherAlert> = emptyList(),
    val isLoading: Boolean = false
)
