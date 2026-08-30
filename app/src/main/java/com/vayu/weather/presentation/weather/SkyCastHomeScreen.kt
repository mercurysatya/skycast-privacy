package com.vayu.weather.presentation.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.components.skycast.SkyCastAqiCard
import com.vayu.weather.presentation.components.skycast.SkyCastAlertList
import com.vayu.weather.presentation.components.skycast.SkyCastBackground
import com.vayu.weather.presentation.components.skycast.filterAndSortAlerts
import com.vayu.weather.presentation.components.skycast.SkyCastCard
import com.vayu.weather.presentation.components.skycast.SkyCastDailyForecast
import com.vayu.weather.presentation.components.skycast.SkyCastErrorState
import com.vayu.weather.presentation.components.skycast.SkyCastHero
import com.vayu.weather.presentation.components.skycast.SkyCastHourlyTimeline
import com.vayu.weather.presentation.components.skycast.SkyCastLoadingState
import com.vayu.weather.presentation.components.skycast.SkyCastMetricCard
import com.vayu.weather.presentation.components.skycast.SkyCastOfflineBanner
import com.vayu.weather.presentation.components.skycast.SkyCastPrecipitationTimeline
import com.vayu.weather.presentation.components.skycast.SkyCastRainForecast
import com.vayu.weather.presentation.components.skycast.SkyCastSectionHeader
import com.vayu.weather.presentation.components.skycast.SkyCastSummaryCard
import com.vayu.weather.presentation.components.skycast.SkyCastSunMoonCard
import com.vayu.weather.presentation.components.skycast.SkyCastUvCard
import com.vayu.weather.presentation.components.skycast.SkyCastWindCompass
import com.vayu.weather.presentation.components.skycast.WeatherSummaryEngine
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastColors
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.WarmOrange
import com.vayu.weather.ui.theme.WarningAmber
import kotlin.math.roundToInt

/**
 * SkyCast premium home screen.
 *
 * Refined information hierarchy:
 *  1. Single location header (refresh / share / more menu)
 *  2. Hero — current temperature + condition + H/L + Feels
 *  3. Intelligent summary
 *  4. Precipitation timeline (rain chance)
 *  5. Hourly forecast
 *  6. 7-day forecast
 *  7. Severe alerts (prominent, sorted by severity, expired filtered)
 *  8. Compact details (humidity / wind / pressure / visibility / dew)
 *  9. UV
 * 10. Air quality
 * 11. Sun & moon
 * 12. Wind compass
 */
@Composable
fun SkyCastHomeScreen(
    state: WeatherState,
    settings: SettingsState,
    cityName: String?,
    regionName: String?,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: ThemeMode,
    onOpenMetricDetail: (String) -> Unit = {},
    isDetailedForecastUnlocked: Boolean = false,
    onWatchAdForDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val info = state.weatherInfo
    val isCelsius = settings.temperatureUnit == TemperatureUnit.CELSIUS
    val context = LocalContext.current
    val isLocationError = state.error?.contains("Location", ignoreCase = true) == true
    val missingPermission = isLocationError && ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            onRefresh()
        }
    }

    when {
        state.isLoading && info == null -> SkyCastLoadingState(modifier)
        info == null -> SkyCastErrorState(
            message = state.error ?: "Unable to update weather. Tap to retry.",
            onRetry = onRefresh,
            modifier = modifier,
            showGrantPermission = missingPermission,
            onGrantPermission = {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        )
        else -> SkyCastHomeContent(
            state = state,
            isCelsius = isCelsius,
            windUnit = settings.windUnit,
            cityName = cityName,
            regionName = regionName,
            onOpenSettings = onOpenSettings,
            onOpenAlerts = onOpenAlerts,
            onOpenHistory = onOpenHistory,
            onShare = onShare,
            onRefresh = onRefresh,
            onOpenDetail = onOpenDetail,
            onToggleUnit = onToggleUnit,
            onToggleTheme = onToggleTheme,
            themeMode = themeMode,
            onOpenMetricDetail = onOpenMetricDetail,
            isDetailedForecastUnlocked = isDetailedForecastUnlocked,
            onWatchAdForDetails = onWatchAdForDetails,
            modifier = modifier
        )
    }
}

