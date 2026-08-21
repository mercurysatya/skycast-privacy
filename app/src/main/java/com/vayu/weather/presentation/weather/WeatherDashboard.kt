package com.vayu.weather.presentation.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.vayu.weather.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    const val METERS_TO_KM = 1000.0
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

private fun getWeatherDescription(weatherCode: Int, isDay: Boolean): String {
    return WeatherDescription.getWeatherDescription(weatherCode, isDay)
}

@Composable
private fun localizedWeatherDescription(weatherCode: Int, isDay: Boolean): String {
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

    Box(
        modifier = modifier.fillMaxSize()
    ) {
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
                            cityName = cityName,
                            isCelsius = isCelsius,
                            onToggleUnit = onToggleUnit,
                            onOpenSettings = onOpenSettings,
                            onOpenAlerts = onOpenAlerts,
                            onShare = onShare
                        )
                    }

                    item {
                        CurrentWeatherSection(
                            info = info,
                            isCelsius = isCelsius,
                            onClick = onOpenDetail
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        InfoGrid(
                            info = info,
                            isCelsius = isCelsius,
                            windUnit = settings.windUnit
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        HourlyForecast(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        WeatherTrends(
                            hourlyData = info.hourly,
                            isCelsius = isCelsius,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        DailyForecast(
                            dailyData = info.daily,
                            isCelsius = isCelsius
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        SunMoonSection(
                            dailyData = info.daily
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        AirQualityCard(
                            airQuality = state.airQuality,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        AdBanner()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (state.isLoading) {
            LoadingSkeleton()
        }

        state.error?.let {
            ErrorState(
                message = it,
                onRetry = onRetry
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopBar(
    cityName: String?,
    isCelsius: Boolean,
    onToggleUnit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlerts: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (cityName != null) cityName else stringResource(R.string.default_city_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onOpenAlerts) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = stringResource(R.string.weather_alerts),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.share_weather),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onToggleUnit) {
                Text(
                    text = if (isCelsius) stringResource(R.string.celsius_label) else stringResource(R.string.fahrenheit_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CurrentWeatherSection(
    info: WeatherInfo,
    isCelsius: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = getWeatherIcon(info.current.weatherCode, info.current.isDay),
            contentDescription = getWeatherDescription(info.current.weatherCode, info.current.isDay),
            modifier = Modifier.size(72.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${convertTemp(info.current.temperature, isCelsius)}\u00B0",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        info.current.apparentTemperature?.let {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.feels_like, convertTemp(it, isCelsius)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun HourlyForecast(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.hourly_forecast),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    with(MaterialTheme.colorScheme) {
                        val fadeColor = surface
                        Modifier.drawWithContent {
                            drawContent()
                            val fadeWidth = size.width * 0.08f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        fadeColor.copy(alpha = 0.6f)
                                    ),
                                    startX = size.width - fadeWidth * 2,
                                    endX = size.width
                                )
                            )
                        }
                    }
                )
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(hourlyData.sortedBy { it.time }.take(24)) { hour ->
                    HourlyCard(
                        data = hour,
                        isCelsius = isCelsius
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyCard(
    data: HourlyWeather,
    isCelsius: Boolean
) {
    val currentHour = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
    val isNow = data.time == currentHour
    val containerAlpha = if (isNow) 0.9f else 0.5f
    val borderColor = if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Transparent

    Card(
        modifier = Modifier
            .width(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timeLabel = try {
                java.time.LocalDateTime.parse(data.time, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                data.time.takeLast(5)
            }

            Text(
                text = if (isNow) stringResource(R.string.now) else timeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                imageVector = getWeatherIcon(data.weatherCode, true),
                contentDescription = localizedWeatherDescription(data.weatherCode, true),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${convertTemp(data.temperature, isCelsius)}\u00B0",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TempRangeBar(
    globalMin: Int, globalMax: Int,
    dayMin: Int, dayMax: Int,
    modifier: Modifier = Modifier
) {
    val range = (globalMax - globalMin).toFloat()
    val startFraction = if (range == 0f) 0f else ((dayMin - globalMin) / range).coerceIn(0f, 1f)
    val widthFraction = if (range == 0f) 1f else ((dayMax - dayMin) / range).coerceIn(0.05f, 1f)

    Canvas(modifier = modifier) {
        val trackColor = Color.White.copy(alpha = 0.15f)
        val barColor = Color(0xFFF97316)
        drawRoundRect(color = trackColor, cornerRadius = CornerRadius(4f))
        drawRoundRect(
            color = barColor,
            topLeft = Offset(size.width * startFraction, 0f),
            size = Size(size.width * widthFraction, size.height),
            cornerRadius = CornerRadius(4f)
        )
    }
}

@Composable
private fun DailyForecast(
    dailyData: List<DailyWeather>,
    isCelsius: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.seven_day_forecast),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            val allMin = remember(dailyData) {
                dailyData.minOf { it.minTemp }.roundToInt()
            }
            val allMax = remember(dailyData) {
                dailyData.maxOf { it.maxTemp }.roundToInt()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                dailyData.forEach { day ->
                    DailyItem(
                        data = day,
                        isCelsius = isCelsius,
                        allMin = allMin,
                        allMax = allMax
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyItem(
    data: DailyWeather,
    isCelsius: Boolean,
    allMin: Int,
    allMax: Int
) {
    val dateLabel: String = remember(data.date) {
        val date = try {
            java.time.LocalDate.parse(data.date)
        } catch (e: Exception) {
            java.time.LocalDate.now()
        }
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)
        when (date) {
            today -> "Today"
            tomorrow -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE"))
        }
    }

    val minTemp = convertTemp(data.minTemp, isCelsius)
    val maxTemp = convertTemp(data.maxTemp, isCelsius)

    val mutedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    val rainTextColor = MaterialTheme.colorScheme.primary
    val smallStyle = MaterialTheme.typography.bodySmall
    val labelStyle = MaterialTheme.typography.bodyMedium

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = dateLabel,
            modifier = Modifier.width(80.dp),
            style = labelStyle,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Icon(
            imageVector = getWeatherIcon(data.weatherCode, true),                contentDescription = "$dateLabel: ${localizedWeatherDescription(data.weatherCode, true)}",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = "$minTemp\u00B0",
            color = mutedTextColor,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
            style = smallStyle
        )

        Spacer(Modifier.width(4.dp))

        TempRangeBar(
            globalMin = allMin, globalMax = allMax,
            dayMin = convertTemp(data.minTemp, true).coerceAtMost(convertTemp(data.maxTemp, true)),
            dayMax = convertTemp(data.maxTemp, true).coerceAtLeast(convertTemp(data.minTemp, true)),
            modifier = Modifier.weight(1f).height(5.dp)
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = "$maxTemp\u00B0",
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
            style = smallStyle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = "${data.precipitationProbability?.let { if (it > 0) "$it%" else "--" } ?: "--"}",
            color = rainTextColor,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            style = smallStyle
        )
    }
}

@Composable
private fun InfoGrid(
    info: WeatherInfo,
    isCelsius: Boolean,
    windUnit: WindUnit = WindUnit.KPH
) {
    val windValue = "${convertWind(info.current.windSpeed, windUnit)} ${windUnitLabel(windUnit)}"
    val visibilityValue = info.current.visibility?.let { "${(it / 1000).roundToInt()} km" }
    val pressureValue = info.current.surfacePressure?.let { "${it.roundToInt()} hPa" }
    val windGustValue = info.current.windGusts?.let { "${convertWind(it, windUnit)} ${windUnitLabel(windUnit)}" }
    val dewPointValue = info.current.dewPoint?.let { "${convertTemp(it, isCelsius)}\u00B0" }

    val hasVisibility = visibilityValue != null
    val hasPressure = pressureValue != null
    val hasWindGust = windGustValue != null
    val hasDewPoint = dewPointValue != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoCard(
                data = InfoItemData(stringResource(R.string.humidity), "${info.current.humidity?.roundToInt() ?: "--"}%", Icons.Rounded.WaterDrop),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                data = InfoItemData(stringResource(R.string.wind), windValue, Icons.Rounded.Air),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                data = InfoItemData(stringResource(R.string.uv_index), "${info.daily.firstOrNull()?.uvIndex?.roundToInt() ?: "--"}", Icons.Rounded.WbSunny),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasVisibility) {
                InfoCard(
                    data = InfoItemData(stringResource(R.string.visibility), visibilityValue!!, Icons.Rounded.Visibility),
                    modifier = Modifier.weight(1f)
                )
            }
            if (hasPressure) {
                InfoCard(
                    data = InfoItemData(stringResource(R.string.pressure), pressureValue!!, Icons.Rounded.Speed),
                    modifier = Modifier.weight(1f)
                )
            }
            if (hasWindGust) {
                InfoCard(
                    data = InfoItemData(stringResource(R.string.wind_gust), windGustValue!!, Icons.Rounded.Air),
                    modifier = Modifier.weight(1f)
                )
            }
            if (hasDewPoint) {
                InfoCard(
                    data = InfoItemData(stringResource(R.string.dew_point), dewPointValue!!, Icons.Rounded.AcUnit),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class InfoItemData(
    val title: String,
    val value: String,
    val icon: ImageVector
)

@Composable
private fun InfoCard(
    data: InfoItemData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Icon(
            imageVector = data.icon,
            contentDescription = data.title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = data.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SunMoonSection(
    dailyData: List<DailyWeather>
) {
    val today = dailyData.firstOrNull() ?: return
    val sunrise = today.sunrise ?: return
    val sunset = today.sunset ?: return

    val sunriseTime = remember(sunrise) {
        try {
            java.time.LocalTime.parse(sunrise.substringAfter("T"))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { sunrise }
    }
    val sunsetTime = remember(sunset) {
        try {
            java.time.LocalTime.parse(sunset.substringAfter("T"))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { sunset }
    }

    val now = java.time.LocalTime.now()
    val sunriseLocal = try { java.time.LocalTime.parse(sunrise.substringAfter("T")) } catch (e: Exception) { null }
    val sunsetLocal = try { java.time.LocalTime.parse(sunset.substringAfter("T")) } catch (e: Exception) { null }

    val daylightStatus = remember(now, sunriseLocal, sunsetLocal) {
        when {
            sunriseLocal == null || sunsetLocal == null -> "Unknown"
            now.isBefore(sunriseLocal) -> "Before sunrise"
            now.isAfter(sunsetLocal) -> "After sunset"
            else -> "Daylight"
        }
    }

    val remainingDaylight = remember(now, sunsetLocal) {
        if (sunsetLocal == null) null
        else {
            val minutes = java.time.Duration.between(now, sunsetLocal).toMinutes()
            if (minutes < 0) null
            else {
                val hours = minutes / 60
                val mins = minutes % 60
                "${hours}h ${mins}m remaining"
            }
        }
    }

    val daylightDuration = remember(sunriseLocal, sunsetLocal) {
        if (sunriseLocal == null || sunsetLocal == null) null
        else {
            val minutes = java.time.Duration.between(sunriseLocal, sunsetLocal).toMinutes()
            val hours = minutes / 60
            val mins = minutes % 60
            "${hours}h ${mins}m"
        }
    }

    val goldenHourMorning = remember(sunriseLocal) {
        sunriseLocal?.plusMinutes(30)?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
    val goldenHourEvening = remember(sunsetLocal) {
        sunsetLocal?.minusMinutes(30)?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.sun_and_moon),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SunMoonItem(
                        icon = Icons.Rounded.WbSunny,
                        label = stringResource(R.string.sunrise),
                        value = sunriseTime,
                        subValue = daylightStatus
                    )
                    SunMoonItem(
                        icon = Icons.Rounded.NightsStay,
                        label = stringResource(R.string.sunset),
                        value = sunsetTime,
                        subValue = remainingDaylight ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SunMoonItem(
                        icon = Icons.Rounded.LightMode,
                        label = stringResource(R.string.day_length),
                        value = daylightDuration ?: "--"
                    )
                    SunMoonItem(
                        icon = Icons.Rounded.WbTwilight,
                        label = stringResource(R.string.golden_hour),
                        value = goldenHourMorning ?: "--",
                        subValue = goldenHourEvening?.let { stringResource(R.string.evening_time, it) } ?: ""
                    )
                }
            }
        }
    }
}

@Composable
private fun SunMoonItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        if (subValue.isNotBlank()) {
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading_weather),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(48.dp)
            ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.retry_loading),
                tint = MaterialTheme.colorScheme.primary
            )
            }
        }
    }
}
