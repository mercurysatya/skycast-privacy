package com.vayu.weather.presentation.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vayu.weather.R
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.components.AirQualityCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    state: WeatherState,
    settings: SettingsState,
    cityName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCelsius = settings.temperatureUnit == TemperatureUnit.CELSIUS
    val info = state.weatherInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName ?: stringResource(R.string.default_city_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (info == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Current conditions - expanded
                item {
                    CurrentConditionsExpanded(
                        info = info,
                        isCelsius = isCelsius,
                        settings = settings
                    )
                }

                // Hourly forecast - full vertical list
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HourlyForecastExpanded(
                        hourlyData = info.hourly,
                        isCelsius = isCelsius,
                        windUnit = settings.windUnit
                    )
                }

                // Daily forecast - full detail
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DailyForecastExpanded(
                        dailyData = info.daily,
                        isCelsius = isCelsius
                    )
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
            }
        }
    }
}

@Composable
private fun CurrentConditionsExpanded(
    info: WeatherInfo,
    isCelsius: Boolean,
    settings: SettingsState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = getWeatherIcon(info.current.weatherCode, info.current.isDay),
            contentDescription = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${convertTemp(info.current.temperature, isCelsius)}°",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        info.current.apparentTemperature?.let {
            Text(
                text = stringResource(R.string.feels_like, convertTemp(it, isCelsius)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Expanded info grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailInfoItem(
                        icon = Icons.Rounded.WaterDrop,
                        label = stringResource(R.string.humidity),
                        value = "${info.current.humidity?.roundToInt() ?: "--"}%"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.Air,
                        label = stringResource(R.string.wind),
                        value = "${convertWindRaw(info.current.windSpeed, settings.windUnit)} ${windUnitLabel(settings.windUnit)}"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.Thermostat,
                        label = stringResource(R.string.pressure),
                        value = info.current.surfacePressure?.let { "${it.roundToInt()} hPa" } ?: "--"
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailInfoItem(
                        icon = Icons.Rounded.Visibility,
                        label = stringResource(R.string.visibility),
                        value = info.current.visibility?.let { "${(it / 1000).roundToInt()} km" } ?: "--"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.WbSunny,
                        label = stringResource(R.string.uv_index),
                        value = "${info.daily.firstOrNull()?.uvIndex?.roundToInt() ?: "--"}"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.AcUnit,
                        label = stringResource(R.string.dew_point),
                        value = info.current.dewPoint?.let { "${convertTemp(it, isCelsius)}°" } ?: "--"
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailInfoItem(
                        icon = Icons.Rounded.Air,
                        label = stringResource(R.string.wind_gust),
                        value = info.current.windGusts?.let { "${convertWindRaw(it, settings.windUnit)} ${windUnitLabel(settings.windUnit)}" } ?: "--"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.Compass,
                        label = stringResource(R.string.wind_direction),
                        value = info.current.windDirection?.let { formatWindDirection(it) } ?: "--"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.WbTwilight,
                        label = stringResource(R.string.sunrise),
                        value = info.daily.firstOrNull()?.sunrise?.let { formatTimeFromISO(it) } ?: "--"
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailInfoItem(
                        icon = Icons.Rounded.NightsStay,
                        label = stringResource(R.string.sunset),
                        value = info.daily.firstOrNull()?.sunset?.let { formatTimeFromISO(it) } ?: "--"
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.WbSunny,
                        label = stringResource(R.string.day_length),
                        value = calculateDayLength(
                            info.daily.firstOrNull()?.sunrise,
                            info.daily.firstOrNull()?.sunset
                        )
                    )
                    DetailInfoItem(
                        icon = Icons.Rounded.Umbrella,
                        label = stringResource(R.string.precipitation),
                        value = info.daily.firstOrNull()?.precipitationProbability?.let { "$it%" } ?: "--"
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 1
        )
    }
}

@Composable
private fun HourlyForecastExpanded(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean,
    windUnit: WindUnit
) {
    val currentHour = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:00"))
    }

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            val sortedHourly = remember(hourlyData) {
                hourlyData.sortedBy { it.time }
            }

            sortedHourly.take(24).forEachIndexed { index, hour ->
                val isNow = try {
                    val hourTime = java.time.LocalTime.parse(hour.time.substringAfter("T"))
                    val nowTime = java.time.LocalTime.parse(currentHour)
                    hourTime == nowTime
                } catch (e: Exception) { false }

                Column {
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time
                        Text(
                            text = if (isNow) stringResource(R.string.now) else formatTimeFromISO(hour.time),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(60.dp)
                        )

                        // Weather icon
                        Icon(
                            imageVector = getWeatherIcon(hour.weatherCode, true),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Description
                        Text(
                            text = WeatherDescription.getWeatherDescription(hour.weatherCode, true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Precipitation probability
                        hour.humidity?.let { humidity ->
                            if (humidity > 50) {
                                Icon(
                                    imageVector = Icons.Rounded.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${humidity.roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Temperature
                        Text(
                            text = "${convertTemp(hour.temperature, isCelsius)}°",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastExpanded(
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
            shape = RoundedCornerShape(20.dp)
        ) {
            dailyData.forEachIndexed { index, day ->
                DailyDetailItem(
                    data = day,
                    isCelsius = isCelsius,
                    isFirst = index == 0,
                    isLast = index == dailyData.lastIndex
                )
                if (index < dailyData.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyDetailItem(
    data: DailyWeather,
    isCelsius: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val dateLabel = remember(data.date) {
        val date = try { LocalDate.parse(data.date) } catch (e: Exception) { LocalDate.now() }
        val today = LocalDate.now()
        when (date) {
            today -> stringResource(R.string.today)
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
    }

    val minTemp = convertTemp(data.minTemp, isCelsius)
    val maxTemp = convertTemp(data.maxTemp, isCelsius)
    val precip = data.precipitationProbability

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Day and weather
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(100.dp)
            )

            Icon(
                imageVector = getWeatherIcon(data.weatherCode, true),
                contentDescription = WeatherDescription.getWeatherDescription(data.weatherCode, true),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = WeatherDescription.getWeatherDescription(data.weatherCode, true),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Temperature range + precipitation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Precipitation probability
            if (precip != null && precip > 0) {
                Icon(
                    imageVector = Icons.Rounded.Umbrella,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${precip}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.width(36.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(54.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Min temp
            Text(
                text = "${minTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Temp range bar
            TempRangeBar(
                globalMin = dailyData.minOf { it.minTemp }.roundToInt(),
                globalMax = dailyData.maxOf { it.maxTemp }.roundToInt(),
                dayMin = minTemp,
                dayMax = maxTemp,
                modifier = Modifier
                    .width(100.dp)
                    .height(5.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Max temp
            Text(
                text = "${maxTemp}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }

        // Sunrise/Sunset row
        if (data.sunrise != null || data.sunset != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                data.sunrise?.let { sr ->
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatTimeFromISO(sr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                data.sunset?.let { ss ->
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatTimeFromISO(ss),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                data.uvIndex?.let { uv ->
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "UV ${uv.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// Utility functions
private fun convertTemp(temp: Double, isCelsius: Boolean): Int {
    return if (isCelsius) temp.roundToInt()
    else (temp * 9 / 5 + 32).roundToInt()
}

private fun convertWindRaw(windKph: Double?, unit: WindUnit): String {
    val speed = windKph ?: return "--"
    return when (unit) {
        WindUnit.KPH -> "${speed.roundToInt()}"
        WindUnit.MPH -> "${(speed * 0.621371).roundToInt()}"
        WindUnit.MS -> "${(speed / 3.6).roundToInt()}"
        WindUnit.KNOTS -> "${(speed * 0.539957).roundToInt()}"
    }
}

private fun windUnitLabel(unit: WindUnit): String = when (unit) {
    WindUnit.KPH -> "km/h"
    WindUnit.MPH -> "mph"
    WindUnit.MS -> "m/s"
    WindUnit.KNOTS -> "knots"
}

private fun formatTimeFromISO(isoString: String): String {
    return try {
        val timeStr = isoString.substringAfter("T")
        val time = LocalTime.parse(timeStr)
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        isoString.takeLast(5)
    }
}

private fun formatWindDirection(degrees: Double): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) / 45).toInt() % 8
    return directions[index]
}

private fun calculateDayLength(sunrise: String?, sunset: String?): String {
    if (sunrise == null || sunset == null) return "--"
    return try {
        val sr = LocalTime.parse(sunrise.substringAfter("T"))
        val ss = LocalTime.parse(sunset.substringAfter("T"))
        val minutes = java.time.Duration.between(sr, ss).toMinutes()
        val hours = minutes / 60
        val mins = minutes % 60
        "${hours}h ${mins}m"
    } catch (e: Exception) {
        "--"
    }
}
