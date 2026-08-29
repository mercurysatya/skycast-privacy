package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Precipitation timeline.
 *
 * Shows a smooth probability curve for the next ~10 hours, with hour
 * markers below. Pairs a single, calm "rain likely around X PM" headline
 * with a visual trace so the user can read both the *when* and the *trend*
 * in a single glance.
 */
@Composable
fun SkyCastPrecipitationTimeline(
    hourly: List<HourlyWeather>,
    isCelsius: Boolean = true,
    hoursToShow: Int = 10,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return

    val sorted = hourly.sortedBy { it.time }
    val now = LocalDateTime.now()
    val window = sorted
        .asSequence()
        .mapNotNull { h -> parseHourly(h.time)?.let { it to h } }
        .filter { (time, _) -> !time.isBefore(now.minusMinutes(30)) }
        .take(hoursToShow)
        .toList()
    if (window.isEmpty()) return

    val probs = window.map { it.second.precipitationProbability ?: 0 }
    val maxProb = probs.maxOrNull() ?: 0
    val avgProb = if (probs.isNotEmpty()) probs.average() else 0.0
    val peak = window.indices.maxByOrNull { probs[it] }?.let { window[it] }
    val headline = rainHeadline(peak, maxProb)

    SkyCastCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SkyBlue.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rain chance",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Avg ${avgProb.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (headline != null) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyBlue,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Smooth precipitation curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    val w = size.width
                    val h = size.height
                    if (window.size < 2) return@Canvas
                    val step = w / (window.size - 1)
                    val points = window.mapIndexed { i, (_, hw) ->
                        Offset(i * step, h - (hw.precipitationProbability ?: 0) / 100f * h)
                    }
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val cur = points[i]
                            val mid = Offset((prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
                            quadraticBezierTo(prev.x, prev.y, mid.x, mid.y)
                        }
                        lineTo(points.last().x, points.last().y)
                    }
                    // Soft fill under the curve
                    val area = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, h)
                        lineTo(points.first().x, h)
                        close()
                    }
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            colors = listOf(SkyBlue.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = path,
                        color = SkyBlue,
                        style = Stroke(width = 3f)
                    )
                }
            }

            // Hour labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                window.forEachIndexed { i, (time, hw) ->
                    val isNow = i == 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isNow) "Now" else time.format(DateTimeFormatter.ofPattern("ha")),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNow) SkyBlue else Color.White.copy(alpha = 0.55f),
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = "${hw.precipitationProbability ?: 0}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun parseHourly(time: String): LocalDateTime? = try {
    LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
} catch (e: Exception) {
    null
}

private fun rainHeadline(
    peak: Pair<LocalDateTime, HourlyWeather>?,
    maxProb: Int
): String? {
    if (peak == null) return if (maxProb >= 30) "Rain likely in the next few hours." else null
    val (time, hw) = peak
    val prob = hw.precipitationProbability ?: 0
    if (prob < 30) return null
    val timeLabel = time.format(DateTimeFormatter.ofPattern("h a"))
    val code = hw.weatherCode
    val intensity = when {
        code in 95..99 -> "Thunderstorms"
        code in 65..82 || code in 80..82 -> "Heavy rain"
        code in 55..67 || code in 71..77 -> "Rain"
        else -> "Rain"
    }
    return "$intensity likely around $timeLabel ($prob%)."
}
