package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.ui.theme.SkyBlue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * SkyCast rain forecast card.
 *
 * Highlights the next rainy hour and the peak rain-probability in the next
 * 12 hours. Falls back gracefully if no rain is on the way.
 */
@Composable
fun SkyCastRainForecast(
    hourly: List<HourlyWeather>,
    modifier: Modifier = Modifier
) {
    val nextRainy = remember(hourly) { findNextRainy(hourly) }
    val next12 = remember(hourly) { hourly.sortedBy { it.time }.take(12) }
    val peakProb = remember(next12) { next12.maxOfOrNull { it.precipitationProbability ?: 0 } ?: 0 }
    val peakRain = remember(next12) { next12.maxOfOrNull { it.precipitation ?: 0.0 } ?: 0.0 }

    SkyCastCard(contentPadding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkyCastSectionHeader(title = "Rain forecast", subtitle = "Next 12 hours")
            Spacer(modifier = Modifier.height(12.dp))

            if (nextRainy == null) {
                Text(
                    text = "No rain expected in the next 12 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rainHeadline(nextRainy.minutesFromNow),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${nextRainy.probability}% chance · ${nextRainy.intensity} intensity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Probability bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peak probability",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Text(
                    text = "$peakProb%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(peakProb / 100f)
                        .height(6.dp)
                        .background(SkyBlue)
                )
            }
            if (peakRain > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Expected rainfall: ${"%.1f".format(peakRain)} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private data class NextRain(
    val minutesFromNow: Int,
    val probability: Int,
    val intensity: String
)

private fun findNextRainy(hourly: List<HourlyWeather>): NextRain? {
    val now = LocalDateTime.now()
    hourly.sortedBy { it.time }.forEach { h ->
        val time = parseHourly(h.time) ?: return@forEach
        if (time.isBefore(now)) return@forEach
        val minutes = java.time.Duration.between(now, time).toMinutes().toInt()
        if (minutes < 0) return@forEach
        val code = h.weatherCode
        val prob = h.precipitationProbability ?: 0
        val isRainy = code in 51..82 || code in 95..99
        if (isRainy && prob >= 30) {
            val intensity = when {
                code in 95..99 -> "Heavy"
                code in 65..67 || code == 82 -> "Heavy"
                code in 80..81 -> "Moderate"
                code in 55..57 || code in 61..63 -> "Light"
                code in 51..53 -> "Drizzle"
                else -> "Light"
            }
            return NextRain(minutes, prob, intensity)
        }
    }
    return null
}

private fun rainHeadline(minutes: Int): String = when {
    minutes <= 0 -> "Rain is starting now."
    minutes < 30 -> "Rain expected within 30 minutes."
    minutes < 60 -> "Rain expected within the next hour."
    minutes < 120 -> "Rain expected in the next 1–2 hours."
    minutes < 360 -> "Rain expected this afternoon."
    minutes < 720 -> "Rain expected this evening."
    else -> "Rain expected later today."
}

private fun parseHourly(time: String): LocalDateTime? = try {
    LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
} catch (e: Exception) {
    null
}
