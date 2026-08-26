package com.vayu.weather.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import com.vayu.weather.ui.theme.WeatherColors
import com.vayu.weather.ui.theme.WeatherOpacity
import kotlin.math.roundToInt

// ============================================================
// UV INDEX CARD — premium gauge
// ============================================================

@Composable
fun UvIndexCard(
    uvIndex: Double?,
    modifier: Modifier = Modifier
) {
    val uv = uvIndex?.roundToInt() ?: 0
    val fraction = (uv / 11f).coerceIn(0f, 1f)
    val (uvColor, uvLabel) = when {
        uv <= 2 -> WeatherColors.uvLow to "Low"
        uv <= 5 -> WeatherColors.uvModerate to "Moderate"
        uv <= 7 -> WeatherColors.uvHigh to "High"
        uv <= 10 -> WeatherColors.uvVeryHigh to "Very High"
        else -> WeatherColors.uvExtreme to "Extreme"
    }

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.uv_index),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = fraction,
                    modifier = Modifier.fillMaxSize(),
                    color = uvColor,
                    strokeWidth = 8f
                )
                Text(
                    text = "$uv",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uvLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = uvColor
            )

            if (uv >= 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        uv >= 8 -> "Protection essential"
                        uv >= 6 -> "Protection required"
                        else -> "Wear sunscreen"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
                )
            }
        }
    }
}

// ============================================================
// WIND CARD — compass visualization
// ============================================================

@Composable
fun WindCard(
    speed: Double?,
    direction: Double?,
    gusts: Double?,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    val windSpeed = speed?.roundToInt() ?: 0
    val dir = direction?.toFloat() ?: 0f

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.wind),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 6f

                    // Compass circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = radius,
                        center = center
                    )

                    // Compass needle (direction arrow)
                    val rad = Math.toRadians(dir.toDouble())
                    val needleLength = radius * 0.7f
                    val endX = center.x + (needleLength * Math.sin(rad)).toFloat()
                    val endY = center.y - (needleLength * Math.cos(rad)).toFloat()

                    drawLine(
                        color = WeatherColors.rainy,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Arrow head
                    drawCircle(
                        color = WeatherColors.rainy,
                        radius = 4f,
                        center = Offset(endX, endY)
                    )

                    // Cardinal points
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(120, 255, 255, 255)
                        textSize = 18f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val labels = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
                    labels.forEach { (label, angle) ->
                        val rad2 = Math.toRadians(angle.toDouble())
                        val lx = center.x + (radius * 0.85f * Math.sin(rad2)).toFloat()
                        val ly = center.y - (radius * 0.85f * Math.cos(rad2)).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(label, lx, ly + 6f, textPaint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$windSpeed $unitLabel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            gusts?.let {
                if (it > 0) {
                    Text(
                        text = "Gusts ${it.roundToInt()} $unitLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
                    )
                }
            }

            // Direction label
            val dirLabel = formatWindDirection(direction)
            if (dirLabel.isNotEmpty()) {
                Text(
                    text = dirLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_TERTIARY)
                )
            }
        }
    }
}

private fun formatWindDirection(degrees: Double?): String {
    if (degrees == null) return ""
    val normalized = ((degrees % 360) + 360) % 360
    return when {
        normalized < 22.5 -> "North"
        normalized < 67.5 -> "Northeast"
        normalized < 112.5 -> "East"
        normalized < 157.5 -> "Southeast"
        normalized < 202.5 -> "South"
        normalized < 247.5 -> "Southwest"
        normalized < 292.5 -> "West"
        normalized < 337.5 -> "Northwest"
        else -> "North"
    }
}

// ============================================================
// PRESSURE CARD — with trend indicator
// ============================================================

@Composable
fun PressureCard(
    pressure: Double?,
    modifier: Modifier = Modifier
) {
    val hpa = pressure?.roundToInt() ?: 0
    val trend = when {
        hpa > 1020 -> "↑ Rising"
        hpa < 1000 -> "↓ Falling"
        else -> "→ Stable"
    }
    val trendColor = when {
        hpa > 1020 -> WeatherColors.aqiGood
        hpa < 1000 -> WeatherColors.uvModerate
        else -> Color.White.copy(alpha = 0.6f)
    }

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.pressure),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$hpa",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "hPa",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_TERTIARY)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = trend,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = trendColor
            )

            if (hpa > 1013) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High pressure — fair weather",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Low pressure — unsettled",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
                )
            }
        }
    }
}

