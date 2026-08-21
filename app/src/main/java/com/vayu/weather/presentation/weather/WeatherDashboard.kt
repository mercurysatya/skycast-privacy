package com.vayu.weather.presentation.weather

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    return androidx.compose.ui.res.stringResource(resId)
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
                    // Top bar
                    item {
                        TopBar(
                            onToggleUnit = onToggleUnit,
                            onOpenSettings = onOpenSettings,
                            onOpenAlerts = onOpenAlerts,
                            onShare = onShare
                        )
                    }

                    // Hero section - Google Weather style
                    item {
                        HeroSection(
                            info = info,
                            cityName = cityName,
                            isCelsius = isCelsius,
                            onClick = onOpenDetail
                        )
                    }

                    // Hourly forecast
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        HourlyForecastSection(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius
                        )
                    }

                    // Daily forecast
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        DailyForecastSection(
                            dailyData = info.daily,
                            isCelsius = isCelsius
                        )
                    }

                    // Weather details
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        WeatherDetailsSection(
                            info = info,
                            isCelsius = isCelsius,
                            windUnit = settings.windUnit
                        )
                    }

                    // Temperature trends chart
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        WeatherTrends(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Sun & Moon
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SunMoonSection(dailyData = info.daily)
                    }

                    // Air quality
                    if (state.airQuality != null) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            AirQualityCard(
                                airQuality = state.airQuality,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Ad
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
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenAlerts) {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = stringResource(R.string.weather_alerts),
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
// HERO SECTION - Google Weather inspired
// ============================================================

@Composable
private fun HeroSection(
    info: WeatherInfo,
    cityName: String?,
    isCelsius: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // City name
        Text(
            text = cityName ?: stringResource(R.string.default_city_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Huge temperature
        Text(
            text = "${convertTemp(info.current.temperature, isCelsius)}°",
            fontSize = 96.sp,
            fontWeight = FontWeight.Thin,
            color = Color.White,
            lineHeight = 96.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Weather condition
        Text(
            text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.85f)
        )

        // High / Low
        info.daily.firstOrNull()?.let { today ->
            val high = convertTemp(today.maxTemp, isCelsius)
            val low = convertTemp(today.minTemp, isCelsius)
            Text(
                text = "H:${high}°  L:${low}°",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Feels like
        info.current.apparentTemperature?.let { apparent ->
            Text(
                text = stringResource(R.string.feels_like, convertTemp(apparent, isCelsius)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ============================================================
// HOURLY FORECAST - Pill-shaped cards
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
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.hourly_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp, bottom = 12.dp)
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
        // Time
        Text(
            text = if (isNow) stringResource(R.string.now) else {
                try {
                    java.time.LocalDateTime.parse(data.time, DateTimeFormatter.ISO_DATE_TIME)
                        .format(DateTimeFormatter.ofPattern("ha"))
                } catch (e: Exception) { data.time.takeLast(5) }
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (isNow) 1f else 0.7f),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Icon
        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Temperature
        Text(
            text = "${convertTemp(data.temperature, isCelsius)}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ============================================================
// DAILY FORECAST - Google Weather style rows
// ============================================================

@Composable
private fun DailyForecastSection(
    dailyData: List<DailyWeather>,
    isCelsius: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.seven_day_forecast),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 20.dp, bottom = 8.dp)
            )

            val allMin = remember(dailyData) { dailyData.minOf { it.minTemp }.roundToInt() }
            val allMax = remember(dailyData) { dailyData.maxOf { it.maxTemp }.roundToInt() }

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
    val dateLabel = remember(data.date) {
        val date = try { java.time.LocalDate.parse(data.date) } catch (_: Exception) { java.time.LocalDate.now() }
        val today = java.time.LocalDate.now()
        when (date) {
            today -> stringResource(R.string.now)
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE"))
        }
    }

    val minTemp = convertTemp(data.minTemp, isCelsius)
    val maxTemp = convertTemp(data.maxTemp, isCelsius)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(72.dp),
            maxLines = 1
        )

        // Weather icon
        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )

        // Precipitation %
        data.precipitationProbability?.let { precip ->
            if (precip > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$precip%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4FC3F7),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        } ?: Spacer(modifier = Modifier.width(40.dp))

        // Low temp
        Text(
            text = "${minTemp}°",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Temp range bar
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

        // High temp
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
// WEATHER DETAILS - 2-column grid
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
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.weather_details),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 4.dp, bottom = 12.dp)
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
            containerColor = Color.White.copy(alpha = 0.1f)
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
            containerColor = Color.White.copy(alpha = 0.15f)
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
