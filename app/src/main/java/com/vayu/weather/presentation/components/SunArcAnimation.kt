package com.vayu.weather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.ui.theme.AmberGlow
import com.vayu.weather.ui.theme.WarmOrange
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

@Composable
fun SunArcAnimation(
    dailyData: List<DailyWeather>,
    modifier: Modifier = Modifier
) {
    val today = dailyData.firstOrNull() ?: return
    val sunrise = today.sunrise?.let { parseTime(it) } ?: return
    val sunset = today.sunset?.let { parseTime(it) } ?: return

    val now = LocalTime.now()
    val totalDaylightMinutes = (sunset.hour * 60 + sunset.minute) - (sunrise.hour * 60 + sunrise.minute)
    val elapsedMinutes = (now.hour * 60 + now.minute) - (sunrise.hour * 60 + sunrise.minute)
    val progress = (elapsedMinutes.toFloat() / totalDaylightMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isDaytime = now.isAfter(sunrise) && now.isBefore(sunset)

    // Animate sun position
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDaytime) progress else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "sun_progress"
    )

    // Pulsating glow for current sun
    val infiniteTransition = rememberInfiniteTransition(label = "sun_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sun & Moon",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val width = size.width
                val height = size.height
                val arcWidth = width * 0.9f
                val arcHeight = height * 0.7f
                val startX = width * 0.05f
                val startY = height * 0.85f

                // Draw dashed horizon line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(startX + arcWidth, startY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // Draw arc path (daylight curve)
                val arcPath = Path().apply {
                    moveTo(startX, startY)
                    quadraticBezierTo(
                        startX + arcWidth / 2, startY - arcHeight,
                        startX + arcWidth, startY
                    )
                }

                // Draw the arc (dim background)
                drawPath(
                    arcPath,
                    Color.LightGray.copy(alpha = 0.2f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Draw illuminated portion (before current position)
                if (isDaytime && animatedProgress > 0f) {
                    val illuminatedPath = Path()
                    val steps = 50
                    val endStep = (steps * animatedProgress).toInt().coerceAtMost(steps)
                    for (i in 0..endStep) {
                        val t = i.toFloat() / steps
                        val x = startX + arcWidth * t
                        val y = startY - arcHeight * 4f * t * (1f - t)
                        if (i == 0) illuminatedPath.moveTo(x, y)
                        else illuminatedPath.lineTo(x, y)
                    }
                    drawPath(
                        illuminatedPath,
                        AmberGlow.copy(alpha = 0.6f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // Draw sun position
                if (isDaytime) {
                    val sunX = startX + arcWidth * animatedProgress
                    val sunY = startY - arcHeight * 4f * animatedProgress * (1f - animatedProgress)

                    // Glow effect
                    drawCircle(
                        color = WarmOrange.copy(alpha = 0.15f * glowScale),
                        radius = 24f * glowScale,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = AmberGlow.copy(alpha = 0.3f * glowScale),
                        radius = 16f * glowScale,
                        center = Offset(sunX, sunY)
                    )
                    // Sun dot
                    drawCircle(
                        color = AmberGlow,
                        radius = 8f,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(sunX, sunY)
                    )
                }

                // Sunrise dot (left)
                drawCircle(
                    color = WarmOrange.copy(alpha = 0.7f),
                    radius = 5f,
                    center = Offset(startX, startY)
                )

                // Sunset dot (right)
                drawCircle(
                    color = WarmOrange.copy(alpha = 0.7f),
                    radius = 5f,
                    center = Offset(startX + arcWidth, startY)
                )
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sunrise",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(today.sunrise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDaytime) "Daytime" else "Nighttime",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDaytime) AmberGlow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    val daylightH = totalDaylightMinutes / 60
                    val daylightM = totalDaylightMinutes % 60
                    Text(
                        text = "${daylightH}h ${daylightM}m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = "Sunset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(today.sunset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun parseTime(isoString: String): LocalTime? {
    return try {
        val timeStr = isoString.substringAfter("T").take(5)
        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) { null }
}

private fun formatTime(isoString: String?): String {
    if (isoString == null) return "--"
    return try {
        val timeStr = isoString.substringAfter("T").take(5)
        val time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        time.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (_: Exception) { "--" }
}
