package com.vayu.weather.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.IconButton
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
import com.vayu.weather.ui.theme.WarmOrange
import androidx.compose.ui.res.stringResource
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
    val shape = RoundedCornerShape(SkyCastTokens.RadiusXl)
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    // The card used to be a near-transparent white gradient with white text,
    // which only worked on the dark weather hero. On the light Favorites
    // screen both the card and its text disappeared. Use theme-aware surface
    // colours so it stays legible in light and dark mode.
    val bg = if (weather != null) {
        Brush.verticalGradient(
            colors = listOf(
                container,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(bg)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = shape
            )
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = listOfNotNull(region, country).joinToString(", ")
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                if (hasAlert) {
                    AlertBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (onRemove != null) {
                    // IconButton provides the 48dp minimum touch target; a bare
                    // 18dp clickable was unusable with TalkBack/touch.
                    IconButton(onClick = onRemove, modifier = Modifier.size(SkyCastTokens.TouchTargetComfortable)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.remove_from_favorites, cityName),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            when {
                isLoading && weather == null -> {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weather == null -> {
                    Text(
                        text = "Tap to retry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$temp°",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = condition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            if (high != null && low != null) {
                Text(
                    text = "H $high° / L $low°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (feels != null) {
                Text(
                    text = "Feels $feels°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AlertBadge() {
    val error = MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(error.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = error,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Alert",
            style = MaterialTheme.typography.labelSmall,
            color = error,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
