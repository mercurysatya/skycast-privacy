package com.vayu.weather.presentation.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.graphics.Color

@Composable
fun AlertsScreen(
    state: AlertsState,
    onDeleteAlert: (WeatherAlert) -> Unit,
    onClearAll: () -> Unit,
    onFilterChange: (SeverityFilter) -> Unit,
    onWeatherTypeFilterChange: (WeatherTypeFilter) -> Unit,
    onSnoozeAlert: (WeatherAlert, Long) -> Unit,
    onSnoozeDurationChange: (Int) -> Unit,
    onToggleExpand: (Long) -> Unit,
    onBack: () -> Unit = {},
    /**
     * Shows the full alert history (including advisories that are over)
     * instead of only the current ones. The "Alert History" entry in the
     * settings opens the screen in this mode.
     */
    showHistory: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    // The screen shows the current advisories; the history view shows every
    // stored advisory. Both the header count and the filter chips describe
    // exactly the list that is on screen.
    val visibleAlerts = remember(state.alerts, showHistory) {
        state.visibleAlerts(showHistory)
    }
    val filteredAlerts = remember(visibleAlerts, state.severityFilter, state.weatherTypeFilter, state.snoozedAlertIds) {
        state.filtered(visibleAlerts)
    }
    val highCount = visibleAlerts.count { it.severity == "high" }
    val mediumCount = visibleAlerts.count { it.severity == "medium" }
    val lowCount = visibleAlerts.count { it.severity == "low" }

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
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.weather_alerts),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (visibleAlerts.isNotEmpty()) {
                    Text(
                        text = if (visibleAlerts.size == 1) {
                            stringResource(R.string.alert_count_one, visibleAlerts.size)
                        } else {
                            stringResource(R.string.alert_count, visibleAlerts.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            if (visibleAlerts.isNotEmpty()) {
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
        if (visibleAlerts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.ALL,
                    onClick = { onFilterChange(SeverityFilter.ALL) },
                    label = { Text("${stringResource(R.string.filter_all)} (${visibleAlerts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.HIGH,
                    onClick = { onFilterChange(SeverityFilter.HIGH) },
                    label = { Text("${stringResource(R.string.severity_high)} ($highCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.severityFilter == SeverityFilter.MEDIUM,
                    onClick = { onFilterChange(SeverityFilter.MEDIUM) },
                    label = { Text("${stringResource(R.string.severity_medium)} ($mediumCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                )
            }
        }

        // Weather type filter chips
        if (visibleAlerts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.ALL,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.ALL) },
                    label = { Text("All Types") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.RAIN,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.RAIN) },
                    label = { Text("Rain (${visibleAlerts.count { it.title.contains("Rain", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E88E5).copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.WIND,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.WIND) },
                    label = { Text("Wind (${visibleAlerts.count { it.title.contains("Wind", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF78909C).copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.UV,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.UV) },
                    label = { Text("UV (${visibleAlerts.count { it.title.contains("UV", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.HEAT,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.HEAT) },
                    label = { Text("Heat (${visibleAlerts.count { it.title.contains("Heat", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.15f)
                    )
                )
                FilterChip(
                    selected = state.weatherTypeFilter == WeatherTypeFilter.COLD,
                    onClick = { onWeatherTypeFilterChange(WeatherTypeFilter.COLD) },
                    label = { Text("Cold (${visibleAlerts.count { it.title.contains("Cold", ignoreCase = true) }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4FC3F7).copy(alpha = 0.15f)
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
        } else if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = stringResource(R.string.no_alerts),
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
            val groupedAlerts = remember(filteredAlerts) {
                groupAlertsByDate(filteredAlerts)
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

                    items(alerts, key = { it.id }, contentType = { "alertCard" }) { alert ->
                        AlertCard(
                            alert = alert,
                            isExpanded = state.expandedAlertId == alert.id,
                            onToggleExpand = { onToggleExpand(alert.id) },
                            onDelete = { onDeleteAlert(alert) },
                            onSnoozeAlert = onSnoozeAlert,
                            onSnoozeDurationChange = onSnoozeDurationChange,
                            snoozedAlertIds = state.snoozedAlertIds,
                            snoozeDurationMinutes = state.snoozeDurationMinutes
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
    onDelete: () -> Unit,
    onSnoozeAlert: (WeatherAlert, Long) -> Unit,
    onSnoozeDurationChange: (Int) -> Unit,
    snoozedAlertIds: Set<Long> = emptySet(),
    snoozeDurationMinutes: Int = 30
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Severity glow border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                severityColor.copy(alpha = 0.8f),
                                severityColor.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
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
                    contentDescription = alert.severity,
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
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
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

                    // Snooze controls
                    if (!snoozedAlertIds.contains(alert.id)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.alert_snooze, snoozeDurationMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Editable minutes field (5-1440). Digits-only with a
                            // crash-safe parse: an empty/partial input just keeps
                            // the previous value instead of throwing.
                            var minutesText by remember(alert.id) {
                                mutableStateOf(snoozeDurationMinutes.toString())
                            }
                            val displayMinutes = minutesText.toIntOrNull()
                                ?: snoozeDurationMinutes
                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(4)
                                    minutesText = digits
                                    digits.toIntOrNull()?.let { minutes ->
                                        if (minutes > 0) {
                                            onSnoozeDurationChange(minutes * 60_000)
                                        }
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    onSnoozeAlert(
                                        alert,
                                        displayMinutes.coerceIn(5, 1440).toLong() * 60_000
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(stringResource(R.string.snooze))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

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