@Composable
private fun SkyCastHomeContent(
    state: WeatherState,
    isCelsius: Boolean,
    windUnit: com.vayu.weather.presentation.weather.WindUnit,
    cityName: String?,
    regionName: String?,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: ThemeMode,
    onOpenMetricDetail: (String) -> Unit = {},
    isDetailedForecastUnlocked: Boolean = false,
    onWatchAdForDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val info = state.weatherInfo ?: return

    val activeAlerts = remember(state.alerts) { filterAndSortAlerts(state.alerts) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = SkyCastBackground.gradientFor(
                        info.current.weatherCode,
                        info.current.isDay
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Offline banner (above all else) ──
            if (state.refreshError != null) {
                item(key = "offline_banner") {
                    SkyCastOfflineBanner(
                        lastUpdatedLabel = state.lastUpdatedTime,
                        onRetry = onRefresh
                    )
                }
            }

            // ── Single, premium location header ──
            item(key = "header") {
                SkyCastLocationHeader(
                    cityName = cityName,
                    regionName = regionName,
                    updatedAtLabel = state.lastUpdatedTime,
                    alertCount = activeAlerts.size,
                    onRefresh = onRefresh,
                    onShare = onShare,
                    onOpenSettings = onOpenSettings,
                    onOpenAlerts = onOpenAlerts,
                    onOpenHistory = onOpenHistory,
                    onToggleUnit = onToggleUnit,
                    onToggleTheme = onToggleTheme,
                    isCelsius = isCelsius,
                    themeMode = themeMode
                )
            }

            // ── Hero ──
            item(key = "hero") {
                SkyCastHero(
                    info = info,
                    isCelsius = isCelsius,
                    previousDayTempC = state.previousDayTempC
                )
            }

            // ── Information-rich summary ──
            item(key = "summary") {
                val summaries = remember(info, isCelsius) {
                    WeatherSummaryEngine.summarizeDetailed(info, isCelsius)
                }
                SkyCastSummaryCard(
                    primaryText = summaries.primary,
                    secondary = summaries.secondary
                )
            }

            // ── Precipitation timeline (rain chance) ──
            item(key = "precip") {
                SkyCastPrecipitationTimeline(
                    hourly = info.hourly,
                    isCelsius = isCelsius
                )
            }

            // ── Severe alerts above the fold (sorted by severity, expired filtered) ──
            if (activeAlerts.isNotEmpty()) {
                item(key = "alerts") {
                    SkyCastAlertList(
                        alerts = activeAlerts,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── Hourly forecast (high priority, moved up) ──
            item(key = "hourly") {
                SkyCastHourlyTimeline(
                    hourly = info.hourly,
                    isCelsius = isCelsius
                )
            }

            // ── 7-day forecast ──
            item(key = "daily") {
                SkyCastDailyForecast(
                    daily = info.daily,
                    hourly = info.hourly,
                    isCelsius = isCelsius
                )
            }

            // ── Rewarded ad: unlock detailed forecast ──
            if (!isDetailedForecastUnlocked) {
                item(key = "unlock_details") {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onWatchAdForDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = com.vayu.weather.ui.theme.SkyBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch ad for extended forecast details")
                    }
                }
            }

            // ── Compact details (humidity / wind / pressure / visibility / dew) ──
            item(key = "details") {
                SkyCastDetailsGrid(
                    info = info,
                    isCelsius = isCelsius,
                    onOpenMetricDetail = onOpenMetricDetail,
                    onToggleUnit = onToggleUnit,
                    isCelsiusSelected = isCelsius
                )
            }

            // ── UV (compact) ──
            item(key = "uv") {
                SkyCastUvCard(
                    currentUv = null,
                    dailyPeakUv = info.daily.firstOrNull()?.uvIndex,
                    hourly = info.hourly,
                    isCelsius = isCelsius
                )
            }

            // ── Air quality (compact) ──
            if (state.airQuality != null) {
                item(key = "aqi") {
                    SkyCastAqiCard(airQuality = state.airQuality)
                }
            }

            // ── Wind compass ──
            item(key = "wind") {
                SkyCastWindCompass(
                    speedKph = info.current.windSpeed,
                    directionDeg = info.current.windDirection,
                    gustsKph = info.current.windGusts,
                    windUnit = windUnit
                )
            }

            // ── Sun & moon ──
            item(key = "sun_moon") {
                SkyCastSunMoonCard(
                    daily = info.daily.firstOrNull(),
                    currentTimeInLocation = info.current.time
                )
            }
        }
    }
}

// ============================================================================
// Top header — one and only location line
// ============================================================================

@Composable
private fun SkyCastLocationHeader(
    cityName: String?,
    regionName: String?,
    updatedAtLabel: String?,
    alertCount: Int,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleTheme: () -> Unit,
    isCelsius: Boolean,
    themeMode: ThemeMode
) {
    var menuOpen by remember { mutableStateOf(false) }

    val displayCity = cityName?.takeIf { it.isNotBlank() } ?: "Current location"
    val displayRegion = regionName?.takeIf { it.isNotBlank() }
    val a11y = buildString {
        append("Location: ").append(displayCity)
        if (!displayRegion.isNullOrBlank()) append(", ").append(displayRegion)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayCity,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            val subtitle = displayRegion ?: updatedAtLabel
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        // Compact action row: refresh + share + more
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactAction(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh weather",
                badgeCount = 0,
                onClick = onRefresh
            )
            CompactAction(
                icon = Icons.Rounded.Share,
                contentDescription = "Share weather",
                badgeCount = 0,
                onClick = onShare
            )
            Box {
                CompactAction(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    badgeCount = alertCount,
                    onClick = { menuOpen = true }
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    if (alertCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Alerts ($alertCount)") },
                            onClick = { menuOpen = false; onOpenAlerts() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { menuOpen = false; onOpenSettings() }
                    )
                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = { menuOpen = false; onOpenHistory() }
                    )
                    DropdownMenuItem(
                        text = { Text("Switch to ${if (isCelsius) "°F" else "°C"}") },
                        onClick = { menuOpen = false; onToggleUnit() }
                    )
                    val themeLabel = when (themeMode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System"
                    }
                    DropdownMenuItem(
                        text = { Text("Theme: $themeLabel") },
                        onClick = { menuOpen = false; onToggleTheme() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badgeCount: Int,
    onClick: () -> Unit
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
            if (badgeCount > 0) {
                androidx.compose.material3.Badge(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

// ============================================================================
// Details grid — compact, tappable
// ============================================================================

@Composable
private fun SkyCastDetailsGrid(
    info: WeatherInfo,
    isCelsius: Boolean,
    onToggleUnit: () -> Unit,
    isCelsiusSelected: Boolean,
    onOpenMetricDetail: (String) -> Unit
) {
    val humidity = info.current.humidity?.roundToInt()
    val pressure = info.current.surfacePressure?.roundToInt()
    val visibility = info.current.visibility?.let { v ->
        if (v < 1000) "${(v / 100.0).roundToInt() * 100}m"
        else "${(v / 1000.0).let { if (it - it.toInt() >= 0.5) it.toInt() + 1 else it.toInt() }} km"
    }
    val dew = info.current.dewPoint?.let { convertTempLocal(it, isCelsius) }
    val wind = info.current.windSpeed?.roundToInt()
    val gusts = info.current.windGusts?.roundToInt()
    val showWind = wind != null
    val showGusts = gusts != null && gusts != wind

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.WaterDrop,
                label = "Humidity",
                value = humidity?.let { "$it%" } ?: "—",
                accent = SkyBlue,
                onClick = { onOpenMetricDetail("humidity") },
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Air,
                label = "Wind",
                value = wind?.let { "$it" } ?: "—",
                subtitle = "km/h",
                accent = com.vayu.weather.ui.theme.SoftLavender,
                onClick = { onOpenMetricDetail("wind") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Compress,
                label = "Pressure",
                value = pressure?.let { "$it" } ?: "—",
                subtitle = "hPa",
                accent = WarmOrange,
                onClick = { onOpenMetricDetail("pressure") },
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Visibility,
                label = "Visibility",
                value = visibility?.let { "$it" } ?: "—",
                subtitle = "km",
                accent = FreshGreen,
                onClick = { onOpenMetricDetail("visibility") },
                modifier = Modifier.weight(1f)
            )
        }
        if (dew != null) {
            SkyCastMetricCard(
                icon = Icons.Rounded.WaterDrop,
                label = "Dew point",
                value = "${dew}°",
                accent = WarningAmber,
                subtitle = "Lower = drier air",
                onClick = { onOpenMetricDetail("dew") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showGusts) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Air,
                label = "Wind gusts",
                value = "$gusts",
                subtitle = "km/h · max today",
                accent = com.vayu.weather.ui.theme.SoftLavender,
                onClick = { onOpenMetricDetail("wind") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun convertTempLocal(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
