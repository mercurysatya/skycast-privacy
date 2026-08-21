package com.vayu.weather.presentation.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vayu.weather.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.repository.WeatherAlert
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AlertsScreen(
    state: AlertsState,
    onDeleteAlert: (WeatherAlert) -> Unit,
    onClearAll: () -> Unit,
    onFilterChange: (SeverityFilter) -> Unit,
    onToggleExpand: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_all_alerts)) },
            text = { Text(stringResource(R.string.clear_all_alerts_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.weather_alerts),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state.alerts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.alert_count, state.alerts.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            if (state.alerts.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = stringResource(R.string.clear_all),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Severity filter chips
        if (state.alerts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.ALL,
                    onClick = { onFilterChange(SeverityFilter.ALL) },
                    label = { Text("${stringResource(R.string.filter_all)} (${state.alerts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.HIGH,
                    onClick = { onFilterChange(SeverityFilter.HIGH) },
                    label = { Text("${stringResource(R.string.severity_high)} (${state.highCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.MEDIUM,
                    onClick = { onFilterChange(SeverityFilter.MEDIUM) },
                    label = { Text("${stringResource(R.string.severity_medium)} (${state.mediumCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (state.severityFilter == SeverityFilter.ALL)
                            stringResource(R.string.no_alerts)
                        else
                            stringResource(R.string.no_matching_alerts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.severityFilter == SeverityFilter.ALL)
                            stringResource(R.string.no_alerts_subtitle)
                        else
                            stringResource(R.string.no_matching_alerts_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Group alerts by date
            val groupedAlerts = remember(state.filteredAlerts) {
                groupAlertsByDate(state.filteredAlerts)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedAlerts.forEach { (dateLabel, alerts) ->
                    item(key = "header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            isExpanded = state.expandedAlertId == alert.id,
                            onToggleExpand = { onToggleExpand(alert.id) },
                            onDelete = { onDeleteAlert(alert) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: WeatherAlert,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val severityColor = when (alert.severity) {
        "high" -> MaterialTheme.colorScheme.error
        "medium" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val timeText = remember(alert.timestamp) {
        try {
            Instant.ofEpochMilli(alert.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onToggleExpand)
                .padding(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (alert.severity == "high")
                        Icons.Rounded.Warning else Icons.Rounded.PriorityHigh,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val severityLabel = when (alert.severity) {
                            "high" -> stringResource(R.string.severity_high)
                            "medium" -> stringResource(R.string.severity_medium)
                            else -> stringResource(R.string.severity_low)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = severityColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = severityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = severityColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        if (timeText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        alert.cityName?.let { city ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "· $city",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Message
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full timestamp
                    val fullTime = remember(alert.timestamp) {
                        try {
                            Instant.ofEpochMilli(alert.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy 'at' HH:mm"))
                        } catch (e: Exception) { "" }
                    }
                    if (fullTime.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.alert_time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fullTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Coordinates
                    if (alert.latitude != null && alert.longitude != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.alert_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.4f, %.4f", alert.latitude, alert.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete button
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

private fun groupAlertsByDate(alerts: List<WeatherAlert>): List<Pair<String, List<WeatherAlert>>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val weekAgo = today.minusDays(7)

    val groups = mutableMapOf<String, MutableList<WeatherAlert>>()

    for (alert in alerts) {
        val date = try {
            Instant.ofEpochMilli(alert.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) { today }

        val label = when {
            date == today -> "Today"
            date == yesterday -> "Yesterday"
            date.isAfter(weekAgo) -> date.format(DateTimeFormatter.ofPattern("EEEE"))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }

        groups.getOrPut(label) { mutableListOf() }.add(alert)
    }

    return groups.entries.map { (label, items) -> label to items }
}
