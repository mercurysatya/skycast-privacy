package com.vayu.weather.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.SunsetRed
import com.vayu.weather.ui.theme.WarmOrange
import kotlin.math.roundToInt

/**
 * Rich Favorites card.
 *
 * Shows the city, current temperature, condition, H/L, precipitation
 * probability, an AQI placeholder, and an optional severe-alert badge.
 * Tapping the card triggers [onClick]; the trailing X button calls
 * [onRemove] when the user wants to drop the city from favorites.
 */
@Composable
fun SkyCastFavoriteCard(
    cityName: String,
    region: String?,
    country: String?,
    weather: WeatherInfo?,
    isCelsius: Boolean,
    isLoading: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = if (weather != null) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(SkyCastTokens.RadiusXl))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = SkyBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = listOfNotNull(region, country).joinToString(", ")
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
                if (hasAlert) {
                    AlertBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (onRemove != null) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onRemove)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            when {
                isLoading && weather == null -> {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                weather == null -> {
                    Text(
                        text = "Tap to retry",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                else -> FavoriteWeatherRow(weather = weather, isCelsius = isCelsius)
            }
        }
    }
}

@Composable
private fun FavoriteWeatherRow(weather: WeatherInfo, isCelsius: Boolean) {
    val current = weather.current
    val today = weather.daily.firstOrNull()
    val condition = localizedWeatherDescription(current.weatherCode, current.isDay)
    val temp = convertTemp(current.temperature, isCelsius)
    val feels = current.apparentTemperature?.let { convertTemp(it, isCelsius) }
    val high = today?.maxTemp?.let { convertTemp(it, isCelsius) }
    val low = today?.minTemp?.let { convertTemp(it, isCelsius) }
    val rain = weather.hourly.firstOrNull()?.precipitationProbability ?: 0
    val wind = current.windSpeed?.roundToInt()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = getWeatherIcon(current.weatherCode, current.isDay),
            contentDescription = condition,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$temp°",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = condition,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            if (high != null && low != null) {
                Text(
                    text = "H $high° / L $low°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            if (feels != null) {
                Text(
                    text = "Feels $feels°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (rain > 0) {
            StatPill(
                icon = Icons.Rounded.WaterDrop,
                text = "$rain%",
                color = SkyBlue
            )
        }
        if (wind != null) {
            StatPill(
                icon = Icons.Rounded.Air,
                text = "$wind km/h",
                color = com.vayu.weather.ui.theme.SoftLavender
            )
        }
        if (today?.uvIndex != null) {
            StatPill(
                icon = Icons.Rounded.WbSunny,
                text = "UV ${today.uvIndex.roundToInt()}",
                color = WarmOrange
            )
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AlertBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SunsetRed.copy(alpha = 0.20f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = SunsetRed,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Alert",
            style = MaterialTheme.typography.labelSmall,
            color = SunsetRed,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
