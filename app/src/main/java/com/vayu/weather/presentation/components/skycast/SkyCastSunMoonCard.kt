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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.ui.theme.SkyCastTokens
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * SkyCast Sun & Moon card.
 *
 * Shows sunrise/sunset, day length, a sun-arc visual, and a stylized moon
 * with phase name, illumination, and moonrise/moonset times when known.
 */
@Composable
fun SkyCastSunMoonCard(
    daily: DailyWeather?,
    currentTimeInLocation: String? = null,
    modifier: Modifier = Modifier
) {
    if (daily == null || daily.sunrise == null || daily.sunset == null) {
        SkyCastCard(contentPadding = PaddingValues(16.dp)) {
            SkyCastSectionHeader(title = "Sun & moon", subtitle = "Data unavailable")
        }
        return
    }

    val sunrise = parseTime(daily.sunrise)
    val sunset = parseTime(daily.sunset)
    // Use the API's current-time string (in the weather location's timezone)
    // instead of LocalTime.now() (device timezone) to avoid incorrect sun
    // position when the user is viewing a city in a different timezone.
    val now = if (currentTimeInLocation != null) {
        parseTime(currentTimeInLocation)
    } else {
        LocalTime.now()
    }
    val dayMinutes = ((sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60).coerceAtLeast(1)
    val elapsed = ((now.toSecondOfDay() - sunrise.toSecondOfDay()) / 60).coerceAtLeast(0)
    val progress = (elapsed.toFloat() / dayMinutes).coerceIn(0f, 1f)
    val remainingMin = (dayMinutes - elapsed).coerceAtLeast(0)

    SkyCastCard(contentPadding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkyCastSectionHeader(title = "Sun & moon", subtitle = formatDayLength(dayMinutes))
            Spacer(modifier = Modifier.height(12.dp))

            // Sun arc visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val w = size.width
                    val h = size.height
                    val arcWidth = w - 32f
                    val arcHeight = h * 0.7f
                    val centerX = w / 2f
                    val topY = h - arcHeight
                    val left = 16f
                    val right = left + arcWidth

                    // Horizon line
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, h - 1f),
                        end = Offset(w, h - 1f),
                        strokeWidth = 2f
                    )

                    // Sun arc (path)
                    val path = Path().apply {
                        moveTo(left, h - 1f)
                        quadraticBezierTo(
                            centerX, topY,
                            right, h - 1f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )

                    // Sun position
                    val sunX = left + (arcWidth * progress)
                    val sunY = h - 1f - (arcHeight * 4f * progress * (1f - progress))
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = 10f,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.3f),
                        radius = 18f,
                        center = Offset(sunX, sunY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sunrise",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(sunrise),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val elapsedMin = (dayMinutes - remainingMin).coerceAtLeast(0)
                    Text(
                        text = if (progress < 0.5f) "Until sunset" else "Since sunrise",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (progress < 0.5f) {
                            "${remainingMin / 60}h ${remainingMin % 60}m"
                        } else {
                            "${elapsedMin / 60}h ${elapsedMin % 60}m"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sunset",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(sunset),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Moon row
            Spacer(modifier = Modifier.height(16.dp))
            // Use the daily forecast date (parsed from the API date string) so
            // the moon phase shown matches the date the user is viewing, not
            // the device's current date. Falls back to today if the API date
            // is missing or unparseable.
            val moonDate = remember(daily.date) {
                runCatching { LocalDate.parse(daily.date) }
                    .getOrElse { LocalDate.now() }
            }
            val moonPhase = remember(moonDate) {
                com.vayu.weather.domain.astronomy.MoonPhaseCalculator
                    .computeForDate(moonDate)
            }
            val illumPct = com.vayu.weather.domain.astronomy.MoonPhaseCalculator
                .illuminationPercent(moonPhase)
            val terminatorOffset = com.vayu.weather.domain.astronomy.MoonPhaseCalculator
                .terminatorOffset(moonPhase)
            val isWaxing = moonPhase.phaseName.isWaxing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF5F5DC),
                                    Color(0xFF9CA3AF)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.width / 2.2f
                        val illum = moonPhase.illuminationFraction

                        when {
                            illum < 0.01 -> {
                                // New Moon — fully dark
                                drawCircle(
                                    color = Color(0xFF1A1A2E),
                                    radius = r,
                                    center = Offset(cx, cy)
                                )
                            }
                            illum > 0.99 -> {
                                // Full Moon — no shadow
                                drawCircle(
                                    color = Color(0xFFF5F5DC),
                                    radius = r,
                                    center = Offset(cx, cy)
                                )
                            }
                            else -> {
                                // Terminator geometry: build a half-disc that
                                // represents either the lit (waxing) or dark
                                // (waning) side, then overlay the opposite half
                                // trimmed by an elliptical terminator.
                                //
                                // For a waxing moon, the RIGHT side is dark.
                                // For a waning moon, the LEFT side is dark.
                                val terminatorX = cx + (terminatorOffset * r).toFloat()
                                if (isWaxing) {
                                    // Lit half-disc (left side) + dark half-disc (right side trimmed by ellipse)
                                    drawCircle(
                                        color = Color(0xFFF5F5DC),
                                        radius = r,
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = Color(0xFF1A1A2E),
                                        radius = r,
                                        center = Offset(cx, cy)
                                    )
                                    // Carve the lit portion back using an ellipse
                                    drawCircle(
                                        color = Color(0xFFF5F5DC),
                                        radius = r,
                                        center = Offset(terminatorX, cy)
                                    )
                                } else {
                                    // Waning: left side dark, right side lit
                                    drawCircle(
                                        color = Color(0xFFF5F5DC),
                                        radius = r,
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = Color(0xFF1A1A2E),
                                        radius = r,
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = Color(0xFFF5F5DC),
                                        radius = r,
                                        center = Offset(terminatorX, cy)
                                    )
                                }
                            }
                        }
                    }
                }
                Column {
                    Text(
                        text = moonPhase.phaseName.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Illumination: $illumPct%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun parseTime(value: String): LocalTime = try {
    val timePart = value.substringAfter("T")
    LocalTime.parse(timePart)
} catch (e: Exception) {
    LocalTime.of(6, 0)
}

private fun formatTime(t: LocalTime): String =
    t.format(DateTimeFormatter.ofPattern("h:mm a"))

private fun formatDayLength(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "Day length: ${h}h ${m}m"
}
