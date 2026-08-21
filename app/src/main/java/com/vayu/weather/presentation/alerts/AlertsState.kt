package com.vayu.weather.presentation.alerts

import com.vayu.weather.domain.repository.WeatherAlert

enum class SeverityFilter { ALL, HIGH, MEDIUM, LOW }

data class AlertsState(
    val alerts: List<WeatherAlert> = emptyList(),
    val isLoading: Boolean = false,
    val severityFilter: SeverityFilter = SeverityFilter.ALL,
    val expandedAlertId: Long? = null
) {
    val filteredAlerts: List<WeatherAlert>
        get() = when (severityFilter) {
            SeverityFilter.ALL -> alerts
            SeverityFilter.HIGH -> alerts.filter { it.severity == "high" }
            SeverityFilter.MEDIUM -> alerts.filter { it.severity == "medium" }
            SeverityFilter.LOW -> alerts.filter { it.severity == "low" }
        }

    val highCount: Int get() = alerts.count { it.severity == "high" }
    val mediumCount: Int get() = alerts.count { it.severity == "medium" }
    val lowCount: Int get() = alerts.count { it.severity == "low" }
}
