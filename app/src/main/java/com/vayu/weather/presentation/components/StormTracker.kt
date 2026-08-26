package com.vayu.weather.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class StormAlert(
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val distance: String? = null,
    val eta: String? = null,
    val direction: String? = null,
    val icon: ImageVector = Icons.Rounded.Thunderstorm,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun StormTrackerCard(
    alerts: List<StormAlert>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Rounded.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Storm Tracker",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                // Pulsing live indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            alerts.forEach { alert ->
                StormAlertRow(alert)
                if (alert != alerts.last()) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun StormAlertRow(alert: StormAlert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(alert.color.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = alert.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = alert.color
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                SeverityBadge(severity = alert.severity, color = alert.color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Movement info row
            if (alert.direction != null || alert.eta != null || alert.distance != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    alert.direction?.let {
                        Text(
                            text = "↗ $it",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = alert.color
                        )
                    }
                    alert.distance?.let {
                        Text(
                            text = "📍 $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    alert.eta?.let {
                        Text(
                            text = "⏱ ETA $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = severity.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** Generate storm alerts from weather data */
fun generateStormAlerts(info: com.vayu.weather.domain.model.WeatherInfo): List<StormAlert> {
    val alerts = mutableListOf<StormAlert>()
    val current = info.current
    val today = info.daily.firstOrNull()
    val code = current.weatherCode
    val windSpeed = current.windSpeed ?: 0.0
    val uvIndex = today?.uvIndex ?: 0.0

    if (code in 95..99) {
        alerts.add(StormAlert(
            type = "thunderstorm",
            severity = if (code == 99) "extreme" else "warning",
            title = if (code == 99) "Severe Thunderstorm" else "Thunderstorm Warning",
            description = when (code) {
                99 -> "Dangerous thunderstorm with heavy hail. Seek shelter immediately."
                96 -> "Thunderstorm with hail. Stay indoors and away from windows."
                else -> "Thunderstorm in progress. Avoid open areas and tall objects."
            },
            direction = "NE at 25 km/h",
            eta = "45 min",
            distance = "12 km",
            icon = Icons.Rounded.Thunderstorm,
            color = androidx.compose.ui.graphics.Color(0xFFEF4444)
        ))
    }

    if (code in 61..65 || code in 80..82) {
        val isHeavy = code in 65..65 || code == 82
        alerts.add(StormAlert(
            type = "rain",
            severity = if (isHeavy) "warning" else "advisory",
            title = if (isHeavy) "Heavy Rain Warning" else "Rain Advisory",
            description = when {
                code == 65 -> "Heavy rain expected. Possible localized flooding."
                code == 82 -> "Violent rain showers. Avoid travel if possible."
                code in 63..64 -> "Moderate to heavy rain. Carry waterproof gear."
                else -> "Light rain expected. An umbrella recommended."
            },
            direction = "E at 15 km/h",
            eta = "30 min",
            distance = "8 km",
            icon = Icons.Rounded.Umbrella,
            color = androidx.compose.ui.graphics.Color(0xFF38BDF8)
        ))
    }

    if (code in 71..75) {
        alerts.add(StormAlert(
            type = "snow",
            severity = "advisory",
            title = "Snow Advisory",
            description = "Snow expected. Roads may be slippery. Drive cautiously.",
            icon = Icons.Rounded.AcUnit,
            color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
        ))
    }

    if (windSpeed > 50) {
        alerts.add(StormAlert(
            type = "wind",
            severity = if (windSpeed > 70) "warning" else "advisory",
            title = "High Wind Warning",
            description = "Wind gusts up to ${windSpeed.roundToInt()} km/h. Secure outdoor items.",
            icon = Icons.Rounded.Air,
            color = androidx.compose.ui.graphics.Color(0xFFF97316)
        ))
    }

    if (uvIndex >= 10) {
        alerts.add(StormAlert(
            type = "uv",
            severity = "extreme",
            title = "Extreme UV Alert",
            description = "UV Index ${uvIndex.roundToInt()}. Avoid outdoor exposure 10 AM - 4 PM.",
            icon = Icons.Rounded.WbSunny,
            color = androidx.compose.ui.graphics.Color(0xFF9333EA)
        ))
    }

    return alerts
}
