package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.data.local.WeatherAlertEntity
import com.vayu.weather.ui.theme.SkyAlertSeverity
import com.vayu.weather.ui.theme.SkyCastTokens
import java.time.format.DateTimeFormatter

/**
 * SkyCast severe weather alert card.
 *
 * Always pairs a color with a non-color identifier (the severity label and
 * a leading icon) so users with color blindness can still distinguish
 * between alert levels.
 */

/** Severity rank for sorting: higher = more urgent. */
fun SkyAlertSeverity.rank(): Int = when (this) {
    SkyAlertSeverity.Info -> 0
    SkyAlertSeverity.Advisory -> 1
    SkyAlertSeverity.Watch -> 2
    SkyAlertSeverity.Warning -> 3
    SkyAlertSeverity.Emergency -> 4
}

/** Maximum age in milliseconds for an alert to be considered active (24 hours). */
const val ALERT_MAX_AGE_MS: Long = 24L * 60L * 60L * 1000L

/** Filter and sort alerts: remove expired (24h old), then sort by severity descending. */
fun filterAndSortAlerts(alerts: List<WeatherAlertEntity>): List<WeatherAlertEntity> {
    val now = System.currentTimeMillis()
    return alerts
        .filter { now - it.timestamp <= ALERT_MAX_AGE_MS }
        .sortedByDescending { severityFromLevel(it.severity).rank() }
}
@Composable
fun SkyCastAlertCard(
    alert: WeatherAlertEntity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val severity = severityFromLevel(alert.severity)
    val accent = severity.color()

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(SkyCastTokens.RadiusLg))
        .background(accent.copy(alpha = 0.10f))
        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(SkyCastTokens.RadiusLg))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .semantics(mergeDescendants = true) {
            contentDescription = "${severity.label} alert: ${alert.title}. ${alert.message}"
        }
        .padding(16.dp)

    Column(modifier = cardModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = severity.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = alert.message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        alert.timestamp.let { ts ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Issued ${formatTimestamp(ts)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        alert.cityName?.takeIf { it.isNotBlank() }?.let { city ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "For: $city",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SkyCastAlertList(
    alerts: List<WeatherAlertEntity>,
    onAlertClick: (WeatherAlertEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SkyCastSectionHeader(title = "Alerts", subtitle = "${alerts.size} active")
        Spacer(modifier = Modifier.height(4.dp))
        alerts.forEach { alert ->
            SkyCastAlertCard(alert = alert, onClick = { onAlertClick(alert) })
        }
    }
}

private fun severityFromLevel(level: String?): SkyAlertSeverity = when (level?.lowercase()) {
    "info" -> SkyAlertSeverity.Info
    "advisory" -> SkyAlertSeverity.Advisory
    "watch" -> SkyAlertSeverity.Watch
    "warning" -> SkyAlertSeverity.Warning
    "emergency", "extreme" -> SkyAlertSeverity.Emergency
    else -> SkyAlertSeverity.Advisory
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
}
