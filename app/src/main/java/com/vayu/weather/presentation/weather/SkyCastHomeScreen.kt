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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vayu.weather.R
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
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastColors
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.WarmOrange
import com.vayu.weather.ui.theme.WarningAmber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    isPremium: Boolean = false,
    onOpenPremium: () -> Unit = {},
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
            pressureUnit = settings.pressureUnit,
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
            isPremium = isPremium,
            onOpenPremium = onOpenPremium,
            modifier = modifier
        )
    }
}

@Composable
private fun SkyCastHomeContent(
    state: WeatherState,
    isCelsius: Boolean,
    windUnit: com.vayu.weather.presentation.weather.WindUnit,
    pressureUnit: String,
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
    isPremium: Boolean = false,
    onOpenPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val info = state.weatherInfo ?: return

    val activeAlerts = remember(state.alerts) { filterAndSortAlerts(state.alerts) }
    // API current-time string in the weather location's timezone, used
    // so "Now" in hourly/precipitation timelines matches the location.
    val currentTimeInLocation = info.current.time

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
                    themeMode = themeMode,
                    isPremium = isPremium,
                    onOpenPremium = onOpenPremium
                )
            }

            // ── Hero ──
            item(key = "hero") {
                SkyCastHero(
                    info = info,
                    isCelsius = isCelsius,
                    previousDayTempC = state.previousDayTempC,
                    onClick = onOpenDetail
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
                    isCelsius = isCelsius,
                    currentTimeInLocation = currentTimeInLocation
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
                    isCelsius = isCelsius,
                    windUnit = windUnit,
                    currentTimeInLocation = currentTimeInLocation
                )
            }

            // ── 7-day forecast ──
            item(key = "daily") {
                SkyCastDailyForecast(
                    daily = info.daily,
                    hourly = info.hourly,
                    isCelsius = isCelsius,
                    currentTimeInLocation = currentTimeInLocation
                )
            }

            // ── Extended outlook: ad-supported, or free forever with Premium ──
            if (!isDetailedForecastUnlocked) {
                item(key = "unlock_details") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isPremium) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onOpenPremium,
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = WarmOrange
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Go Premium — unlock all features")
                            }
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = onWatchAdForDetails,
                            modifier = Modifier.fillMaxWidth(),
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
                            Text("Watch ad for extended 48-hour outlook")
                        }
                    }
                }
            } else {
                item(key = "extended_outlook") {
                    SkyCastExtendedOutlook(hourly = info.hourly, isCelsius = isCelsius)
                }
            }

            // ── Compact details (humidity / wind / pressure / visibility / dew) ──
            item(key = "details") {
                SkyCastDetailsGrid(
                    info = info,
                    isCelsius = isCelsius,
                    windUnit = windUnit,
                    pressureUnit = pressureUnit,
                    onOpenMetricDetail = onOpenMetricDetail,
                    onToggleUnit = onToggleUnit,
                    isCelsiusSelected = isCelsius
                )
            }

            // ── UV (compact) ──
            item(key = "uv") {
                SkyCastUvCard(
                    currentUv = info.current.uvIndex,
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
    themeMode: ThemeMode,
    isPremium: Boolean,
    onOpenPremium: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val displayCity = cityName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.current_location)
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
                if (isPremium) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Premium member",
                        tint = com.vayu.weather.ui.theme.SkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
                contentDescription = stringResource(R.string.cd_refresh),
                badgeCount = 0,
                onClick = onRefresh
            )
            CompactAction(
                icon = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.share_weather),
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
                            text = { Text(stringResource(R.string.alert_count, alertCount)) },
                            onClick = { menuOpen = false; onOpenAlerts() }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isPremium) "Premium · Active" else "Go Premium",
                                color = if (isPremium) com.vayu.weather.ui.theme.SkyBlue
                                       else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isPremium) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPremium) com.vayu.weather.ui.theme.SkyBlue
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { menuOpen = false; onOpenPremium() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        onClick = { menuOpen = false; onOpenSettings() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history)) },
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
    windUnit: com.vayu.weather.presentation.weather.WindUnit,
    pressureUnit: String,
    onToggleUnit: () -> Unit,
    isCelsiusSelected: Boolean,
    onOpenMetricDetail: (String) -> Unit
) {
    val humidity = info.current.humidity?.roundToInt()
    val pressureHpa = info.current.surfacePressure
    val pressureDisplay = when (pressureUnit) {
        "inHg" -> pressureHpa?.let { "${(it / 33.8639).toInt()} inHg" }
        else -> pressureHpa?.let { "${it.toInt()} hPa" }
    }
    val visibility = info.current.visibility?.let { v ->
        if (v < 1000) "${(v / 100.0).roundToInt() * 100}m"
        else "${(v / 1000.0).let { if (it - it.toInt() >= 0.5) it.toInt() + 1 else it.toInt() }} km"
    }
    val dew = info.current.dewPoint?.let { convertTempLocal(it, isCelsius) }
    val windKmh = info.current.windSpeed
    val windDisplay = when (windUnit) {
        com.vayu.weather.presentation.weather.WindUnit.KPH -> windKmh?.let { "${it.roundToInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.MPH -> windKmh?.let { "${(it * 0.621371).toInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.MS -> windKmh?.let { "${(it / 3.6).toInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.KNOTS -> windKmh?.let { "${(it * 0.539957).toInt()}" }
    }
    val windLabel = when (windUnit) {
        com.vayu.weather.presentation.weather.WindUnit.KPH -> "km/h"
        com.vayu.weather.presentation.weather.WindUnit.MPH -> "mph"
        com.vayu.weather.presentation.weather.WindUnit.MS -> "m/s"
        com.vayu.weather.presentation.weather.WindUnit.KNOTS -> "kn"
    }
    val gustsDisplayRaw = info.current.windGusts
    val gustsDisplay = when (windUnit) {
        com.vayu.weather.presentation.weather.WindUnit.KPH -> gustsDisplayRaw?.let { "${it.roundToInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.MPH -> gustsDisplayRaw?.let { "${(it * 0.621371).toInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.MS -> gustsDisplayRaw?.let { "${(it / 3.6).toInt()}" }
        com.vayu.weather.presentation.weather.WindUnit.KNOTS -> gustsDisplayRaw?.let { "${(it * 0.539957).toInt()}" }
    }
    val showWind = windDisplay != null
    val showGusts = gustsDisplayRaw != null && gustsDisplay != windDisplay

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
                value = windDisplay?.let { "$it" } ?: "—",
                subtitle = windLabel,
                accent = com.vayu.weather.ui.theme.SoftLavender,
                onClick = { onOpenMetricDetail("wind") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Compress,
                label = "Pressure",
                value = pressureDisplay ?: "—",
                subtitle = if (pressureUnit == "inHg") "inHg" else "hPa",
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
                value = gustsDisplay ?: "—",
                subtitle = "$windLabel · max today",
                accent = com.vayu.weather.ui.theme.SoftLavender,
                onClick = { onOpenMetricDetail("wind") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun convertTempLocal(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()

// ============================================================================
// Extended 48-hour outlook — unlocked via the rewarded ad
// ============================================================================

/**
 * Renders hours 24–48 of the hourly feed (the main hourly timeline already
 * shows the first 24). This is the real content behind the rewarded-ad
 * unlock, so the reward promise matches what the user receives.
 */
@Composable
private fun SkyCastExtendedOutlook(
    hourly: List<com.vayu.weather.domain.model.HourlyWeather>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val sorted = remember(hourly) { hourly.sortedBy { it.time } }
    val extended = remember(sorted) { sorted.drop(24).take(24) }
    if (extended.isEmpty()) return

    SkyCastCard(contentPadding = PaddingValues(vertical = 16.dp)) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkyCastSectionHeader(
                    title = "Extended outlook",
                    subtitle = "Hours 24–48 · unlocked"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                extended.forEach { h ->
                    val dateTime = remember(h.time) {
                        try { LocalDateTime.parse(h.time, DateTimeFormatter.ISO_DATE_TIME) } catch (_: Exception) { null }
                    }
                    val isDayHour = dateTime?.hour?.let { it in 6..18 } ?: true
                    val hourLabel = dateTime?.format(DateTimeFormatter.ofPattern("h a"))
                        ?: h.time.substringAfter("T").take(5)
                    // Composable calls (string resources) must happen outside the
                    // semantics lambda — it is not a composable context.
                    val desc = localizedWeatherDescription(h.weatherCode, isDayHour)
                    val a11y = run {
                        val t = if (isCelsius) "${h.temperature.roundToInt()}°C"
                            else "${(h.temperature * 9.0 / 5.0 + 32).roundToInt()}°F"
                        "$hourLabel, $t, $desc"
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(56.dp)
                            .semantics { contentDescription = a11y }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = hourLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            imageVector = getWeatherIcon(h.weatherCode, isDayHour),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${convertTempLocal(h.temperature, isCelsius)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        val rain = h.precipitationProbability ?: 0
                        Text(
                            text = if (rain >= 20) "$rain%" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBlue.copy(alpha = if (rain >= 20) 0.95f else 0.3f)
                        )
                    }
                }
            }
        }
    }
}
