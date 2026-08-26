package com.vayu.weather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.ui.theme.*
import kotlin.math.roundToInt

/**
 * Smart Insights Engine — AI-powered daily briefing, weather comparison, trend predictions
 * Generates contextual insights based on weather data patterns.
 */
@Composable
fun SmartInsightsSection(
    info: WeatherInfo,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val insights = remember(info) { generateSmartInsights(info, isCelsius) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Smart Weather Insights"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Smart Insights",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Insights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            insights.forEach { insight ->
                InsightRow(insight)
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightRow(insight: SmartInsight) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(insight.color.copy(alpha = 0.1f))
            .padding(12.dp)
            .semantics {
                contentDescription = "${insight.category}: ${insight.title}. ${insight.description}"
            },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(insight.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = insight.color
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )
        }
        if (insight.score > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(insight.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${insight.score}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = insight.color
                )
            }
        }
    }
}

private data class SmartInsight(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val category: String,
    val color: Color,
    val score: Int = 0
)

private fun generateSmartInsights(info: WeatherInfo, isCelsius: Boolean): List<SmartInsight> {
    val insights = mutableListOf<SmartInsight>()
    val current = info.current
    val daily = info.daily.firstOrNull()
    val code = current.weatherCode
    val temp = current.temperature
    val humidity = current.humidity ?: 50.0
    val windSpeed = current.windSpeed ?: 0.0
    val uv = daily?.uvIndex ?: 0.0
    val visibility = current.visibility ?: 10000.0

    // Daily Briefing
    val highTemp = daily?.maxTemp?.let { if (isCelsius) it.roundToInt() else (it * 9/5 + 32).roundToInt() } ?: "--"
    val lowTemp = daily?.minTemp?.let { if (isCelsius) it.roundToInt() else (it * 9/5 + 32).roundToInt() } ?: "--"
    val briefing = when {
        code in listOf(0, 1) && current.isDay -> "Beautiful day ahead! Expect a high of ${highTemp}° with clear skies. Perfect for outdoor activities."
        code in listOf(2, 3) -> "Mostly ${if (code == 2) "partly cloudy" else "overcast"} today. High of ${highTemp}°, low of ${lowTemp}°."
        code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> "Rain expected today. High of ${highTemp}° with ${daily?.precipitationProbability ?: 0}% precipitation chance."
        code in listOf(95, 96, 99) -> "⚠️ Thunderstorm warning! Stay indoors. High of ${highTemp}° with dangerous conditions."
        code in listOf(71, 73, 75) -> "Snow expected. Bundle up! High of ${highTemp}° with accumulation possible."
        code in listOf(45, 48) -> "Foggy conditions reduce visibility to ${(visibility / 1000).roundToInt()}km. Drive carefully."
        else -> "Today's forecast: ${highTemp}° high, ${lowTemp}° low."
    }
    insights += SmartInsight(
        icon = Icons.Rounded.WbSunny,
        title = "Daily Briefing",
        description = briefing,
        category = "Daily Briefing",
        color = AmberGlow
    )

    // Comfort Index (0-10)
    val comfortScore = calculateComfortScore(temp, humidity, windSpeed, uv)
    val comfortLabel = when {
        comfortScore >= 8 -> "Excellent"
        comfortScore >= 6 -> "Good"
        comfortScore >= 4 -> "Fair"
        else -> "Poor"
    }
    insights += SmartInsight(
        icon = Icons.Rounded.SentimentSatisfied,
        title = "Comfort Index: $comfortLabel",
        description = "Temperature feels ${if (current.apparentTemperature != null) "${(if(isCelsius) current.apparentTemperature else current.apparentTemperature * 9/5 + 32).roundToInt()}°" else "like ${temp.roundToInt()}°"}. Humidity is ${humidity.roundToInt()}%. Wind is ${windSpeed.roundToInt()} km/h.",
        category = "Comfort",
        color = FreshGreen,
        score = comfortScore
    )

    // UV Protection
    if (uv > 0) {
        val uvAdvice = when {
            uv >= 11 -> "Extreme UV! Avoid outdoor exposure between 10 AM - 4 PM. SPF 50+ mandatory."
            uv >= 8 -> "Very High UV. Seek shade during midday. Wear SPF 30+ and protective clothing."
            uv >= 6 -> "High UV. Apply sunscreen SPF 30+ if outdoors for extended periods."
            uv >= 3 -> "Moderate UV. Some protection recommended for prolonged sun exposure."
            else -> "Low UV. Minimal sun protection needed."
        }
        insights += SmartInsight(
            icon = Icons.Rounded.WbSunny,
            title = "UV Protection",
            description = uvAdvice,
            category = "UV",
            color = when {
                uv >= 8 -> SunsetRed
                uv >= 6 -> AmberGlow
                uv >= 3 -> FreshGreen
                else -> SkyBlue
            },
            score = uv.roundToInt()
        )
    }

    // Activity Timing
    val hourlyToday = info.hourly.take(12) // next 12 hours
    val bestOutdoorHour = hourlyToday.minByOrNull {
        val codeScore = when (it.weatherCode) {
            0 -> 0; 1 -> 1; 2 -> 2; 3 -> 3
            in 51..65 -> 8; in 80..82 -> 8
            in 95..99 -> 10; else -> 5
        }
        codeScore
    }
    if (bestOutdoorHour != null) {
        val timeLabel = try {
            bestOutdoorHour.time.substringAfter("T").take(5)
        } catch (e: Exception) { "later" }
        val outdoorScore = when {
            bestOutdoorHour.weatherCode == 0 -> "Excellent"
            bestOutdoorHour.weatherCode <= 3 -> "Good"
            bestOutdoorHour.weatherCode in 51..82 -> "Avoid"
            else -> "Fair"
        }
        insights += SmartInsight(
            icon = Icons.Rounded.DirectionsWalk,
            title = "Best Outdoor Time",
            description = "Best time for outdoor activities is around $timeLabel — $outdoorScore conditions expected.",
            category = "Activity",
            color = when (outdoorScore) {
                "Excellent" -> FreshGreen; "Good" -> SkyBlue
                "Avoid" -> SunsetRed; else -> AmberGlow
            }
        )
    }

    // Wind Chill / Heat Index
    val feelsLike = current.apparentTemperature
    if (feelsLike != null) {
        val diff = feelsLike - temp
        if (kotlin.math.abs(diff) > 3) {
            val description = if (diff < -3) {
                "Wind chill makes it feel ${(-diff).roundToInt()}° colder than actual temperature. Dress warmly with windproof layers."
            } else {
                "Heat index makes it feel ${diff.roundToInt()}° warmer. Stay hydrated and avoid prolonged sun exposure."
            }
            insights += SmartInsight(
                icon = Icons.Rounded.Thermostat,
                title = if (diff < -3) "Wind Chill Alert" else "Heat Index Alert",
                description = description,
                category = "Temperature",
                color = if (diff < -3) SkyBlue else SunsetRed,
                score = feelsLike.roundToInt()
            )
        }
    }

    // Health Advisory
    if (humidity > 80 && temp > 30) {
        insights += SmartInsight(
            icon = Icons.Rounded.LocalHospital,
            title = "Heat & Humidity Advisory",
            description = "High humidity (${humidity.roundToInt()}%) combined with ${temp.roundToInt()}° heat increases risk of heat exhaustion. Drink plenty of water and take breaks in shade.",
            category = "Health",
            color = SunsetRed
        )
    }

    // Air Quality note if available
    if (info.current.visibility != null && info.current.visibility < 5000) {
        insights += SmartInsight(
            icon = Icons.Rounded.Visibility,
            title = "Low Visibility Warning",
            description = "Visibility reduced to ${(info.current.visibility / 1000).roundToInt()}km. Use fog lights if driving. Consider indoor alternatives.",
            category = "Visibility",
            color = AmberGlow
        )
    }

    return insights.take(5)
}

private fun calculateComfortScore(temp: Double, humidity: Double, wind: Double, uv: Double): Int {
    // Temperature score (ideal 18-26°C)
    val tempScore = when {
        temp in 18.0..26.0 -> 10
        temp in 15.0..30.0 -> 7
        temp in 10.0..35.0 -> 5
        else -> 3
    }
    // Humidity score (ideal 30-60%)
    val humidScore = when {
        humidity in 30.0..60.0 -> 10
        humidity in 20.0..70.0 -> 7
        else -> 4
    }
    // Wind score (ideal < 20 km/h)
    val windScore = when {
        wind < 10 -> 10
        wind < 20 -> 8
        wind < 30 -> 5
        else -> 3
    }
    // UV score
    val uvScore = when {
        uv < 3 -> 10; uv < 6 -> 7; uv < 8 -> 5; else -> 3
    }
    return ((tempScore + humidScore + windScore + uvScore) / 4.0).roundToInt().coerceIn(1, 10)
}