// ============================================================
// MOON PHASE CARD
// ============================================================

@Composable
fun MoonPhaseCard(
    modifier: Modifier = Modifier
) {
    // Accurate moon phase calculation using multiple reference points
    val moonPhase = remember {
        val now = java.time.LocalDate.now()
        // Use the most recent known new moon before today
        // New moons from 2024-2027 (UTC dates)
        val newMoons = listOf(
            java.time.LocalDate.of(2024, 1, 11),
            java.time.LocalDate.of(2024, 2, 9),
            java.time.LocalDate.of(2024, 3, 10),
            java.time.LocalDate.of(2024, 4, 8),
            java.time.LocalDate.of(2024, 5, 8),
            java.time.LocalDate.of(2024, 6, 6),
            java.time.LocalDate.of(2024, 7, 5),
            java.time.LocalDate.of(2024, 8, 4),
            java.time.LocalDate.of(2024, 9, 3),
            java.time.LocalDate.of(2024, 10, 2),
            java.time.LocalDate.of(2024, 11, 1),
            java.time.LocalDate.of(2024, 12, 1),
            java.time.LocalDate.of(2025, 1, 29),
            java.time.LocalDate.of(2025, 2, 28),
            java.time.LocalDate.of(2025, 3, 29),
            java.time.LocalDate.of(2025, 4, 27),
            java.time.LocalDate.of(2025, 5, 27),
            java.time.LocalDate.of(2025, 6, 25),
            java.time.LocalDate.of(2025, 7, 24),
            java.time.LocalDate.of(2025, 8, 23),
            java.time.LocalDate.of(2025, 9, 21),
            java.time.LocalDate.of(2025, 10, 21),
            java.time.LocalDate.of(2025, 11, 20),
            java.time.LocalDate.of(2025, 12, 20),
            java.time.LocalDate.of(2026, 1, 18),
            java.time.LocalDate.of(2026, 2, 17),
            java.time.LocalDate.of(2026, 3, 19),
            java.time.LocalDate.of(2026, 4, 17),
            java.time.LocalDate.of(2026, 5, 16),
            java.time.LocalDate.of(2026, 6, 15),
            java.time.LocalDate.of(2026, 7, 14),
            java.time.LocalDate.of(2026, 8, 12),
            java.time.LocalDate.of(2026, 9, 11),
            java.time.LocalDate.of(2026, 10, 11),
            java.time.LocalDate.of(2026, 11, 9),
            java.time.LocalDate.of(2026, 12, 9),
            java.time.LocalDate.of(2027, 1, 7),
            java.time.LocalDate.of(2027, 2, 6),
            java.time.LocalDate.of(2027, 3, 8),
            java.time.LocalDate.of(2027, 4, 6),
            java.time.LocalDate.of(2027, 5, 6),
            java.time.LocalDate.of(2027, 6, 4),
            java.time.LocalDate.of(2027, 7, 4),
            java.time.LocalDate.of(2027, 8, 2)
        )
        // Find the most recent new moon before today
        val referenceNewMoon = newMoons.lastOrNull { !it.isAfter(now) }
            ?: newMoons.first()
        val synodicPeriod = 29.53058770576
        val daysSinceNew = java.time.temporal.ChronoUnit.DAYS.between(referenceNewMoon, now)
        val phase = (daysSinceNew / synodicPeriod).coerceIn(0.0, 1.0)
        phase
    }

    // Accurate illumination using cosine formula
    val illumination = remember(moonPhase) {
        ((1.0 - kotlin.math.cos(2.0 * Math.PI * moonPhase)) / 2.0 * 100.0).toInt()
    }

    // Phase name with accurate boundaries
    val phaseName = remember(moonPhase) {
        when {
            moonPhase < 0.0625 || moonPhase > 0.9375 -> "New Moon"
            moonPhase < 0.1875 -> "Waxing Crescent"
            moonPhase < 0.3125 -> "First Quarter"
            moonPhase < 0.4375 -> "Waxing Gibbous"
            moonPhase < 0.5625 -> "Full Moon"
            moonPhase < 0.6875 -> "Waning Gibbous"
            moonPhase < 0.8125 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    // Moon age in days (for display)
    val moonAge = remember(moonPhase) {
        (moonPhase * 29.53058770576).toInt()
    }

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Moon Phase",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Moon visualization — accurate terminator line
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension / 2f

                    // 1) Draw illuminated moon base
                    drawCircle(
                        color = com.vayu.weather.ui.theme.MoonBase,
                        radius = r,
                        center = Offset(cx, cy)
                    )

                    // 2) Draw shadow using terminator math
                    // The terminator is always a half-circle (ellipse)
                    // Its width varies: 0 at full/new, full radius at quarters
                    val terminatorWidth = (kotlin.math.cos(2.0 * Math.PI * moonPhase) * r).toFloat()

                    // Shadow = circle clipped by the terminator
                    // For waxing phases (0-0.5), shadow is on the right
                    // For waning phases (0.5-1.0), shadow is on the left
                    if (moonPhase < 0.0625 || moonPhase > 0.9375) {
                        // New Moon — fully dark
                        drawCircle(
                            color = com.vayu.weather.ui.theme.MoonShadow.copy(alpha = 0.9f),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    } else if (moonPhase > 0.4375 && moonPhase < 0.5625) {
                        // Full Moon — no shadow (skip drawing)
                    } else {
                        // All other phases — draw terminator shadow
                        // The shadow is a rectangle covering half the moon,
                        // with a curved edge (ellipse) for the terminator
                        val isWaxing = moonPhase < 0.5

                        // Shadow rectangle (covers the dark side)
                        if (isWaxing) {
                            // Waxing: right side is dark, terminator curves from center
                            // Draw shadow rectangle on right side
                            drawRect(
                                color = com.vayu.weather.ui.theme.MoonShadow.copy(alpha = 0.9f),
                                topLeft = Offset(cx, cy - r),
                                size = Size(r, r * 2)
                            )
                            // Clip the curved terminator edge
                            drawCircle(
                                color = com.vayu.weather.ui.theme.MoonBase,
                                radius = kotlin.math.abs(terminatorWidth),
                                center = Offset(cx + terminatorWidth, cy)
                            )
                        } else {
                            // Waning: left side is dark, terminator curves from center
                            drawRect(
                                color = com.vayu.weather.ui.theme.MoonShadow.copy(alpha = 0.9f),
                                topLeft = Offset(cx - r, cy - r),
                                size = Size(r, r * 2)
                            )
                            drawCircle(
                                color = com.vayu.weather.ui.theme.MoonBase,
                                radius = kotlin.math.abs(terminatorWidth),
                                center = Offset(cx + terminatorWidth, cy)
                            )
                        }
                    }

                    // Subtle glow around moon
                    drawCircle(
                        color = com.vayu.weather.ui.theme.MoonBase.copy(alpha = 0.15f),
                        radius = r + 4f,
                        center = Offset(cx, cy)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = phaseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "${illumination.coerceIn(0, 100)}% illuminated",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
            )
            Text(
                text = "Day $moonAge of 29",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY * 0.7f)
            )
        }
    }
}

