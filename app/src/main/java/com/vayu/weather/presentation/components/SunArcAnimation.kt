package com.vayu.weather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val sunriseMin = sunrise.hour * 60 + sunrise.minute
    val sunsetMin = sunset.hour * 60 + sunset.minute
    val totalDaylightMin = (sunsetMin - sunriseMin).coerceAtLeast(1)
    val elapsedMin = (now.hour * 60 + now.minute) - sunriseMin
    val progress = (elapsedMin.toFloat() / totalDaylightMin).coerceIn(0f, 1f)
    val isDaytime = now.isAfter(sunrise) && now.isBefore(sunset)

    // Countdown to next event
    val minutesRemaining = if (isDaytime) {
        sunsetMin - (now.hour * 60 + now.minute)
    } else {
        // Time until next sunrise
        val nowMin = now.hour * 60 + now.minute
        if (nowMin >= sunsetMin) {
            // After sunset → until tomorrow's sunrise
            (24 * 60 - nowMin) + sunriseMin
        } else {
            // Before sunrise → until today's sunrise
            sunriseMin - nowMin
        }
    }
    val countdownH = minutesRemaining / 60
    val countdownM = minutesRemaining % 60
    val countdownLabel = if (isDaytime) "sunset" else "sunrise"

    // Animate sun position on arc
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDaytime) progress else if (now.isBefore(sunrise)) 0f else 1f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "sun_progress"
    )

    // Pulsating glow
    val infiniteTransition = rememberInfiniteTransition(label = "sun_pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    // Slow rotation for sun rays
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ray_rotation"
    )

    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isDaytime) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = if (isDaytime) AmberGlow else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sun & Moon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                // Countdown badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDaytime)
                        WarmOrange.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (countdownH > 0)
                            "${countdownH}h ${countdownM}m to $countdownLabel"
                        else
                            "${countdownM}m to $countdownLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDaytime) WarmOrange else primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Sun Arc Canvas ──
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height
                val arcW = w * 0.88f
                val arcH = h * 0.65f
                val startX = w * 0.06f
                val endX = startX + arcW
                val horizonY = h * 0.82f

                // ── Background gradient for arc area ──
                val arcBgPath = Path().apply {
                    moveTo(startX, horizonY)
                    quadraticBezierTo(startX + arcW / 2, horizonY - arcH * 1.1f, endX, horizonY)
                    lineTo(endX, h)
                    lineTo(startX, h)
                    close()
                }
                drawPath(
                    arcBgPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A5F).copy(alpha = 0.08f), // sky blue tint
                            Color.Transparent
                        ),
                        startY = horizonY - arcH,
                        endY = horizonY
                    )
                )

                // ── Horizon line (dashed) ──
                drawLine(
                    color = onSurface.copy(alpha = 0.1f),
                    start = Offset(startX, horizonY),
                    end = Offset(endX, horizonY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                // ── Arc path (full day arc) ──
                val fullArcPath = Path().apply {
                    moveTo(startX, horizonY)
                    quadraticBezierTo(startX + arcW / 2, horizonY - arcH, endX, horizonY)
                }
                // Dim background arc
                drawPath(
                    fullArcPath,
                    onSurface.copy(alpha = 0.08f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // ── Night arc (from sunset to next sunrise, shown as dashed curve below horizon) ──
                val nightArcPath = Path().apply {
                    moveTo(endX, horizonY)
                    // Draw a subtle curve below horizon
                    quadraticBezierTo(
                        startX + arcW / 2, horizonY + arcH * 0.3f,
                        startX, horizonY
                    )
                }
                drawPath(
                    nightArcPath,
                    primaryColor.copy(alpha = 0.06f),
                    style = Stroke(
                        width = 2f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))
                    )
                )

                // ── Illuminated portion (sunrise → current position) ──
                if (isDaytime && animatedProgress > 0.01f) {
                    val litPath = Path()
                    val steps = 80
                    val endStep = (steps * animatedProgress).toInt().coerceAtMost(steps)
                    for (i in 0..endStep) {
                        val t = i.toFloat() / steps
                        val x = startX + arcW * t
                        val y = horizonY - arcH * 4f * t * (1f - t)
                        if (i == 0) litPath.moveTo(x, y) else litPath.lineTo(x, y)
                    }
                    drawPath(
                        litPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                WarmOrange.copy(alpha = 0.7f),
                                AmberGlow.copy(alpha = 0.9f)
                            )
                        ),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // ── Hour markers along arc ──
                val daylightHours = totalDaylightMin / 60.0
                val hourStep = if (daylightHours <= 10) 1 else 2
                var hourCount = 0
                for (h2 in 0 until daylightHours.toInt() + 1 step hourStep) {
                    val t = h2.toFloat() / daylightHours.toFloat()
                    if (t > 1f) continue
                    val mx = startX + arcW * t
                    val my = horizonY - arcH * 4f * t * (1f - t)

                    // Small dot on arc
                    drawCircle(
                        color = onSurface.copy(alpha = 0.12f),
                        radius = 2.5f,
                        center = Offset(mx, my)
                    )

                    // Hour label below horizon
                    val hourOfDay = sunrise.hour + h2
                    val label = when {
                        hourOfDay == 0 || hourOfDay == 24 -> "12a"
                        hourOfDay < 12 -> "${hourOfDay}a"
                        hourOfDay == 12 -> "12p"
                        else -> "${hourOfDay - 12}p"
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#94A3B8")
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(label, mx, horizonY + 22f, paint)
                    }
                    hourCount++
                }

                // ── Sunrise / Sunset dots ──
                // Sunrise marker
                drawCircle(
                    color = WarmOrange.copy(alpha = 0.5f),
                    radius = 6f,
                    center = Offset(startX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3f,
                    center = Offset(startX, horizonY)
                )

                // Sunset marker
                drawCircle(
                    color = Color(0xFF7C3AED).copy(alpha = 0.5f),
                    radius = 6f,
                    center = Offset(endX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3f,
                    center = Offset(endX, horizonY)
                )

                // ── Sun position ──
                if (isDaytime) {
                    val sunX = startX + arcW * animatedProgress
                    val sunY = horizonY - arcH * 4f * animatedProgress * (1f - animatedProgress)

                    // Vertical dashed line to horizon (sun altitude reference)
                    drawLine(
                        color = onSurface.copy(alpha = 0.06f),
                        start = Offset(sunX, sunY + 8f),
                        end = Offset(sunX, horizonY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f))
                    )

                    // Sun rays (rotating)
                    val rayCount = 8
                    val innerR = 10f
                    val outerR = 18f * glowScale
                    for (i in 0 until rayCount) {
                        val angle = Math.toRadians((rayRotation + i * 360f / rayCount).toDouble())
                        val x1 = sunX + cos(angle).toFloat() * innerR
                        val y1 = sunY + sin(angle).toFloat() * innerR
                        val x2 = sunX + cos(angle).toFloat() * outerR
                        val y2 = sunY + sin(angle).toFloat() * outerR
                        drawLine(
                            color = AmberGlow.copy(alpha = 0.4f * glowScale),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Outer glow
                    drawCircle(
                        color = WarmOrange.copy(alpha = 0.08f * glowScale),
                        radius = 30f * glowScale,
                        center = Offset(sunX, sunY)
                    )
                    // Mid glow
                    drawCircle(
                        color = AmberGlow.copy(alpha = 0.2f * glowScale),
                        radius = 18f * glowScale,
                        center = Offset(sunX, sunY)
                    )
                    // Sun core
                    drawCircle(
                        color = AmberGlow,
                        radius = 8f,
                        center = Offset(sunX, sunY)
                    )
                    // Bright center
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(sunX, sunY)
                    )
                } else {
                    // Nighttime — show moon icon on the night arc
                    val moonX = startX + arcW * 0.5f
                    val moonY = horizonY + arcH * 0.15f

                    // Moon glow
                    drawCircle(
                        color = Color(0xFFCBD5E1).copy(alpha = 0.15f),
                        radius = 20f,
                        center = Offset(moonX, moonY)
                    )
                    // Moon crescent (circle minus overlapping circle)
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        radius = 8f,
                        center = Offset(moonX, moonY)
                    )
                    // Shadow overlay for crescent
                    drawCircle(
                        color = surfaceColor.copy(alpha = 0.8f),
                        radius = 7f,
                        center = Offset(moonX + 4f, moonY - 2f)
                    )
                }
            }

            // ── Bottom labels ──
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sunrise
                Column {
                    Text(
                        text = "Sunrise",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(today.sunrise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
                    )
                }

                // Daytime duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDaytime) "☀️ Daytime" else "🌙 Nighttime",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDaytime) AmberGlow else primaryColor.copy(alpha = 0.7f)
                    )
                    val dH = totalDaylightMin / 60
                    val dM = totalDaylightMin % 60
                    Text(
                        text = "${dH}h ${dM}m of daylight",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
                    )
                }

                // Sunset
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sunset",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(today.sunset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
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
