package com.vayu.weather.presentation.components.skycast

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * SkyCast 7-day forecast.
 *
 * Each day is a row showing day name, icon, min/max and a precipitation bar.
 * Tapping a row expands a Daily Details panel with morning/afternoon/evening/
 * night summaries, a temperature strip and sunrise/sunset.
 */
@Composable
fun SkyCastDailyForecast(
    daily: List<DailyWeather>,
    hourly: List<HourlyWeather>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    if (daily.isEmpty()) return
    var expanded by remember { mutableStateOf<String?>(null) }

    val allMin = convertTemp(daily.minOf { it.minTemp }, isCelsius)
    val allMax = convertTemp(daily.maxOf { it.maxTemp }, isCelsius)

    SkyCastCard(
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkyCastSectionHeader(
                title = "7-day forecast",
                subtitle = "Tap a day for hourly details"
            )
            Spacer(modifier = Modifier.height(8.dp))

            daily.forEachIndexed { index, day ->
                val isExpanded = expanded == day.date
                val today = LocalDate.now()
                val date = try { LocalDate.parse(day.date) } catch (e: Exception) { today }
                val label = when (date) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> date.format(DateTimeFormatter.ofPattern("EEE"))
                }

                Column {
                    DailyRow(
                        label = label,
                        day = day,
                        allMin = allMin,
                        allMax = allMax,
                        isCelsius = isCelsius,
                        isExpanded = isExpanded,
                        onClick = {
                            expanded = if (isExpanded) null else day.date
                        }
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val filteredHourly = remember(day.date, hourly.size) {
                            hourly.filter { it.time.startsWith(day.date) }
                        }
                        DailyDetails(
                            day = day,
                            hourly = filteredHourly,
                            isCelsius = isCelsius
                        )
                    }
                    if (index < daily.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRow(
    label: String,
    day: DailyWeather,
    allMin: Int,
    allMax: Int,
    isCelsius: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val high = convertTemp(day.maxTemp, isCelsius)
    val low = convertTemp(day.minTemp, isCelsius)
    val highLabel = "${high}°"
    val lowLabel = "${low}°"
    val weatherDesc = localizedWeatherDescription(day.weatherCode, true)
    val range = (allMax - allMin).coerceAtLeast(1)
    val startFraction = ((convertTemp(day.minTemp, isCelsius) - allMin).toFloat() / range).coerceIn(0f, 1f)
    val endFraction = ((convertTemp(day.maxTemp, isCelsius) - allMin).toFloat() / range).coerceIn(0f, 1f)
    val barWidth = (endFraction - startFraction).coerceAtLeast(0.06f)
    val rain = day.precipitationProbability ?: 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $weatherDesc, high $highLabel, low $lowLabel"
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = if (label == "Today") FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.width(76.dp)
        )
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getWeatherIcon(day.weatherCode, true),
                contentDescription = localizedWeatherDescription(day.weatherCode, true),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Rain probability
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(40.dp)
        ) {
            if (rain > 0) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = SkyBlue,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "$rain%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyBlue
                )
            }
        }
        // Min temp
        Text(
            text = "${low}°",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.width(34.dp)
        )
        // Range bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barWidth)
                    .height(6.dp)
                    .padding(start = 0.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(SkyBlue, com.vayu.weather.ui.theme.WarmOrange)
                        )
                    )
            )
        }
        // Max temp
        Text(
            text = "${high}°",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(34.dp)
        )
    }
}

@Composable
private fun DailyDetails(
    day: DailyWeather,
    hourly: List<HourlyWeather>,
    isCelsius: Boolean
) {
    val (morning, afternoon, evening, night) = partitionByPeriod(hourly)
    val periods = listOf(
        "Morning" to morning,
        "Afternoon" to afternoon,
        "Evening" to evening,
        "Night" to night
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    ) {
        // Summary chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { (name, list) ->
                if (list.isNotEmpty()) {
                    val avg = list.map { it.temperature }.average()
                    val code = list.groupingBy { it.weatherCode }.eachCount().maxByOrNull { it.value }?.key ?: 0
                    PeriodChip(
                        name = name,
                        temp = convertTemp(avg, isCelsius),
                        weatherCode = code,
                        isDay = name != "Night"
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Hourly strip
        if (hourly.isNotEmpty()) {
            val scroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hourly.forEach { h ->
                    val time = try {
                        java.time.LocalTime.parse(h.time.substringAfter("T"))
                    } catch (e: Exception) {
                        null
                    }
                    val label = time?.format(DateTimeFormatter.ofPattern("ha")) ?: h.time.takeLast(5)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = getWeatherIcon(h.weatherCode, true),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${convertTemp(h.temperature, isCelsius)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        if ((h.precipitationProbability ?: 0) > 0) {
                            Text(
                                text = "${h.precipitationProbability}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyBlue,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // Sunrise / sunset row
        if (day.sunrise != null && day.sunset != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayExtra(label = "Sunrise", value = day.sunrise.take(5))
                DayExtra(label = "Sunset", value = day.sunset.take(5))
                DayExtra(
                    label = "UV",
                    value = day.uvIndex?.let { "${it.roundToInt()}" } ?: "—"
                )
                DayExtra(
                    label = "Rain",
                    value = "${day.precipitationProbability ?: 0}%"
                )
            }
        }
    }
}

@Composable
private fun PeriodChip(name: String, temp: Int, weatherCode: Int, isDay: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Icon(
            imageVector = getWeatherIcon(weatherCode, isDay),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${temp}°",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayExtra(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private data class PeriodLists(
    val morning: List<HourlyWeather>,
    val afternoon: List<HourlyWeather>,
    val evening: List<HourlyWeather>,
    val night: List<HourlyWeather>
)

private fun partitionByPeriod(hourly: List<HourlyWeather>): PeriodLists {
    val morning = mutableListOf<HourlyWeather>()
    val afternoon = mutableListOf<HourlyWeather>()
    val evening = mutableListOf<HourlyWeather>()
    val night = mutableListOf<HourlyWeather>()
    hourly.forEach { h ->
        val hour = try {
            java.time.LocalTime.parse(h.time.substringAfter("T")).hour
        } catch (e: Exception) { 12 }
        when (hour) {
            in 5..11 -> morning += h
            in 12..16 -> afternoon += h
            in 17..20 -> evening += h
            else -> night += h
        }
    }
    return PeriodLists(morning, afternoon, evening, night)
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
