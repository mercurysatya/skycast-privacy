package com.vayu.weather.presentation.travel

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.components.skycast.SkyCastCard
import com.vayu.weather.presentation.components.skycast.SkyCastSectionHeader
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.WarmOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * SkyCast Travel Forecast.
 *
 * Pick a destination + travel date and the UI displays a forecast summary
 * with temperature, precipitation, UV, AQI placeholders and a natural-
 * language travel-readiness sentence.
 */
@Composable
fun SkyCastTravelScreen(
    state: TravelViewModel.TravelState,
    onSetDate: (LocalDate) -> Unit,
    onPickDestination: () -> Unit,
    onRefresh: () -> Unit,
    isCelsius: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.Flight, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Travel forecast",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        com.vayu.weather.presentation.ads.AdBanner()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DestinationPicker(destination = state.destination, onClick = onPickDestination) }
            item { DatePicker(travelDate = state.travelDate, onPickDate = onSetDate) }
            if (state.isLoading) {
                item {
                    Text(
                        text = "Loading forecast…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            state.error?.let { err ->
                item {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            if (state.destination != null && state.travelDate != null && state.dailyForDate != null) {
                val dest = state.destination!!
                val date = state.travelDate!!
                val day = state.dailyForDate!!
                item {
                    TravelSummaryCard(
                        destination = dest,
                        date = date,
                        day = day,
                        aqi = state.aqi,
                        isCelsius = isCelsius
                    )
                }
            } else if (!state.isLoading) {
                item {
                    Text(
                        text = "Pick a destination and travel date to see your forecast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationPicker(destination: City?, onClick: () -> Unit) {
    SkyCastCard(
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SkyBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Destination",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = destination?.let { "${it.name}, ${it.country ?: ""}" } ?: "Tap to choose",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DatePicker(travelDate: LocalDate?, onPickDate: (LocalDate) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    SkyCastCard(
        contentPadding = PaddingValues(16.dp),
        onClick = {
            // Cycle through the next 14 days as a simple stand-in for a full date picker.
            val next = (travelDate ?: LocalDate.now()).plusDays(1)
            onPickDate(next)
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = WarmOrange)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Travel date",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = travelDate?.format(DateTimeFormatter.ofPattern("EEE, MMM d")) ?: "Tap to choose",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TravelSummaryCard(
    destination: City,
    date: LocalDate,
    day: DailyWeather,
    aqi: Int?,
    isCelsius: Boolean
) {
    val high = convertTemp(day.maxTemp, isCelsius)
    val low = convertTemp(day.minTemp, isCelsius)
    val rain = day.precipitationProbability ?: 0
    val uv = day.uvIndex?.roundToInt() ?: 0
    val isHot = high >= (if (isCelsius) 32 else 90)
    val isRainy = rain >= 40
    val isCold = low <= (if (isCelsius) 5 else 41)
    val isUvy = uv >= 7

    val ready: Boolean = !isRainy && !isUvy && !isCold

    SkyCastCard(contentPadding = PaddingValues(20.dp)) {
        Column {
            Text(
                text = destination.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getWeatherIcon(day.weatherCode, true),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$high° / $low°",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = localizedWeatherDescription(day.weatherCode, true),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Travel readiness verdict
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (ready) FreshGreen.copy(alpha = 0.15f)
                        else WarmOrange.copy(alpha = 0.15f)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = if (ready) FreshGreen else WarmOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = travelReadiness(destination, day, isCelsius),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TravelStat(icon = Icons.Rounded.WaterDrop, label = "Rain", value = "$rain%")
                TravelStat(icon = Icons.Rounded.WbSunny, label = "UV", value = "$uv")
                TravelStat(icon = Icons.Rounded.LocationOn, label = "AQI", value = aqi?.toString() ?: "—")
            }
        }
    }
}

@Composable
private fun TravelStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
    }
}

private fun travelReadiness(city: City, day: DailyWeather, isCelsius: Boolean): String {
    val high = convertTemp(day.maxTemp, isCelsius)
    val low = convertTemp(day.minTemp, isCelsius)
    val rain = day.precipitationProbability ?: 0
    val parts = mutableListOf<String>()
    parts += "Travel conditions in ${city.name} look ${if (rain < 30) "favorable" else "mixed"}."
    if (rain >= 60) parts += "Expect significant rain — pack waterproofs."
    if (high >= (if (isCelsius) 32 else 90)) parts += "Hot afternoon — light clothing and sunscreen advised."
    if (low <= (if (isCelsius) 5 else 41)) parts += "Cold nights — pack warm layers."
    if ((day.uvIndex ?: 0.0) >= 7) parts += "Strong UV — sunglasses and SPF recommended."
    return parts.joinToString(" ")
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
