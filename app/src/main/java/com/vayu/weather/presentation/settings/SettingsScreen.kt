package com.vayu.weather.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.systemBarsPadding
import com.vayu.weather.BuildConfig
import com.vayu.weather.R
import com.vayu.weather.presentation.weather.AlertSeverity
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WidgetSize
import com.vayu.weather.presentation.weather.WindUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onToggleUnit: () -> Unit,
    onWindUnitChange: (WindUnit) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRainAlertThresholdChange: (Int) -> Unit,
    onCheckIntervalChange: (Int) -> Unit = {},
    onSeverityFilterChange: (AlertSeverity) -> Unit = {},
    onWidgetSizeChange: (WidgetSize) -> Unit = {},
    onWindAlertThresholdChange: (Int) -> Unit = {},
    onEnableWindAlertsChange: (Boolean) -> Unit = {},
    onUvAlertThresholdChange: (Int) -> Unit = {},
    onEnableUvAlertsChange: (Boolean) -> Unit = {},
    onHeatAlertThresholdChange: (Int) -> Unit = {},
    onEnableHeatAlertsChange: (Boolean) -> Unit = {},
    onColdAlertThresholdChange: (Int) -> Unit = {},
    onEnableColdAlertsChange: (Boolean) -> Unit = {},
    onUse24hClockChange: (Boolean) -> Unit = {},
    onPressureUnitChange: (String) -> Unit = {},
    onPrecipitationUnitChange: (String) -> Unit = {},
    onSectionVisibilityChange: (String, Boolean) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onOpenPrivacyPolicy: (String?) -> Unit = {},
    onDeleteAllData: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
    // Quiet hours
    onQuietHoursEnabledChange: (Boolean) -> Unit = {},
    onQuietHoursStartHourChange: (Int) -> Unit = {},
    onQuietHoursStartMinuteChange: (Int) -> Unit = {},
    onQuietHoursEndHourChange: (Int) -> Unit = {},
    onQuietHoursEndMinuteChange: (Int) -> Unit = {},
    // Per-day notification times
    onNotificationTime1EnabledChange: (Boolean) -> Unit = {},
    onNotificationTime1HourChange: (Int) -> Unit = {},
    onNotificationTime1MinuteChange: (Int) -> Unit = {},
    onNotificationTime2EnabledChange: (Boolean) -> Unit = {},
    onNotificationTime2HourChange: (Int) -> Unit = {},
    onNotificationTime2MinuteChange: (Int) -> Unit = {},
    onNotificationTime3EnabledChange: (Boolean) -> Unit = {},
    onNotificationTime3HourChange: (Int) -> Unit = {},
    onNotificationTime3MinuteChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val wrappedToggleUnit: () -> Unit = {
        snackbarMessage = if (state.temperatureUnit == TemperatureUnit.CELSIUS)
            context.getString(R.string.switched_to_fahrenheit)
        else
            context.getString(R.string.switched_to_celsius)
        onToggleUnit()
    }

    val wrappedWindUnitChange: (WindUnit) -> Unit = { unit ->
        snackbarMessage = when (unit) {
            WindUnit.KPH -> context.getString(R.string.wind_speed_kph)
            WindUnit.MPH -> context.getString(R.string.wind_speed_mph)
            WindUnit.MS -> context.getString(R.string.wind_speed_ms)
            WindUnit.KNOTS -> context.getString(R.string.wind_speed_knots)
        }
        onWindUnitChange(unit)
    }

    val wrappedThemeModeChange: (ThemeMode) -> Unit = { mode ->
        snackbarMessage = when (mode) {
            ThemeMode.SYSTEM -> context.getString(R.string.dark_mode_system)
            ThemeMode.LIGHT -> context.getString(R.string.dark_mode_off)
            ThemeMode.DARK -> context.getString(R.string.dark_mode_on)
        }
        onThemeModeChange(mode)
    }

    val wrappedToggleDynamicColor: (Boolean) -> Unit = { value ->
        snackbarMessage = if (value) context.getString(R.string.dynamic_colors_enabled)
        else context.getString(R.string.dynamic_colors_disabled)
        onToggleDynamicColor(value)
    }

    val wrappedToggleNotifications: (Boolean) -> Unit = { value ->
        snackbarMessage = if (value) context.getString(R.string.notifications_enabled_toast)
        else context.getString(R.string.notifications_disabled_toast)
        onToggleNotifications(value)
    }

    val wrappedRainAlertThresholdChange: (Int) -> Unit = { value ->
        snackbarMessage = context.getString(R.string.rain_threshold_toast, value)
        onRainAlertThresholdChange(value)
    }

    val wrappedCheckIntervalChange: (Int) -> Unit = { value ->
        val label = when (value) {
            1 -> context.getString(R.string.check_interval_1h)
            2 -> context.getString(R.string.check_interval_2h)
            6 -> context.getString(R.string.check_interval_6h)
            12 -> context.getString(R.string.check_interval_12h)
            else -> context.getString(R.string.check_interval_3h)
        }
        snackbarMessage = context.getString(R.string.check_interval_toast, label)
        onCheckIntervalChange(value)
    }

    val wrappedSeverityFilterChange: (AlertSeverity) -> Unit = { value ->
        val label = when (value) {
            AlertSeverity.ALL -> context.getString(R.string.severity_all)
            AlertSeverity.HIGH -> context.getString(R.string.severity_high_only)
            AlertSeverity.HIGH_MEDIUM -> context.getString(R.string.severity_high_and_medium)
        }
        snackbarMessage = context.getString(R.string.severity_filter_toast, label)
        onSeverityFilterChange(value)
    }

    val wrappedWidgetSizeChange: (WidgetSize) -> Unit = { value ->
        val label = when (value) {
            WidgetSize.SMALL -> context.getString(R.string.widget_size_small)
            WidgetSize.MEDIUM -> context.getString(R.string.widget_size_medium)
            WidgetSize.LARGE -> context.getString(R.string.widget_size_large)
        }
        snackbarMessage = context.getString(R.string.widget_size_toast, label)
        onWidgetSizeChange(value)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // === UNITS ===
                SettingsGroup(title = stringResource(R.string.units)) {
                    SettingsRow(
                        icon = Icons.Rounded.Thermostat,
                        title = stringResource(R.string.temperature),
                        subtitle = if (state.temperatureUnit == TemperatureUnit.CELSIUS)
                            stringResource(R.string.celsius_full) else stringResource(R.string.fahrenheit_full)
                    ) {
                        Switch(
                            checked = state.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                            onCheckedChange = { wrappedToggleUnit() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Wind speed dropdown
                    SettingsRow(
                        icon = Icons.Rounded.Air,
                        title = stringResource(R.string.wind_speed),
                        subtitle = when (state.windUnit) {
                            WindUnit.KPH -> stringResource(R.string.wind_kph)
                            WindUnit.MPH -> stringResource(R.string.wind_mph)
                            WindUnit.MS -> stringResource(R.string.wind_ms)
                            WindUnit.KNOTS -> stringResource(R.string.wind_knots)
                        }
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text = when (state.windUnit) {
                                        WindUnit.KPH -> stringResource(R.string.wind_kph)
                                        WindUnit.MPH -> stringResource(R.string.wind_mph)
                                        WindUnit.MS -> stringResource(R.string.wind_ms)
                                        WindUnit.KNOTS -> stringResource(R.string.wind_knots)
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                WindUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = when (unit) {
                                                WindUnit.KPH -> stringResource(R.string.wind_kph)
                                                WindUnit.MPH -> stringResource(R.string.wind_mph)
                                                WindUnit.MS -> stringResource(R.string.wind_ms)
                                                WindUnit.KNOTS -> stringResource(R.string.wind_knots)
                                            })
                                        },
                                        onClick = {
                                            wrappedWindUnitChange(unit)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === APPEARANCE ===
                SettingsGroup(title = stringResource(R.string.appearance)) {
                    SettingsRow(
                        icon = Icons.Rounded.DarkMode,
                        title = stringResource(R.string.dark_mode),
                        subtitle = when (state.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.follow_system)
                            ThemeMode.LIGHT -> stringResource(R.string.always_off)
                            ThemeMode.DARK -> stringResource(R.string.always_on)
                        }
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(text = when (state.themeMode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.follow_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.light_mode)
                                    ThemeMode.DARK -> stringResource(R.string.dark_mode_option)
                                })
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                ThemeMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (mode) {
                                                    ThemeMode.SYSTEM -> stringResource(R.string.follow_system)
                                                    ThemeMode.LIGHT -> stringResource(R.string.light_mode)
                                                    ThemeMode.DARK -> stringResource(R.string.dark_mode_option)
                                                },
                                                fontWeight = if (mode == state.themeMode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            wrappedThemeModeChange(mode)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.material_you),
                        subtitle = if (state.useDynamicColor) stringResource(R.string.dynamic_colors_enabled) else stringResource(R.string.custom_colors)
                    ) {
                        Switch(
                            checked = state.useDynamicColor,
                            onCheckedChange = { wrappedToggleDynamicColor(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === NOTIFICATIONS ===
                SettingsGroup(title = stringResource(R.string.notifications_group)) {
                    SettingsRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.weather_alerts_setting),
                        subtitle = if (state.notificationsEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = { wrappedToggleNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (state.notificationsEnabled) {
                        // Rain alert threshold
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = context.getString(R.string.rain_threshold_toast, state.rainAlertThreshold),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = state.rainAlertThreshold.toFloat(),
                                onValueChange = { wrappedRainAlertThresholdChange(it.roundToInt()) },
                                valueRange = 10f..100f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Check frequency
                        SettingsRow(
                            icon = Icons.Rounded.Schedule,
                            title = stringResource(R.string.check_interval),
                            subtitle = when (state.checkIntervalHours) {
                                1 -> stringResource(R.string.check_interval_1h)
                                2 -> stringResource(R.string.check_interval_2h)
                                6 -> stringResource(R.string.check_interval_6h)
                                12 -> stringResource(R.string.check_interval_12h)
                                else -> stringResource(R.string.check_interval_3h)
                            }
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(
                                        text = when (state.checkIntervalHours) {
                                            1 -> stringResource(R.string.check_interval_1h)
                                            2 -> stringResource(R.string.check_interval_2h)
                                            6 -> stringResource(R.string.check_interval_6h)
                                            12 -> stringResource(R.string.check_interval_12h)
                                            else -> stringResource(R.string.check_interval_3h)
                                        },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf(1, 2, 3, 6, 12).forEach { hours ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (hours) {
                                                        1 -> stringResource(R.string.check_interval_1h)
                                                        2 -> stringResource(R.string.check_interval_2h)
                                                        6 -> stringResource(R.string.check_interval_6h)
                                                        12 -> stringResource(R.string.check_interval_12h)
                                                        else -> stringResource(R.string.check_interval_3h)
                                                    },
                                                    fontWeight = if (hours == state.checkIntervalHours) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                wrappedCheckIntervalChange(hours)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Severity filter
                        SettingsRow(
                            icon = Icons.Rounded.PriorityHigh,
                            title = stringResource(R.string.severity_filter),
                            subtitle = when (state.severityFilter) {
                                AlertSeverity.ALL -> stringResource(R.string.severity_all)
                                AlertSeverity.HIGH -> stringResource(R.string.severity_high_only)
                                AlertSeverity.HIGH_MEDIUM -> stringResource(R.string.severity_high_and_medium)
                            }
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(
                                        text = when (state.severityFilter) {
                                            AlertSeverity.ALL -> stringResource(R.string.severity_all)
                                            AlertSeverity.HIGH -> stringResource(R.string.severity_high_only)
                                            AlertSeverity.HIGH_MEDIUM -> stringResource(R.string.severity_high_and_medium)
                                        },
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    AlertSeverity.entries.forEach { severity ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (severity) {
                                                        AlertSeverity.ALL -> stringResource(R.string.severity_all)
                                                        AlertSeverity.HIGH -> stringResource(R.string.severity_high_only)
                                                        AlertSeverity.HIGH_MEDIUM -> stringResource(R.string.severity_high_and_medium)
                                                    },
                                                    fontWeight = if (severity == state.severityFilter) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                wrappedSeverityFilterChange(severity)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === SEVERE WEATHER ALERTS ===
                SettingsGroup(title = stringResource(R.string.severe_alerts_group)) {
                    // Wind Alerts
                    SettingsRow(
                        icon = Icons.Rounded.Air,
                        title = stringResource(R.string.wind_alerts),
                        subtitle = if (state.enableWindAlerts)
                            stringResource(R.string.wind_threshold, state.windAlertThreshold)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.enableWindAlerts,
                            onCheckedChange = { onEnableWindAlertsChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.enableWindAlerts) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.wind_threshold, state.windAlertThreshold),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = state.windAlertThreshold.toFloat(),
                                onValueChange = { onWindAlertThresholdChange(it.toInt()) },
                                valueRange = 20f..120f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("20", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("120", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // UV Alerts
                    SettingsRow(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.uv_alerts),
                        subtitle = if (state.enableUvAlerts)
                            stringResource(R.string.uv_threshold, state.uvAlertThreshold)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.enableUvAlerts,
                            onCheckedChange = { onEnableUvAlertsChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.enableUvAlerts) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.uv_threshold, state.uvAlertThreshold),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = state.uvAlertThreshold.toFloat(),
                                onValueChange = { onUvAlertThresholdChange(it.toInt()) },
                                valueRange = 3f..12f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("12", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Heat Alerts
                    SettingsRow(
                        icon = Icons.Rounded.Thermostat,
                        title = stringResource(R.string.heat_alerts),
                        subtitle = if (state.enableHeatAlerts)
                            stringResource(R.string.heat_threshold, state.heatAlertThreshold)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.enableHeatAlerts,
                            onCheckedChange = { onEnableHeatAlertsChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.enableHeatAlerts) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.heat_threshold, state.heatAlertThreshold),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = state.heatAlertThreshold.toFloat(),
                                onValueChange = { onHeatAlertThresholdChange(it.toInt()) },
                                valueRange = 30f..50f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("30°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("50°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Cold Alerts
                    SettingsRow(
                        icon = Icons.Rounded.AcUnit,
                        title = stringResource(R.string.cold_alerts),
                        subtitle = if (state.enableColdAlerts)
                            stringResource(R.string.cold_threshold, state.coldAlertThreshold)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.enableColdAlerts,
                            onCheckedChange = { onEnableColdAlertsChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.enableColdAlerts) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.cold_threshold, state.coldAlertThreshold),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = state.coldAlertThreshold.toFloat(),
                                onValueChange = { onColdAlertThresholdChange(it.toInt()) },
                                valueRange = -10f..10f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("-10°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("10°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === QUIET HOURS & NOTIFICATION SCHEDULE ===
                SettingsGroup(title = stringResource(R.string.quiet_hours_group)) {
                    // Quiet hours enabled
                    SettingsRow(
                        icon = Icons.Rounded.Bedtime,
                        title = stringResource(R.string.quiet_hours),
                        subtitle = if (state.quietHoursEnabled)
                            stringResource(R.string.quiet_hours_enabled, state.quietHoursStartHour, state.quietHoursStartMinute, state.quietHoursEndHour, state.quietHoursEndMinute)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.quietHoursEnabled,
                            onCheckedChange = { onQuietHoursEnabledChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (state.quietHoursEnabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            // Start time
                            SettingsRow(
                                icon = Icons.Rounded.Schedule,
                                title = stringResource(R.string.quiet_hours_start),
                                subtitle = String.format("%02d:%02d", state.quietHoursStartHour, state.quietHoursStartMinute)
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", state.quietHoursStartHour, state.quietHoursStartMinute),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (0..23).forEach { hour ->
                                            (0..59 step 15).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(text = String.format("%02d:%02d", hour, minute))
                                                    },
                                                    onClick = {
                                                        onQuietHoursStartHourChange(hour)
                                                        onQuietHoursStartMinuteChange(minute)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // End time
                            SettingsRow(
                                icon = Icons.Rounded.WbSunny,
                                title = stringResource(R.string.quiet_hours_end),
                                subtitle = String.format("%02d:%02d", state.quietHoursEndHour, state.quietHoursEndMinute)
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", state.quietHoursEndHour, state.quietHoursEndMinute),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (0..23).forEach { hour ->
                                            (0..59 step 15).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(text = String.format("%02d:%02d", hour, minute))
                                                    },
                                                    onClick = {
                                                        onQuietHoursEndHourChange(hour)
                                                        onQuietHoursEndMinuteChange(minute)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Per-day notification times
                    Text(
                        text = stringResource(R.string.notification_times),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                    )

                    // Time 1 (Morning)
                    SettingsRow(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.notification_time_1),
                        subtitle = if (state.notificationTime1Enabled)
                            String.format("%02d:%02d", state.notificationTime1Hour, state.notificationTime1Minute)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.notificationTime1Enabled,
                            onCheckedChange = { onNotificationTime1EnabledChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.notificationTime1Enabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SettingsRow(
                                icon = Icons.Rounded.Schedule,
                                title = stringResource(R.string.notification_time_1_time),
                                subtitle = String.format("%02d:%02d", state.notificationTime1Hour, state.notificationTime1Minute)
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", state.notificationTime1Hour, state.notificationTime1Minute),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (0..23).forEach { hour ->
                                            (0..59 step 15).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = { Text(text = String.format("%02d:%02d", hour, minute)) },
                                                    onClick = {
                                                        onNotificationTime1HourChange(hour)
                                                        onNotificationTime1MinuteChange(minute)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Time 2 (Midday)
                    SettingsRow(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.notification_time_2),
                        subtitle = if (state.notificationTime2Enabled)
                            String.format("%02d:%02d", state.notificationTime2Hour, state.notificationTime2Minute)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.notificationTime2Enabled,
                            onCheckedChange = { onNotificationTime2EnabledChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.notificationTime2Enabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SettingsRow(
                                icon = Icons.Rounded.Schedule,
                                title = stringResource(R.string.notification_time_2_time),
                                subtitle = String.format("%02d:%02d", state.notificationTime2Hour, state.notificationTime2Minute)
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", state.notificationTime2Hour, state.notificationTime2Minute),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (0..23).forEach { hour ->
                                            (0..59 step 15).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = { Text(text = String.format("%02d:%02d", hour, minute)) },
                                                    onClick = {
                                                        onNotificationTime2HourChange(hour)
                                                        onNotificationTime2MinuteChange(minute)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Time 3 (Evening)
                    SettingsRow(
                        icon = Icons.Rounded.NightsStay,
                        title = stringResource(R.string.notification_time_3),
                        subtitle = if (state.notificationTime3Enabled)
                            String.format("%02d:%02d", state.notificationTime3Hour, state.notificationTime3Minute)
                        else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.notificationTime3Enabled,
                            onCheckedChange = { onNotificationTime3EnabledChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                    if (state.notificationTime3Enabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SettingsRow(
                                icon = Icons.Rounded.Schedule,
                                title = stringResource(R.string.notification_time_3_time),
                                subtitle = String.format("%02d:%02d", state.notificationTime3Hour, state.notificationTime3Minute)
                            ) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", state.notificationTime3Hour, state.notificationTime3Minute),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        (0..23).forEach { hour ->
                                            (0..59 step 15).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = { Text(text = String.format("%02d:%02d", hour, minute)) },
                                                    onClick = {
                                                        onNotificationTime3HourChange(hour)
                                                        onNotificationTime3MinuteChange(minute)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === WIDGET ===
                SettingsGroup(title = stringResource(R.string.widget_group)) {
                    SettingsRow(
                        icon = Icons.Rounded.Widgets,
                        title = stringResource(R.string.widget_size),
                        subtitle = when (state.widgetSize) {
                            WidgetSize.SMALL -> stringResource(R.string.widget_size_small)
                            WidgetSize.MEDIUM -> stringResource(R.string.widget_size_medium)
                            WidgetSize.LARGE -> stringResource(R.string.widget_size_large)
                        }
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text = when (state.widgetSize) {
                                        WidgetSize.SMALL -> stringResource(R.string.widget_size_small)
                                        WidgetSize.MEDIUM -> stringResource(R.string.widget_size_medium)
                                        WidgetSize.LARGE -> stringResource(R.string.widget_size_large)
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                WidgetSize.entries.forEach { size ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (size) {
                                                    WidgetSize.SMALL -> stringResource(R.string.widget_size_small)
                                                    WidgetSize.MEDIUM -> stringResource(R.string.widget_size_medium)
                                                    WidgetSize.LARGE -> stringResource(R.string.widget_size_large)
                                                },
                                                fontWeight = if (size == state.widgetSize) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            wrappedWidgetSizeChange(size)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = when (state.widgetSize) {
                                WidgetSize.SMALL -> stringResource(R.string.widget_size_small_desc)
                                WidgetSize.MEDIUM -> stringResource(R.string.widget_size_medium_desc)
                                WidgetSize.LARGE -> stringResource(R.string.widget_size_large_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === ADDITIONAL UNITS ===
                SettingsGroup(title = stringResource(R.string.units)) {
                    // Clock Format
                    SettingsRow(
                        icon = Icons.Rounded.Schedule,
                        title = stringResource(R.string.clock_format),
                        subtitle = if (state.use24hClock)
                            stringResource(R.string.clock_24h)
                        else stringResource(R.string.clock_12h)
                    ) {
                        Switch(
                            checked = state.use24hClock,
                            onCheckedChange = { onUse24hClockChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Pressure Unit
                    SettingsRow(
                        icon = Icons.Rounded.Compress,
                        title = stringResource(R.string.pressure_unit),
                        subtitle = if (state.pressureUnit == "inHg") stringResource(R.string.pressure_inhg) else stringResource(R.string.pressure_hpa)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text = if (state.pressureUnit == "inHg") stringResource(R.string.pressure_inhg) else stringResource(R.string.pressure_hpa),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("hPa" to stringResource(R.string.pressure_hpa), "inHg" to stringResource(R.string.pressure_inhg)).forEach { (unit, label) ->
                                    DropdownMenuItem(
                                        text = { Text(text = label, fontWeight = if (unit == state.pressureUnit) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            snackbarMessage = context.getString(R.string.pressure_unit_toast, label)
                                            onPressureUnitChange(unit)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Precipitation Unit
                    SettingsRow(
                        icon = Icons.Rounded.WaterDrop,
                        title = stringResource(R.string.precipitation_unit),
                        subtitle = if (state.precipitationUnit == "inches") stringResource(R.string.precip_inches) else stringResource(R.string.precip_mm)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(
                                    text = if (state.precipitationUnit == "inches") stringResource(R.string.precip_inches) else stringResource(R.string.precip_mm),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("mm" to stringResource(R.string.precip_mm), "inches" to stringResource(R.string.precip_inches)).forEach { (unit, label) ->
                                    DropdownMenuItem(
                                        text = { Text(text = label, fontWeight = if (unit == state.precipitationUnit) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            snackbarMessage = context.getString(R.string.precipitation_unit_toast, label)
                                            onPrecipitationUnitChange(unit)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === SECTION VISIBILITY ===
                SettingsGroup(title = stringResource(R.string.section_visibility)) {
                    SettingsRow(
                        icon = Icons.Rounded.WbSunny,
                        title = stringResource(R.string.show_hourly_forecast),
                        subtitle = if (state.showHourlyForecast) stringResource(R.string.enabled) else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.showHourlyForecast,
                            onCheckedChange = { onSectionVisibilityChange("hourly", it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.NightsStay,
                        title = stringResource(R.string.show_sun_moon),
                        subtitle = if (state.showSunMoon) stringResource(R.string.enabled) else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.showSunMoon,
                            onCheckedChange = { onSectionVisibilityChange("sun_moon", it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.Air,
                        title = stringResource(R.string.show_air_quality),
                        subtitle = if (state.showAirQuality) stringResource(R.string.enabled) else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.showAirQuality,
                            onCheckedChange = { onSectionVisibilityChange("air_quality", it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.show_weather_details),
                        subtitle = if (state.showWeatherDetails) stringResource(R.string.enabled) else stringResource(R.string.disabled)
                    ) {
                        Switch(
                            checked = state.showWeatherDetails,
                            onCheckedChange = { onSectionVisibilityChange("weather_details", it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === ABOUT ===
                SettingsGroup(title = stringResource(R.string.about)) {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.version),
                        subtitle = BuildConfig.VERSION_NAME
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(R.string.powered_by_open_meteo)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.RateReview,
                        title = stringResource(R.string.rate_app),
                        subtitle = stringResource(R.string.rate_app_subtitle)
                    ) {
                        TextButton(onClick = {
                            val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
                                } catch (e2: Exception) {
                                    // No browser available - nothing else we can do
                                }
                            }
                        }) {
                            Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.view_privacy_policy)
                    ) {
                        TextButton(onClick = { onOpenPrivacyPolicy(null) }) {
                            Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // GDPR/UMP privacy options entry point - required for EEA/UK users
                    val showPrivacyOptions = remember {
                        com.vayu.weather.presentation.ConsentManager.isPrivacyOptionsRequired(context)
                    }
                    if (showPrivacyOptions) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsRow(
                            icon = Icons.Rounded.ManageAccounts,
                            title = stringResource(R.string.privacy_options),
                            subtitle = stringResource(R.string.privacy_options_subtitle)
                        ) {
                            TextButton(onClick = {
                                (context as? android.app.Activity)?.let { activity ->
                                    com.vayu.weather.presentation.ConsentManager.showPrivacyOptionsForm(activity) {
                                        snackbarMessage = context.getString(R.string.privacy_options_updated)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.alert_history),
                        subtitle = stringResource(R.string.alert_history_subtitle)
                    ) {
                        TextButton(onClick = onOpenAlerts) {
                            Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Delete My Data
                    SettingsRow(
                        icon = Icons.Rounded.DeleteForever,
                        title = stringResource(R.string.delete_my_data),
                        subtitle = stringResource(R.string.delete_my_data_subtitle)
                    ) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text(stringResource(R.string.delete_my_data)) },
                                text = { Text(stringResource(R.string.delete_all_data_confirm)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onDeleteAllData()
                                        showDeleteDialog = false
                                    }) {
                                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Clear Cache
                    SettingsRow(
                        icon = Icons.Rounded.CleaningServices,
                        title = stringResource(R.string.clear_cache),
                        subtitle = stringResource(R.string.clear_cache_subtitle)
                    ) {
                        var showCacheDialog by remember { mutableStateOf(false) }
                        if (showCacheDialog) {
                            AlertDialog(
                                onDismissRequest = { showCacheDialog = false },
                                title = { Text(stringResource(R.string.clear_cache)) },
                                text = { Text(stringResource(R.string.clear_cache_confirm)) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onClearCache()
                                        showCacheDialog = false
                                    }) {
                                        Text(stringResource(R.string.delete))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCacheDialog = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                        TextButton(onClick = { showCacheDialog = true }) {
                            Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Ad banner at bottom of settings
                Spacer(modifier = Modifier.height(16.dp))
                com.vayu.weather.presentation.ads.AdBanner()
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// === TOP-LEVEL HELPER FUNCTIONS ===

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
