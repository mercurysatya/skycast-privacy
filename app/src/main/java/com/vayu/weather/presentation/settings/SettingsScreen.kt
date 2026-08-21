package com.vayu.weather.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.vayu.weather.BuildConfig
import androidx.compose.ui.res.stringResource
import com.vayu.weather.BuildConfig
import com.vayu.weather.R
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WindUnit
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsState,
    onToggleUnit: () -> Unit,
    onWindUnitChange: (WindUnit) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRainAlertThresholdChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenPrivacyPolicy: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val wrappedToggleUnit: () -> Unit = {
        snackbarMessage = if (state.temperatureUnit == TemperatureUnit.CELSIUS) context.getString(R.string.switched_to_fahrenheit) else context.getString(R.string.switched_to_celsius)
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
        snackbarMessage = if (value) context.getString(R.string.dynamic_colors_enabled) else context.getString(R.string.dynamic_colors_disabled)
        onToggleDynamicColor(value)
    }
    val wrappedToggleNotifications: (Boolean) -> Unit = { value ->
        snackbarMessage = if (value) context.getString(R.string.notifications_enabled_toast) else context.getString(R.string.notifications_disabled_toast)
        onToggleNotifications(value)
    }
    val wrappedRainAlertThresholdChange: (Int) -> Unit = { value ->
        snackbarMessage = "Rain alert threshold: $value%"
        onRainAlertThresholdChange(value)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(48.dp).padding(12.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SettingsGroup(title = stringResource(R.string.units)) {
                SettingsRow(
                    icon = Icons.Rounded.Thermostat,
                    title = stringResource(R.string.temperature),
                    subtitle = if (state.temperatureUnit == TemperatureUnit.CELSIUS) stringResource(R.string.celsius_full) else stringResource(R.string.fahrenheit_full)
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
                        TextButton(onClick = { expanded = true }) {                                Text(
                                    text = when (state.windUnit) {
                                        WindUnit.KPH -> stringResource(R.string.wind_kph)
                                        WindUnit.MPH -> stringResource(R.string.wind_mph)
                                        WindUnit.MS -> stringResource(R.string.wind_ms)
                                        WindUnit.KNOTS -> stringResource(R.string.wind_knots)
                                    },
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            WindUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                    text = when (unit) {
                                        WindUnit.KPH -> stringResource(R.string.wind_kph)
                                        WindUnit.MPH -> stringResource(R.string.wind_mph)
                                        WindUnit.MS -> stringResource(R.string.wind_ms)
                                        WindUnit.KNOTS -> stringResource(R.string.wind_knots)
                                    }
                                        )
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

            Spacer(modifier = Modifier.height(24.dp))                SettingsGroup(title = stringResource(R.string.appearance)) {
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
                            Text(
                                text = when (state.themeMode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                }
                            )
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

            Spacer(modifier = Modifier.height(24.dp))                SettingsGroup(title = stringResource(R.string.notifications_group)) {
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Rain alert threshold: ${state.rainAlertThreshold}%",
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))                SettingsGroup(title = stringResource(R.string.about)) {
                val context = LocalContext.current
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.powered_by_open_meteo)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.RateReview,
                    title = stringResource(R.string.rate_app),
                    subtitle = stringResource(R.string.rate_app_subtitle)
                ) {
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(intent)
                        }
                    }) {
                        Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.view_privacy_policy)
                ) {
                    TextButton(onClick = { onOpenPrivacyPolicy(null) }) {
                        Text(stringResource(R.string.open), color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = stringResource(R.string.delete_my_data),
                    subtitle = stringResource(R.string.delete_my_data_subtitle)
                ) {
                    TextButton(onClick = { onOpenPrivacyPolicy("data-deletion") }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

