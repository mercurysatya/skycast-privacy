package com.vayu.weather.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.WeatherInfo
import kotlin.math.roundToInt

data class WeatherSuggestion(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val priority: Int, // 0 = highest
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun SmartSuggestionsCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier
) {
    val suggestions = generateSuggestions(weatherInfo)
    if (suggestions.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Smart Suggestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.take(4).forEach { suggestion ->
                SuggestionRow(suggestion)
                if (suggestion != suggestions.take(4).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: WeatherSuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(suggestion.color.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = suggestion.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = suggestion.color
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun generateSuggestions(info: WeatherInfo): List<WeatherSuggestion> {
    val suggestions = mutableListOf<WeatherSuggestion>()
    val current = info.current
    val today = info.daily.firstOrNull()
    val temp = current.temperature
    val code = current.weatherCode
    val windSpeed = current.windSpeed ?: 0.0
    val uvIndex = today?.uvIndex ?: 0.0
    val humidity = current.humidity ?: 50.0
    val isRainy = code in 51..82 || code in 95..99
    val isStormy = code in 95..99

    // ── Umbrella ──
    if (isRainy) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.Umbrella,
            title = "Bring an Umbrella",
            description = when {
                code in 63..65 || code in 80..82 -> "Heavy rain expected. Waterproof gear recommended."
                code in 51..55 -> "Light rain likely. An umbrella should be enough."
                else -> "Rain in the forecast. Don't forget your umbrella!"
            },
            priority = 0,
            color = MaterialTheme.colorScheme.primary
        ))
    } else {
        // Check upcoming hours for rain
        val nextRainHours = info.hourly.take(6).count { it.weatherCode in 51..82 }
        if (nextRainHours >= 2) {
            suggestions.add(WeatherSuggestion(
                icon = Icons.Rounded.Umbrella,
                title = "Umbrella Later",
                description = "Rain expected in the next few hours. Carry an umbrella.",
                priority = 1,
                color = MaterialTheme.colorScheme.primary
            ))
        }
    }

    // ── Sunscreen ──
    if (uvIndex >= 3 && current.isDay) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.WbSunny,
            title = when {
                uvIndex >= 8 -> "Extreme UV — Stay in Shade"
                uvIndex >= 6 -> "High UV — Sunscreen Essential"
                uvIndex >= 3 -> "Moderate UV — Apply Sunscreen"
                else -> "UV Protection Recommended"
            },
            description = "UV Index ${uvIndex.roundToInt()}. ${
                when {
                    uvIndex >= 8 -> "Limit outdoor exposure between 10 AM and 4 PM."
                    uvIndex >= 6 -> "SPF 30+ recommended. Reapply every 2 hours."
                    else -> "Wear sunglasses and a hat if outdoors."
                }
            }",
            priority = 0,
            color = MaterialTheme.colorScheme.tertiary
        ))
    }

    // ── Clothing ──
    val clothingSuggestion = when {
        temp >= 38 -> WeatherSuggestion(
            icon = Icons.Rounded.AcUnit,
            title = "Extreme Heat — Stay Cool",
            description = "Wear light, loose clothing. Stay hydrated. Avoid strenuous activity.",
            priority = 0,
            color = MaterialTheme.colorScheme.error
        )
        temp >= 32 -> WeatherSuggestion(
            icon = Icons.Rounded.WbSunny,
            title = "Hot Weather — Light Clothing",
            description = "Cotton/linen recommended. Carry water. Seek shade when possible.",
            priority = 1,
            color = MaterialTheme.colorScheme.tertiary
        )
        temp >= 25 -> WeatherSuggestion(
            icon = Icons.Rounded.WbSunny,
            title = "Warm — Comfortable Clothing",
            description = "Light layers work well. T-shirt weather!",
            priority = 2,
            color = MaterialTheme.colorScheme.secondary
        )
        temp >= 15 -> WeatherSuggestion(
            icon = Icons.Rounded.WaterDrop,
            title = "Mild — Layer Up",
            description = "A light jacket or sweater recommended for the evening.",
            priority = 2,
            color = MaterialTheme.colorScheme.secondary
        )
        temp >= 5 -> WeatherSuggestion(
            icon = Icons.Rounded.AcUnit,
            title = "Cool — Wear a Jacket",
            description = "Warm layers recommended. A coat may be needed for extended outdoor time.",
            priority = 1,
            color = MaterialTheme.colorScheme.primary
        )
        else -> WeatherSuggestion(
            icon = Icons.Rounded.AcUnit,
            title = "Cold — Bundle Up",
            description = "Heavy jacket, gloves, and scarf recommended. Protect extremities.",
            priority = 0,
            color = MaterialTheme.colorScheme.primary
        )
    }
    suggestions.add(clothingSuggestion)

    // ── Storm warning ──
    if (isStormy) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.Thunderstorm,
            title = "Thunderstorm Alert",
            description = "Seek shelter indoors. Avoid open areas and tall objects.",
            priority = 0,
            color = MaterialTheme.colorScheme.error
        ))
    }

    // ── Wind ──
    if (windSpeed > 40) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.Air,
            title = "Strong Winds",
            description = "Wind gusts up to ${windSpeed.roundToInt()} km/h. Secure loose items.",
            priority = 1,
            color = MaterialTheme.colorScheme.secondary
        ))
    }

    // ── Fog / Visibility ──
    if (code in 45..48) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.Visibility,
            title = "Foggy Conditions",
            description = "Reduced visibility. Drive carefully with fog lights on.",
            priority = 1,
            color = MaterialTheme.colorScheme.outline
        ))
    }

    // ── Humidity ──
    if (humidity >= 80 && temp >= 28) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.WaterDrop,
            title = "High Humidity",
            description = "Feels warmer than actual temperature. Stay hydrated.",
            priority = 2,
            color = MaterialTheme.colorScheme.primary
        ))
    }

    // ── Outdoor activity ──
    if (!isRainy && !isStormy && temp in 15.0..30.0 && windSpeed < 30 && uvIndex < 8) {
        suggestions.add(WeatherSuggestion(
            icon = Icons.Rounded.DirectionsWalk,
            title = "Great for Outdoor Activities",
            description = "Pleasant conditions for walking, jogging, or cycling.",
            priority = 3,
            color = MaterialTheme.colorScheme.tertiary
        ))
    }

    return suggestions.sortedBy { it.priority }
}
