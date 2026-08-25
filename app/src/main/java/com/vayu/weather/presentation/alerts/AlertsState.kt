package com.vayu.weather.presentation.alerts

import com.vayu.weather.domain.repository.WeatherAlert

enum class SeverityFilter { ALL, HIGH, MEDIUM, LOW }
enum class WeatherTypeFilter { ALL, RAIN, WIND, UV, HEAT, COLD }

data class AlertsState(
    val alerts: List<WeatherAlert> = emptyList(),
    val isLoading: Boolean = false,
    val severityFilter: SeverityFilter = SeverityFilter.ALL,
    val weatherTypeFilter: WeatherTypeFilter = WeatherTypeFilter.ALL,
    val snoozedAlertIds: Set<Long> = emptySet(),
    val expandedAlertId: Long? = null
) {
    val filteredAlerts: List<WeatherAlert>
        get() = alerts
            .filter { alert -> !snoozedAlertIds.contains(alert.id) }
            .filter { alert ->
                when (severityFilter) {
                    SeverityFilter.ALL -> true
                    SeverityFilter.HIGH -> alert.severity == "high"
                    SeverityFilter.MEDIUM -> alert.severity == "medium"
                    SeverityFilter.LOW -> alert.severity == "low"
                }
            }
            .filter { alert ->
                when (weatherTypeFilter) {
                    WeatherTypeFilter.ALL -> true
                    WeatherTypeFilter.RAIN -> alert.title.contains("Rain", ignoreCase = true)
                    WeatherTypeFilter.WIND -> alert.title.contains("Wind", ignoreCase = true)
                    WeatherTypeFilter.UV -> alert.title.contains("UV", ignoreCase = true)
                    WeatherTypeFilter.HEAT -> alert.title.contains("Heat", ignoreCase = true)
                    WeatherTypeFilter.COLD -> alert.title.contains("Cold", ignoreCase = true)
                }
            }

    val highCount: Int get() = alerts.count { it.severity == "high" }
    val mediumCount: Int get() = alerts.count { it.severity == "medium" }
    val lowCount: Int get() = alerts.count { it.severity == "low" }
    val rainCount: Int get() = alerts.count { it.title.contains("Rain", ignoreCase = true) }
    val windCount: Int get() = alerts.count { it.title.contains("Wind", ignoreCase = true) }
    val uvCount: Int get() = alerts.count { it.title.contains("UV", ignoreCase = true) }
    val heatCount: Int get() = alerts.count { it.title.contains("Heat", ignoreCase = true) }
    val coldCount: Int get() = alerts.count { it.title.contains("Cold", ignoreCase = true) }
    val snoozedCount: Int get() = alerts.count { snoozedAlertIds.contains(it.id) }
}
