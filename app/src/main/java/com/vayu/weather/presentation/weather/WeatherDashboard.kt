package com.vayu.weather.presentation.weather

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import kotlinx.coroutines.delay
import com.vayu.weather.R
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherDescription
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.ads.AdBanner
import com.vayu.weather.ui.theme.AmberGlow
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SunsetRed
import com.vayu.weather.ui.theme.TrendingGreen
import com.vayu.weather.ui.theme.TrendingRed
import com.vayu.weather.ui.theme.WarmOrange
import com.vayu.weather.ui.theme.WarningAmber
import com.vayu.weather.presentation.components.AirQualityCard
import com.vayu.weather.presentation.components.WeatherBackground
import com.vayu.weather.presentation.components.WeatherTrends
import com.vayu.weather.presentation.components.UvIndexCard
import com.vayu.weather.presentation.components.WindCard
import com.vayu.weather.presentation.components.PressureCard
import com.vayu.weather.presentation.components.MoonPhaseCard
import com.vayu.weather.presentation.components.PrecipitationTimelineCard
import com.vayu.weather.presentation.components.SmartSuggestionsCard
import com.vayu.weather.presentation.components.PressureTrendChart
import com.vayu.weather.presentation.components.SunArcAnimation
import com.vayu.weather.presentation.components.StormTrackerCard
import com.vayu.weather.presentation.components.generateStormAlerts
import com.vayu.weather.presentation.components.GardeningPetWeatherCard
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private object ConversionConstants {
    const val KPH_TO_MPH = 0.621371
    const val KPH_TO_MS = 1.0 / 3.6
    const val KPH_TO_KNOTS = 0.539957
    const val CELSIUS_TO_FAHRENHEIT_FACTOR = 9.0 / 5.0
    const val FAHRENHEIT_OFFSET = 32.0
}

@Composable
private fun rememberHapticFeedback(): (Int) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { hapticFeedbackType: Int ->
            view.performHapticFeedback(hapticFeedbackType)
        }
    }
}

private fun convertTemp(temp: Double, isCelsius: Boolean): Int {
    return if (isCelsius) temp.roundToInt()
    else (temp * ConversionConstants.CELSIUS_TO_FAHRENHEIT_FACTOR + ConversionConstants.FAHRENHEIT_OFFSET).roundToInt()
}

private fun convertWind(windKph: Double?, unit: WindUnit): String {
    val speed = windKph ?: return "--"
    return when (unit) {
        WindUnit.KPH -> "${speed.roundToInt()}"
        WindUnit.MPH -> "${(speed * ConversionConstants.KPH_TO_MPH).roundToInt()}"
        WindUnit.MS -> "${(speed * ConversionConstants.KPH_TO_MS).roundToInt()}"
        WindUnit.KNOTS -> "${(speed * ConversionConstants.KPH_TO_KNOTS).roundToInt()}"
    }
}

@Composable
private fun windUnitLabel(unit: WindUnit): String = when (unit) {
    WindUnit.KPH -> stringResource(R.string.wind_kph)
    WindUnit.MPH -> stringResource(R.string.wind_mph)
    WindUnit.MS -> stringResource(R.string.wind_ms)
    WindUnit.KNOTS -> stringResource(R.string.wind_knots)
}

private fun formatWindDirection(degrees: Double): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    if (degrees.isNaN()) return "--"
    val normalized = ((degrees % 360) + 360) % 360
    val index = ((normalized + 22.5) / 45).toInt() % 8
    return directions[index.coerceIn(0, directions.lastIndex)]
}

fun getWeatherIcon(weatherCode: Int, isDay: Boolean): ImageVector {
    return when (weatherCode) {
        0 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        1 -> Icons.Rounded.WbSunny
        2 -> Icons.Rounded.Cloud
        3 -> Icons.Rounded.CloudQueue
        45, 48 -> Icons.Rounded.CloudQueue
        51 -> Icons.Rounded.WaterDrop
        53 -> Icons.Rounded.WaterDrop
        55, 61 -> Icons.Rounded.Umbrella
        63, 65 -> Icons.Rounded.Thunderstorm
        71, 73, 75 -> Icons.Rounded.AcUnit
        80 -> Icons.Rounded.WaterDrop
        81 -> Icons.Rounded.Umbrella
        82, 95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.CloudOff
    }
}

@Composable
internal fun localizedWeatherDescription(weatherCode: Int, isDay: Boolean): String {
    val resId = when (weatherCode) {
        0 -> if (isDay) R.string.weather_clear_sky else R.string.weather_clear_night
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_fog
        51 -> R.string.weather_light_drizzle
        53 -> R.string.weather_moderate_drizzle
        55 -> R.string.weather_dense_drizzle
        61 -> R.string.weather_slight_rain
        63 -> R.string.weather_moderate_rain
        65 -> R.string.weather_heavy_rain
        71 -> R.string.weather_slight_snow
        73 -> R.string.weather_moderate_snow
        75 -> R.string.weather_heavy_snow
        80 -> R.string.weather_slight_rain_showers
        81 -> R.string.weather_moderate_rain_showers
        82 -> R.string.weather_violent_rain_showers
        95 -> R.string.weather_thunderstorm
        96 -> R.string.weather_thunderstorm_hail
        99 -> R.string.weather_thunderstorm_heavy_hail
        else -> R.string.weather_cloudy
    }
    return stringResource(resId)
}

