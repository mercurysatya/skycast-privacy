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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.vayu.weather.R
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherDescription
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.ads.AdBanner
import com.vayu.weather.presentation.components.AirQualityCard
import com.vayu.weather.presentation.components.WeatherBackground
import com.vayu.weather.presentation.components.WeatherTrends
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private object ConversionConstants {
    const val KPH_TO_MPH = 0.621371
    const val KPH_TO_MS = 1.0 / 3.6
    const val KPH_TO_KNOTS = 0.539957
    const val CELSIUS_TO_FAHRENHEIT_FACTOR = 9.0 / 5.0
    const val FAHRENHEIT_OFFSET = 32.0
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        TopBar(
                            onToggleUnit = onToggleUnit,
                            onOpenSettings = onOpenSettings,
                            onOpenAlerts = onOpenAlerts,
                            onOpenHistory = onOpenHistory,
                            onShare = onShare
                        )
                    }

                    item {
                        HeroSection(
                            info = info,
                            cityName = cityName,
                            isCelsius = isCelsius,
                            onClick = onOpenDetail
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        HourlyForecastSection(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        DailyForecastSection(
                            dailyData = info.daily,
                            isCelsius = isCelsius
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        WeatherDetailsSection(
                            info = info,
                            isCelsius = isCelsius,
                            windUnit = settings.windUnit
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        AIInsightsCard(
                            info = info,
                            isCelsius = isCelsius
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        WeatherTrends(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SunMoonSection(dailyData = info.daily)
                    }

                    if (state.airQuality != null) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            AirQualityCard(
                                airQuality = state.airQuality,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AdBanner()
                    }
                }
            }
        }

        if (state.isLoading) {
            LoadingState()
        }

        state.error?.let {
            ErrorState(message = it, onRetry = onRetry)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================================
// TOP BAR
// ============================================================

@Composable
private fun TopBar(
    onToggleUnit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHistory: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = true)
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenAlerts) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = stringResource(R.string.weather_alerts),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onOpenHistory) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = stringResource(R.string.weather_history_title),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.share_weather),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onToggleUnit) {
                Text(
                    text = "°",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ============================================================
// HERO SECTION
// ============================================================

@Composable
private fun HeroSection(
    info: WeatherInfo,
    cityName: String?,
    isCelsius: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hero_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.12f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cityName ?: stringResource(R.string.default_city_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "${convertTemp(info.current.temperature, isCelsius)}°",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White,
                    lineHeight = 96.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f)
                )

                info.daily.firstOrNull()?.let { today ->
                    val high = convertTemp(today.maxTemp, isCelsius)
                    val low = convertTemp(today.minTemp, isCelsius)
                    Text(
                        text = "H:${high}°  L:${low}°",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                info.current.apparentTemperature?.let { apparent ->
                    Text(
                        text = stringResource(R.string.feels_like, convertTemp(apparent, isCelsius)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// HOURLY FORECAST
// ============================================================

@Composable
private fun HourlyForecastSection(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(hourlyData.sortedBy { it.time }.take(24)) { hour ->
                    HourlyPillCard(data = hour, isCelsius = isCelsius)
                }
            }
        }
    }
}

@Composable
private fun HourlyPillCard(
    data: HourlyWeather,
    isCelsius: Boolean
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

    Column(
        modifier = Modifier
            .width(64.dp)
            .then(
                if (isNow) Modifier.background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(20.dp)
                ) else Modifier
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (isNow) 1f else 0.7f),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${convertTemp(data.temperature, isCelsius)}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ============================================================
// DAILY FORECAST
// ============================================================

@Composable
private fun DailyForecastSection(
    dailyData: List<DailyWeather>,
    isCelsius: Boolean
) {
    if (dailyData.isEmpty()) return
    val allMin = remember(dailyData) { dailyData.minOf { it.minTemp }.roundToInt() }
    val allMax = remember(dailyData) { dailyData.maxOf { it.maxTemp }.roundToInt() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.seven_day_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )

            dailyData.forEach { day ->
                DailyRow(
                    data = day,
                    isCelsius = isCelsius,
                    globalMin = allMin,
                    globalMax = allMax
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
    globalMax: Int
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(72.dp),
            maxLines = 1
        )

        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = precipText,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4FC3F7),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${minTemp}°",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(32.dp),
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
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
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
        val trackColor = Color.White.copy(alpha = 0.15f)
        val warmColor = Color(0xFFFFB74D)
        val coolColor = Color(0xFF4FC3F7)
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
// WEATHER DETAILS
// ============================================================

@Composable
private fun WeatherDetailsSection(
    info: WeatherInfo,
    isCelsius: Boolean,
    windUnit: WindUnit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.weather_details),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(
                    icon = Icons.Rounded.WaterDrop,
                    label = stringResource(R.string.humidity),
                    value = "${info.current.humidity?.roundToInt() ?: "--"}%",
                    modifier = Modifier.weight(1f)
                )
                DetailCard(
                    icon = Icons.Rounded.Air,
                    label = stringResource(R.string.wind),
                    value = "${convertWind(info.current.windSpeed, windUnit)} ${windUnitLabel(windUnit)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(
                    icon = Icons.Rounded.Thermostat,
                    label = stringResource(R.string.pressure),
                    value = info.current.surfacePressure?.let { "${it.roundToInt()} hPa" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                DetailCard(
                    icon = Icons.Rounded.Visibility,
                    label = stringResource(R.string.visibility),
                    value = info.current.visibility?.let { "${(it / 1000).roundToInt()} km" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(
                    icon = Icons.Rounded.WbSunny,
                    label = stringResource(R.string.uv_index),
                    value = "${info.daily.firstOrNull()?.uvIndex?.roundToInt() ?: "--"}",
                    modifier = Modifier.weight(1f)
                )
                DetailCard(
                    icon = Icons.Rounded.AcUnit,
                    label = stringResource(R.string.dew_point),
                    value = info.current.dewPoint?.let { "${convertTemp(it, isCelsius)}°" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
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
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFFD54F)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_insights),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            insights.forEach { insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when (insight.severity) {
                                    InsightSeverity.WARNING -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                                    InsightSeverity.ALERT -> Color(0xFFEF5350).copy(alpha = 0.2f)
                                    InsightSeverity.POSITIVE -> Color(0xFF66BB6A).copy(alpha = 0.2f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = insight.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (insight.severity) {
                                InsightSeverity.WARNING -> Color(0xFFFFB74D)
                                InsightSeverity.ALERT -> Color(0xFFEF5350)
                                InsightSeverity.POSITIVE -> Color(0xFF66BB6A)
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.sun_and_moon),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
private fun SunMoonItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(24.dp)
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
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ============================================================
// LOADING & ERROR
// ============================================================

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading_weather),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(onClick = onRetry, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.retry_loading),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