// ============================================================
// PRECIPITATION TIMELINE CARD
// ============================================================

@Composable
fun PrecipitationTimelineCard(
    hourlyData: List<com.vayu.weather.domain.model.HourlyWeather>,
    modifier: Modifier = Modifier
) {
    if (hourlyData.isEmpty()) return

    val next6Hours = remember(hourlyData) {
        val now = java.time.LocalDateTime.now()
        val currentHour = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
        hourlyData
            .sortedBy { it.time }
            .filter { it.time >= currentHour }
            .take(6)
    }

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Precipitation Timeline",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Visual timeline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                val barWidth = size.width / next6Hours.size.coerceAtLeast(1)
                next6Hours.forEachIndexed { index, hour ->
                    val precipProb = when (hour.weatherCode) {
                        in 51..55, in 61..65, in 80..82 -> 60
                        in 71..75 -> 70
                        in 95..99 -> 85
                        else -> 0
                    }
                    val fraction = (precipProb / 100f).coerceIn(0f, 1f)
                    val barHeight = size.height * fraction
                    val x = barWidth * index + barWidth * 0.2f
                    val w = barWidth * 0.6f

                    // Track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = Offset(x, 0f),
                        size = Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )

                    // Fill
                    if (fraction > 0f) {
                        drawRoundRect(
                            color = WeatherColors.rainy.copy(alpha = 0.7f),
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(w, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                next6Hours.forEach { hour ->
                    val label = try {
                        java.time.LocalTime.parse(hour.time.substringAfter("T"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("ha"))
                    } catch (e: Exception) { hour.time.takeLast(5) }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = WeatherOpacity.TEXT_TERTIARY),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