// ============================================================
// MAIN DASHBOARD
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboard(
    state: WeatherState,
    settings: SettingsState = SettingsState(),
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onToggleUnit: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
    onOpenDetail: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onShare: () -> Unit = {},
    onDismissRefreshError: () -> Unit = {},
    cityName: String? = null,
    modifier: Modifier = Modifier
) {
    val isCelsius = settings.temperatureUnit == TemperatureUnit.CELSIUS
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.refreshError) {
        state.refreshError?.let {
            snackbarHostState.showSnackbar(it)
            onDismissRefreshError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        state.weatherInfo?.let { info ->
            WeatherBackground(
                weatherCode = info.current.weatherCode,
                isDay = info.current.isDay
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item(key = "topbar") {
                        TopBar(
                            onToggleUnit = onToggleUnit,
                            onOpenSettings = onOpenSettings,
                            onOpenAlerts = onOpenAlerts,
                            onOpenHistory = onOpenHistory,
                            onShare = onShare,
                            weatherCode = info.current.weatherCode,
                            isCelsius = isCelsius,
                            cityName = cityName,
                            onHaptic = { _ -> }
                        )
                    }

                    item(key = "hero") {
                        HeroSection(
                            info = info,
                            cityName = cityName,
                            isCelsius = isCelsius,
                            onClick = onOpenDetail
                        )
                    }

                    item(key = "summary") {
                        WeatherSummary(
                            info = info,
                            isCelsius = isCelsius
                        )
                    }

                    item(key = "hourly") {
                        Spacer(modifier = Modifier.height(24.dp))
                        HourlyForecastSection(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius
                        )
                    }

                    item(key = "daily") {
                        Spacer(modifier = Modifier.height(16.dp))
                        DailyForecastSection(
                            dailyData = info.daily,
                            isCelsius = isCelsius
                        )
                    }

                    item(key = "details") {
                        Spacer(modifier = Modifier.height(16.dp))
                        WeatherDetailsSection(
                            info = info,
                            isCelsius = isCelsius,
                            windUnit = settings.windUnit
                        )
                    }

                    item(key = "metrics") {
                        Spacer(modifier = Modifier.height(16.dp))
                            // Premium metric cards — responsive: 2 cols phone, 4 cols tablet
                            val windUnitLabel = when (settings.windUnit) {
                                WindUnit.KPH -> "km/h"
                                WindUnit.MPH -> "mph"
                                WindUnit.MS -> "m/s"
                                WindUnit.KNOTS -> "kn"
                            }
                            androidx.compose.foundation.layout.BoxWithConstraints(
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                val isWide = maxWidth > 480.dp
                                if (isWide) {
                                    // 4-column grid for tablets/foldables
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        UvIndexCard(
                                            uvIndex = info.daily.firstOrNull()?.uvIndex,
                                            modifier = Modifier.width(180.dp)
                                        )
                                        WindCard(
                                            speed = info.current.windSpeed,
                                            direction = info.current.windDirection,
                                            gusts = info.current.windGusts,
                                            unitLabel = windUnitLabel,
                                            modifier = Modifier.width(180.dp)
                                        )
                                        PressureCard(
                                            pressure = info.current.surfacePressure,
                                            modifier = Modifier.width(180.dp)
                                        )
                                        MoonPhaseCard(
                                            modifier = Modifier.width(180.dp)
                                        )
                                    }
                                } else {
                                    // 2-column grid for phones
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            UvIndexCard(
                                                uvIndex = info.daily.firstOrNull()?.uvIndex,
                                                modifier = Modifier.weight(1f)
                                            )
                                            WindCard(
                                                speed = info.current.windSpeed,
                                                direction = info.current.windDirection,
                                                gusts = info.current.windGusts,
                                                unitLabel = windUnitLabel,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PressureCard(
                                                pressure = info.current.surfacePressure,
                                                modifier = Modifier.weight(1f)
                                            )
                                            MoonPhaseCard(
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    item(key = "precip") {
                        Spacer(modifier = Modifier.height(16.dp))
                            PrecipitationTimelineCard(
                                hourlyData = info.hourly,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                    }

                    item(key = "insights") {
                        Spacer(modifier = Modifier.height(16.dp))
                        AIInsightsCard(
                            info = info,
                            isCelsius = isCelsius
                        )
                    }

                    item(key = "trends") {
                        Spacer(modifier = Modifier.height(16.dp))
                        WeatherTrends(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // === SMART SUGGESTIONS ===
                    item(key = "suggestions") {
                        Spacer(modifier = Modifier.height(16.dp))
                        SmartSuggestionsCard(weatherInfo = info)
                    }

                    // === STORM TRACKER ===
                    item(key = "storm") {
                        val stormAlerts = remember(info) { generateStormAlerts(info) }
                        if (stormAlerts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            StormTrackerCard(alerts = stormAlerts)
                        }
                    }

                    // === PRESSURE TREND ===
                    item(key = "pressure") {
                        Spacer(modifier = Modifier.height(16.dp))
                        PressureTrendChart(hourlyData = info.hourly)
                    }

                    // === SUN ARC ===
                    item(key = "sunarc") {
                        Spacer(modifier = Modifier.height(16.dp))
                        SunArcAnimation(dailyData = info.daily)
                    }

                    // === GARDENING / PET ===
                    item(key = "activities") {
                        Spacer(modifier = Modifier.height(16.dp))
                        GardeningPetWeatherCard(weatherInfo = info)
                    }

                    if (state.lastUpdatedTime != null) {
                        item(key = "updated") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.lastUpdatedTime!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item(key = "sunmoon") {
                        Spacer(modifier = Modifier.height(16.dp))
                        SunMoonSection(dailyData = info.daily)
                    }

                    if (state.airQuality != null) {
                        item(key = "aqi") {
                            Spacer(modifier = Modifier.height(16.dp))
                            AirQualityCard(
                                airQuality = state.airQuality,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    item(key = "ad") {
                        Spacer(modifier = Modifier.height(12.dp))
                        AdBanner()
                    }
                }
            }

            if (isIdealWeather(info)) {
                ConfettiOverlay(modifier = Modifier.fillMaxSize())
            }
        }

        if (state.isLoading) {
            LoadingState()
        }

        state.error?.let {
            ErrorState(message = it, onRetry = onRetry)
        }

        // Offline indicator — shows when data is stale or refresh failed
        if (state.refreshError != null && state.weatherInfo != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = stringResource(R.string.offline_indicator),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = stringResource(R.string.offline_indicator),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}// ============================================================
// TOP BAR — premium header
// ============================================================

@Composable
private fun TopBar(
    onToggleUnit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit,
    onShare: () -> Unit,
    weatherCode: Int? = null,
    isCelsius: Boolean = true,
    cityName: String? = null,
    onHaptic: (Int) -> Unit = {}
) {
    val haptic = rememberHapticFeedback()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Location icon + city name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = stringResource(R.string.current_location),
                modifier = Modifier.size(18.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = cityName ?: stringResource(R.string.default_city_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }

        // Right: Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallIconButton(
                icon = Icons.Rounded.NotificationsActive,
                contentDescription = stringResource(R.string.weather_alerts),
                onClick = { haptic(HapticFeedbackConstants.VIRTUAL_KEY); onOpenAlerts() }
            )
            SmallIconButton(
                icon = Icons.Rounded.History,
                contentDescription = stringResource(R.string.weather_history_title),
                onClick = { haptic(HapticFeedbackConstants.VIRTUAL_KEY); onOpenHistory() }
            )
            SmallIconButton(
                icon = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.share_weather),
                onClick = { haptic(HapticFeedbackConstants.VIRTUAL_KEY); onShare() }
            )
            // Unit toggle pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { haptic(HapticFeedbackConstants.VIRTUAL_KEY); onToggleUnit() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCelsius) "°C" else "°F",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            SmallIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings),
                onClick = { haptic(HapticFeedbackConstants.VIRTUAL_KEY); onOpenSettings() }
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================
// HERO SECTION — clean, spacious
// ============================================================

@Composable
private fun HeroSection(
    info: WeatherInfo,
    cityName: String?,
    isCelsius: Boolean,
    onClick: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hero_scale"
    )

    val weatherDesc = localizedWeatherDescription(info.current.weatherCode, info.current.isDay)
    val heroTemp = convertTemp(info.current.temperature, isCelsius)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${cityName ?: ""}, $weatherDesc, $heroTemp degrees"
            }
            .clickable(onClick = {
                haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            })
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // City name
        Text(
            text = cityName ?: stringResource(R.string.default_city_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weather icon
        Icon(
            imageVector = getWeatherIcon(info.current.weatherCode, info.current.isDay),
            contentDescription = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            modifier = Modifier.size(56.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Large temperature — animated
        AnimatedTemperatureText(
            temperature = info.current.temperature,
            isCelsius = isCelsius
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Weather description
        Text(
            text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // High/Low + Feels like row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            info.daily.firstOrNull()?.let { today ->
                val high = convertTemp(today.maxTemp, isCelsius)
                val low = convertTemp(today.minTemp, isCelsius)
                Text(
                    text = "H:${high}°  L:${low}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            info.current.apparentTemperature?.let { apparent ->
                Text(
                    text = stringResource(R.string.feels_like, convertTemp(apparent, isCelsius)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ============================================================
// WEATHER SUMMARY — contextual sentence below hero
// ============================================================

@Composable
private fun WeatherSummary(
    info: WeatherInfo,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val summary = remember(info) { generateWeatherSummary(info, isCelsius) }
    if (summary.isNotEmpty()) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

private fun plainWeatherDescription(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "clear sky" else "clear night"
    1 -> "mainly clear"
    2 -> "partly cloudy"
    3 -> "overcast"
    45, 48 -> "fog"
    51 -> "light drizzle"
    53 -> "moderate drizzle"
    55 -> "dense drizzle"
    61 -> "slight rain"
    63 -> "moderate rain"
    65 -> "heavy rain"
    71 -> "slight snow"
    73 -> "moderate snow"
    75 -> "heavy snow"
    80 -> "rain showers"
    81 -> "moderate rain showers"
    82 -> "violent rain showers"
    95 -> "thunderstorm"
    96 -> "thunderstorm with hail"
    99 -> "thunderstorm with heavy hail"
    else -> "cloudy"
}

private fun generateWeatherSummary(info: WeatherInfo, isCelsius: Boolean): String {
    val current = info.current
    val today = info.daily.firstOrNull()
    val nextHours = info.hourly.sortedBy { it.time }.take(6)
    val nowHour = java.time.LocalDateTime.now().hour
    val timeOfDay = when {
        nowHour in 5..11 -> "this morning"
        nowHour in 12..16 -> "this afternoon"
        nowHour in 17..20 -> "this evening"
        else -> "tonight"
    }
    val temp = current.temperature.roundToInt()
    val high = today?.maxTemp?.roundToInt()
    val low = today?.minTemp?.roundToInt()
    val precipHours = nextHours.count { it.weatherCode in 51..82 || it.weatherCode in 95..99 }
    val windSpeed = current.windSpeed ?: 0.0
    val isHot = temp >= 35
    val isCold = temp <= 5
    val isWindy = windSpeed > 40
    val isRainy = current.weatherCode in 51..82 || current.weatherCode in 95..99
    val isStormy = current.weatherCode in 95..99

    return buildString {
        // Opening sentence
        when {
            isStormy -> append("Stormy conditions with thunder and lightning $timeOfDay.")
            isRainy && windSpeed > 30 -> append("Rain and gusty winds $timeOfDay.")
            isRainy -> append("Rain expected $timeOfDay.")
            isHot -> append("Very hot at ${temp}° — stay hydrated and avoid prolonged sun exposure.")
            isCold -> append("Cold at ${temp}° — dress warmly $timeOfDay.")
            isWindy -> append("Strong winds of ${windSpeed.roundToInt()} km/h $timeOfDay.")
            current.weatherCode == 0 && current.isDay -> append("Beautiful clear skies $timeOfDay.")
            current.weatherCode in 1..2 -> append("Mostly clear with some clouds $timeOfDay.")
            current.weatherCode == 3 -> append("Overcast skies $timeOfDay.")
            current.weatherCode in 45..48 -> append("Foggy conditions may reduce visibility.")
            current.weatherCode in 71..75 -> append("Snow expected $timeOfDay.")
            else -> append("Current conditions: ${plainWeatherDescription(current.weatherCode, current.isDay)}.")
        }
        // Additional context
        if (precipHours > 2) {
            append(" Rain likely in $precipHours of the next 6 hours.")
        }
        if (high != null && low != null) {
            val tempDesc = when {
                high >= 40 -> "extreme heat"
                high >= 35 -> "very warm"
                high >= 28 -> "warm"
                high >= 20 -> "mild"
                high >= 10 -> "cool"
                else -> "cold"
            }
            append(" Today will be $tempDesc with a high of ${high}° and low of ${low}°.")
        }
    }
}

// ============================================================
// HOURLY FORECAST — modern horizontal scroller
// ============================================================

@Composable
private fun HourlyForecastSection(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean
) {
    var selectedHour by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHapticFeedback()
    val sorted = remember(hourlyData) { hourlyData.sortedBy { it.time }.take(24) }
    val selectedData = remember(selectedHour, sorted) { sorted.find { it.time == selectedHour } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(sorted.size) { index ->
                    val prevTemp = sorted.getOrNull(index - 1)?.temperature
                    HourlyPillCard(
                        data = sorted[index],
                        isCelsius = isCelsius,
                        prevTemp = prevTemp,
                        isSelected = selectedHour == sorted[index].time,
                        onClick = {
                            haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                            selectedHour = if (selectedHour == sorted[index].time) null else sorted[index].time
                        }
                    )
                }
            }

            // Expanded details for selected hour
            AnimatedVisibility(
                visible = selectedData != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedData?.let { data ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp)
                    ) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HourlyDetailItem("Temp", "${convertTemp(data.temperature, isCelsius)}°")
                            HourlyDetailItem("Wind", "${convertWind(data.windSpeed, WindUnit.KPH)} km/h")
                            HourlyDetailItem("Humidity", "${data.humidity?.roundToInt() ?: "--"}%")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HourlyDetailItem("Rain", data.precipitationProbability?.let { "$it%" } ?: "--")
                            HourlyDetailItem("Wind Dir", data.windDirection?.let { formatWindDirection(it) } ?: "--")
                            HourlyDetailItem("Condition", localizedWeatherDescription(data.weatherCode, true))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HourlyDetailItem("Pressure", "${data.pressure?.roundToInt() ?: "--"} hPa")
                            HourlyDetailItem("Visibility", data.visibility?.let { "${(it / 1000).roundToInt()} km" } ?: "--")
                            HourlyDetailItem("Precip", data.precipitation?.let { "${String.format("%.1f", it)} mm" } ?: "--")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun HourlyPillCard(
    data: HourlyWeather,
    isCelsius: Boolean,
    prevTemp: Double? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val currentHour = java.time.LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
    val isNow = data.time == currentHour

    val timeLabel = if (isNow) {
        stringResource(R.string.now)
    } else {
        try {
            java.time.LocalDateTime.parse(data.time, DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("ha"))
        } catch (e: Exception) { data.time.takeLast(5) }
    }

    val precipProb = data.precipitationProbability
        ?: when (data.weatherCode) {
            0 -> 0
            1, 2, 3 -> 5
            45, 48 -> 15
            51, 53, 55 -> 40 + (data.weatherCode - 51) * 15
            61, 63, 65 -> 50 + (data.weatherCode - 61) * 20
            80, 81, 82 -> 60 + (data.weatherCode - 80) * 15
            71, 73, 75 -> 70 + (data.weatherCode - 71) * 10
            95, 96, 99 -> 85
            else -> 0
        }
    val showPrecip = precipProb >= 20

    val trendArrow = remember(prevTemp, data.temperature) {
        when {
            prevTemp == null -> null
            data.temperature > prevTemp + 0.5 -> "up"
            data.temperature < prevTemp - 0.5 -> "down"
            else -> "same"
        }
    }

    Column(
        modifier = Modifier
            .width(60.dp)
            .then(
                if (isSelected) Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SkyBlue.copy(alpha = 0.18f))
                else if (isNow) Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (isNow) 1f else 0.6f),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),
            contentDescription = plainWeatherDescription(data.weatherCode, true),
            modifier = Modifier.size(22.dp),
            tint = Color.White.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${convertTemp(data.temperature, isCelsius)}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (trendArrow == "up") {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = stringResource(R.string.trending_up),
                    modifier = Modifier.size(10.dp),
                    tint = TrendingGreen
                )
            } else if (trendArrow == "down") {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = stringResource(R.string.trending_down),
                    modifier = Modifier.size(10.dp),
                    tint = TrendingRed
                )
            }
        }

        if (showPrecip) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (precipProb / 100f).coerceIn(0.1f, 1f))
                        .background(
                            SkyBlue.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            Text(
                text = "$precipProb%",
                style = MaterialTheme.typography.labelSmall,
                color = SkyBlue.copy(alpha = 0.85f),
                fontSize = 9.sp
            )
        }
    }
}

// ============================================================
// DAILY FORECAST — clean rows with temp bar
// ============================================================

@Composable
private fun DailyForecastSection(
    dailyData: List<DailyWeather>,
    isCelsius: Boolean
) {
    if (dailyData.isEmpty()) return
    val allMin = remember(dailyData) { dailyData.minOf { it.minTemp }.roundToInt() }
    val allMax = remember(dailyData) { dailyData.maxOf { it.maxTemp }.roundToInt() }
    var expandedDay by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHapticFeedback()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.seven_day_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )

            dailyData.forEachIndexed { index, day ->
                DailyRow(
                    data = day,
                    isCelsius = isCelsius,
                    globalMin = allMin,
                    globalMax = allMax,
                    isLast = index == dailyData.lastIndex,
                    isExpanded = expandedDay == day.date,
                    onClick = {
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                        expandedDay = if (expandedDay == day.date) null else day.date
                    }
                )
            }
        }
    }
}

@Composable
private fun DailyRow(
    data: DailyWeather,
    isCelsius: Boolean,
    globalMin: Int,
    globalMax: Int,
    isLast: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {}
) {
    val nowLabel = stringResource(R.string.now)
    val today = java.time.LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dateLabel = try {
        val date = java.time.LocalDate.parse(data.date)
        when (date) {
            today -> nowLabel
            tomorrow -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE"))
        }
    } catch (e: Exception) { "---" }

    val minTemp = convertTemp(data.minTemp, isCelsius)
    val maxTemp = convertTemp(data.maxTemp, isCelsius)
    val precipText = data.precipitationProbability?.let { if (it > 0) "$it%" else "" } ?: ""

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (dateLabel == nowLabel) FontWeight.Bold else FontWeight.Medium,
                color = Color.White.copy(alpha = if (dateLabel == nowLabel) 1f else 0.8f),
                modifier = Modifier.width(64.dp),
                maxLines = 1
            )

            Icon(
                imageVector = getWeatherIcon(data.weatherCode, true),
                contentDescription = plainWeatherDescription(data.weatherCode, true),
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = precipText,
                style = MaterialTheme.typography.labelSmall,
                color = SkyBlue,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${minTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(8.dp))

            TempRangeBar(
                globalMin = globalMin,
                globalMax = globalMax,
                dayMin = minTemp.coerceAtMost(maxTemp),
                dayMax = maxTemp.coerceAtLeast(minTemp),
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${maxTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.End
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HourlyDetailItem("High", "${maxTemp}°")
                    HourlyDetailItem("Low", "${minTemp}°")
                    HourlyDetailItem("Rain", data.precipitationProbability?.let { "$it%" } ?: "--")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HourlyDetailItem("UV", data.uvIndex?.let { "${it.roundToInt()}" } ?: "--")
                    HourlyDetailItem("Condition", localizedWeatherDescription(data.weatherCode, true))
                    if (data.sunrise != null && data.sunset != null) {
                        HourlyDetailItem("Sun", "${data.sunrise.take(5)} / ${data.sunset.take(5)}")
                    }
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun TempRangeBar(
    globalMin: Int,
    globalMax: Int,
    dayMin: Int,
    dayMax: Int,
    modifier: Modifier = Modifier
) {
    val range = (globalMax - globalMin).toFloat()
    val startFraction = if (range == 0f) 0f else ((dayMin - globalMin) / range).coerceIn(0f, 1f)
    val widthFraction = if (range == 0f) 1f else ((dayMax - dayMin) / range).coerceIn(0.05f, 1f)

    Canvas(modifier = modifier) {
        val trackColor = Color.White.copy(alpha = 0.1f)
        val warmColor = WarmOrange
        val coolColor = SkyBlue
        drawRoundRect(color = trackColor, cornerRadius = CornerRadius(4f))
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(coolColor, warmColor),
                startX = size.width * startFraction,
                endX = size.width * (startFraction + widthFraction)
            ),
            topLeft = Offset(size.width * startFraction, 0f),
            size = Size(size.width * widthFraction, size.height),
            cornerRadius = CornerRadius(4f)
        )
    }
}

// ============================================================
// WEATHER DETAILS — clean grid
// ============================================================

@Composable
private fun WeatherDetailsSection(
    info: WeatherInfo,
    isCelsius: Boolean,
    windUnit: WindUnit
) {
    var expandedCard by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHapticFeedback()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.weather_details),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpandableDetailCard(
                    icon = Icons.Rounded.WaterDrop,
                    label = stringResource(R.string.humidity),
                    value = "${info.current.humidity?.roundToInt() ?: "--"}%",
                    extra = info.current.humidity?.let { "${(it / 100f * 0.5 + 0.5).roundToInt()}/10 Comfort" } ?: "",
                    isExpanded = expandedCard == "humidity",
                    onClick = {
                        expandedCard = if (expandedCard == "humidity") null else "humidity"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
                ExpandableDetailCard(
                    icon = Icons.Rounded.Air,
                    label = stringResource(R.string.wind),
                    value = "${convertWind(info.current.windSpeed, windUnit)} ${windUnitLabel(windUnit)}",
                    extra = info.current.windSpeed?.let { "${(it * 0.1).roundToInt()} gusts" } ?: "",
                    isExpanded = expandedCard == "wind",
                    onClick = {
                        expandedCard = if (expandedCard == "wind") null else "wind"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpandableDetailCard(
                    icon = Icons.Rounded.Thermostat,
                    label = stringResource(R.string.pressure),
                    value = info.current.surfacePressure?.let { "${it.roundToInt()} hPa" } ?: "--",
                    extra = if ((info.current.surfacePressure ?: 1013.0) > 1013.0) "High pressure" else "Low pressure",
                    isExpanded = expandedCard == "pressure",
                    onClick = {
                        expandedCard = if (expandedCard == "pressure") null else "pressure"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
                ExpandableDetailCard(
                    icon = Icons.Rounded.Visibility,
                    label = stringResource(R.string.visibility),
                    value = info.current.visibility?.let { "${(it / 1000).roundToInt()} km" } ?: "--",
                    extra = when {
                        (info.current.visibility ?: 0.0) < 1000 -> "Foggy"
                        (info.current.visibility ?: 0.0) < 5000 -> "Reduced"
                        else -> "Clear"
                    },
                    isExpanded = expandedCard == "visibility",
                    onClick = {
                        expandedCard = if (expandedCard == "visibility") null else "visibility"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpandableDetailCard(
                    icon = Icons.Rounded.WbSunny,
                    label = stringResource(R.string.uv_index),
                    value = "${info.daily.firstOrNull()?.uvIndex?.roundToInt() ?: "--"}",
                    extra = when {
                        (info.daily.firstOrNull()?.uvIndex ?: 0.0) >= 8 -> "Very High"
                        (info.daily.firstOrNull()?.uvIndex ?: 0.0) >= 6 -> "High"
                        (info.daily.firstOrNull()?.uvIndex ?: 0.0) >= 3 -> "Moderate"
                        else -> "Low"
                    },
                    isExpanded = expandedCard == "uv",
                    onClick = {
                        expandedCard = if (expandedCard == "uv") null else "uv"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
                ExpandableDetailCard(
                    icon = Icons.Rounded.AcUnit,
                    label = stringResource(R.string.dew_point),
                    value = info.current.dewPoint?.let { "${convertTemp(it, isCelsius)}°" } ?: "--",
                    extra = if ((info.current.dewPoint ?: 0.0) > (info.current.temperature - 2)) "Humid" else "Dry",
                    isExpanded = expandedCard == "dew",
                    onClick = {
                        expandedCard = if (expandedCard == "dew") null else "dew"
                        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExpandableDetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    extra: String = "",
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isExpanded) 0.12f else 0.07f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                if (extra.isNotEmpty()) {
                    Text(
                        text = if (isExpanded) "−" else "+",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded && extra.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Text(
                        text = extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ============================================================
// AI INSIGHTS
// ============================================================

@Composable
private fun AIInsightsCard(
    info: WeatherInfo,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val insights = remember(info) { generateInsights(info, isCelsius) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = stringResource(R.string.ai_insights),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_insights),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            insights.forEach { insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = when (insight.severity) {
                                    InsightSeverity.WARNING -> AmberGlow.copy(alpha = 0.15f)
                                    InsightSeverity.ALERT -> SunsetRed.copy(alpha = 0.15f)
                                    InsightSeverity.POSITIVE -> FreshGreen.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.06f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = insight.icon,
                            contentDescription = insight.text,
                            modifier = Modifier.size(14.dp),
                            tint = when (insight.severity) {
                                InsightSeverity.WARNING -> AmberGlow
                                InsightSeverity.ALERT -> SunsetRed
                                InsightSeverity.POSITIVE -> FreshGreen
                                else -> Color.White.copy(alpha = 0.6f)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = insight.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

enum class InsightSeverity { POSITIVE, NEUTRAL, WARNING, ALERT }

private data class Insight(
    val icon: ImageVector,
    val text: String,
    val severity: InsightSeverity
)

private fun generateInsights(info: WeatherInfo, isCelsius: Boolean): List<Insight> {
    val insights = mutableListOf<Insight>()
    val code = info.current.weatherCode
    val current = info.current
    val daily = info.daily.firstOrNull()

    when {
        code == 0 && current.isDay -> {
            insights += Insight(
                icon = Icons.Rounded.WbSunny,
                text = if ((daily?.uvIndex ?: 0.0) >= 7)
                    "High UV index expected. Apply SPF 30+ and seek shade during midday hours."
                else
                    "Great day for outdoor activities! UV levels are moderate.",
                severity = if ((daily?.uvIndex ?: 0.0) >= 7) InsightSeverity.WARNING else InsightSeverity.POSITIVE
            )
            if ((current.humidity ?: 50.0) < 30) {
                insights += Insight(
                    icon = Icons.Rounded.WaterDrop,
                    text = "Dry air detected. Stay hydrated and consider using moisturizer.",
                    severity = InsightSeverity.NEUTRAL
                )
            }
        }
        code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> {
            insights += Insight(
                icon = Icons.Rounded.Umbrella,
                text = "Rain expected for the next several hours. Don't forget your umbrella.",
                severity = InsightSeverity.WARNING
            )
            if ((current.windSpeed ?: 0.0) > 25) {
                insights += Insight(
                    icon = Icons.Rounded.Air,
                    text = "Gusty winds with rain. Secure loose outdoor items before heading out.",
                    severity = InsightSeverity.WARNING
                )
            }
            if ((current.visibility ?: 10000.0) < 5000) {
                val visKm = (current.visibility!! / 1000.0).roundToInt()
                insights += Insight(
                    icon = Icons.Rounded.Visibility,
                    text = "Reduced visibility ($visKm km). Drive with caution and use headlights.",
                    severity = InsightSeverity.WARNING
                )
            }
        }
        code in listOf(71, 73, 75) -> {
            insights += Insight(
                icon = Icons.Rounded.AcUnit,
                text = "Snow accumulation possible today. Allow extra travel time and dress in warm layers.",
                severity = InsightSeverity.WARNING
            )
            if ((current.apparentTemperature ?: current.temperature) < 20) {
                insights += Insight(
                    icon = Icons.Rounded.Thermostat,
                    text = "Wind chill makes it feel much colder. Bundle up with insulated layers.",
                    severity = InsightSeverity.WARNING
                )
            }
        }
        code in listOf(95, 96, 99) -> {
            insights += Insight(
                icon = Icons.Rounded.Thunderstorm,
                text = "Severe weather in your area. Stay indoors and away from windows.",
                severity = InsightSeverity.ALERT
            )
            insights += Insight(
                icon = Icons.Rounded.Umbrella,
                text = "Heavy downpours likely. Avoid flooded roadways and low-lying areas.",
                severity = InsightSeverity.ALERT
            )
        }
    }

    if (insights.isEmpty()) {
        insights += Insight(
            icon = Icons.Rounded.WbSunny,
            text = "Pleasant conditions ahead. Enjoy your day!",
            severity = InsightSeverity.POSITIVE
        )
    }

    return insights.take(4)
}

// ============================================================
// SUN & MOON
// ============================================================

@Composable
private fun SunMoonSection(dailyData: List<DailyWeather>) {
    val today = dailyData.firstOrNull() ?: return
    val sunrise = today.sunrise ?: return
    val sunset = today.sunset ?: return

    val sunriseTime = remember(sunrise) {
        try {
            java.time.LocalTime.parse(sunrise.substringAfter("T"))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { sunrise }
    }
    val sunsetTime = remember(sunset) {
        try {
            java.time.LocalTime.parse(sunset.substringAfter("T"))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { sunset }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.sun_and_moon),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SunArc(
                sunriseTime = sunriseTime,
                sunsetTime = sunsetTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SunMoonItem(
                    icon = Icons.Rounded.WbSunny,
                    label = stringResource(R.string.sunrise),
                    value = sunriseTime
                )
                SunMoonItem(
                    icon = Icons.Rounded.NightsStay,
                    label = stringResource(R.string.sunset),
                    value = sunsetTime
                )
            }
        }
    }
}

@Composable
private fun SunArc(
    sunriseTime: String,
    sunsetTime: String,
    modifier: Modifier = Modifier
) {
    val now = java.time.LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute

    val (sunriseMinutes, sunsetMinutes) = remember(sunriseTime, sunsetTime) {
        try {
            val sr = java.time.LocalTime.parse(sunriseTime, DateTimeFormatter.ofPattern("HH:mm"))
            val ss = java.time.LocalTime.parse(sunsetTime, DateTimeFormatter.ofPattern("HH:mm"))
            sr.hour * 60 + sr.minute to ss.hour * 60 + ss.minute
        } catch (e: Exception) { 360 to 1080 }
    }

    val dayDuration = (sunsetMinutes - sunriseMinutes).coerceAtLeast(1)
    val dayProgress = ((currentMinutes - sunriseMinutes).toFloat() / dayDuration).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val arcHeight = height * 0.7f
        val centerY = height * 0.85f

        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(0f, centerY - arcHeight),
            size = Size(width, arcHeight * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        val sunX = when {
            dayProgress <= 0f -> width * 0.1f
            dayProgress >= 1f -> width * 0.9f
            else -> {
                val angle = Math.toRadians((180 + dayProgress * 180).toDouble())
                width * 0.5f + Math.cos(angle) * width * 0.4f
            }
        }.toFloat()

        val sunY = when {
            dayProgress <= 0f || dayProgress >= 1f -> centerY - 10f
            else -> {
                val angle = Math.toRadians((180 + dayProgress * 180).toDouble())
                centerY - Math.sin(angle) * arcHeight
            }
        }.toFloat()

        drawCircle(
            color = AmberGlow.copy(alpha = 0.85f),
            radius = 10f,
            center = Offset(sunX, sunY)
        )

        drawCircle(
            color = AmberGlow.copy(alpha = 0.2f),
            radius = 16f,
            center = Offset(sunX, sunY)
        )

        val sunriseX = width * 0.1f
        drawCircle(
            color = AmberGlow.copy(alpha = 0.5f),
            radius = 5f,
            center = Offset(sunriseX, centerY)
        )

        val sunsetX = width * 0.9f
        drawCircle(
            color = WarmOrange.copy(alpha = 0.5f),
            radius = 5f,
            center = Offset(sunsetX, centerY)
        )
    }
}

@Composable
private fun SunMoonItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AmberGlow,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private fun isIdealWeather(info: WeatherInfo): Boolean {
    val tempF = if (info.current.temperature < 30) {
        info.current.temperature * 9 / 5 + 32
    } else {
        info.current.temperature
    }
    return info.current.weatherCode == 0 &&
            info.current.isDay &&
            tempF in 65.0..80.0 &&
            (info.current.humidity ?: 50.0) < 50
}

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val confettiColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFD93D),
        Color(0xFF6BCB77),
        Color(0xFF4D96FF),
        Color(0xFFFF6FB5),
        Color(0xFFFFA07A)
    )

    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.5f,
                size = 4f + Random.nextFloat() * 6f,
                color = confettiColors.random(),
                sway = 20f + Random.nextFloat() * 30f,
                swaySpeed = 1f + Random.nextFloat() * 2f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val y = ((progress * p.speed + p.y) % 1.2f - 0.1f) * h
            val swayX = Math.sin((progress * p.swaySpeed * 2 * Math.PI)).toFloat() * p.sway
            val x = p.x * w + swayX

            drawRect(
                color = p.color.copy(alpha = 0.8f),
                topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                size = Size(p.size, p.size * 0.6f)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val sway: Float,
    val swaySpeed: Float
)

// ============================================================
// LOADING & ERROR — polished
// ============================================================

@Composable
private fun LoadingState() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.04f),
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.04f)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val color = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize().semantics(mergeDescendants = true) {
        contentDescription = "Loading weather data"
    }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar skeleton
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonPill(width = 120.dp, height = 16.dp, shimmerColors, translateX)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonCircle(36.dp, shimmerColors, translateX)
                        SkeletonCircle(36.dp, shimmerColors, translateX)
                        SkeletonCircle(36.dp, shimmerColors, translateX)
                    }
                }
            }
            // Hero skeleton — city + temp + desc
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SkeletonPill(width = 140.dp, height = 14.dp, shimmerColors, translateX)
                    Spacer(modifier = Modifier.height(24.dp))
                    SkeletonCircle(56.dp, shimmerColors, translateX)
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonPill(width = 160.dp, height = 48.dp, shimmerColors, translateX)
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonPill(width = 120.dp, height = 16.dp, shimmerColors, translateX)
                    Spacer(modifier = Modifier.height(10.dp))
                    SkeletonPill(width = 180.dp, height = 14.dp, shimmerColors, translateX)
                }
            }
            // Hourly forecast skeleton
            item {
                SkeletonCard(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shimmerColors = shimmerColors,
                    translateX = translateX,
                    height = 140.dp
                )
            }
            // Daily forecast skeleton
            item {
                SkeletonCard(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shimmerColors = shimmerColors,
                    translateX = translateX,
                    height = 220.dp
                )
            }
            // Weather metrics skeleton
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCard(
                        shimmerColors = shimmerColors,
                        translateX = translateX,
                        height = 140.dp,
                        modifier = Modifier.weight(1f)
                    )
                    SkeletonCard(
                        shimmerColors = shimmerColors,
                        translateX = translateX,
                        height = 140.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                SkeletonCard(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shimmerColors = shimmerColors,
                    translateX = translateX,
                    height = 100.dp
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SkeletonCard(
    modifier: Modifier = Modifier,
    shimmerColors: List<Color>,
    translateX: Float,
    height: Dp = 80.dp
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(translateX - 300f, 0f),
                        end = Offset(translateX + 300f, 0f)
                    )
                )
        )
    }
}

@Composable
private fun SkeletonPill(
    width: Dp,
    height: Dp,
    shimmerColors: List<Color>,
    translateX: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateX - 300f, 0f),
                    end = Offset(translateX + 300f, 0f)
                )
            )
    )
}

@Composable
private fun SkeletonCircle(
    size: Dp,
    shimmerColors: List<Color>,
    translateX: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateX - 300f, 0f),
                    end = Offset(translateX + 300f, 0f)
                )
            )
    )
}

// ============================================================
// ANIMATED TEMPERATURE (item 2)
// ============================================================

@Composable
private fun AnimatedTemperatureText(
    temperature: Double,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val targetTemp = convertTemp(temperature, isCelsius)
    val animatedTemp by animateIntAsState(
        targetValue = targetTemp,
        animationSpec = tween(durationMillis = 600),
        label = "temp_anim"
    )
    Text(
        text = "${animatedTemp}°",
        modifier = modifier,
        fontSize = 96.sp,
        fontWeight = FontWeight.Thin,
        color = Color.White,
        lineHeight = 96.sp,
        textAlign = TextAlign.Center
    )
}

// ============================================================
// STAGGERED ENTRY WRAPPER (item 8)
// ============================================================

@Composable
private fun StaggeredEntry(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Simplified: instant render, no animation delays that cause blank scroll spaces
    content()
}

// ============================================================
// ENHANCED CARD PRESS STATE (item 7)
// ============================================================

@Composable
private fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 0f else 2f,
        animationSpec = tween(150),
        label = "press_elevation"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation
            }
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Error: $message"
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = message,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                val retryLabel = stringResource(R.string.retry_loading)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .semantics { contentDescription = retryLabel }
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.retry_loading),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
