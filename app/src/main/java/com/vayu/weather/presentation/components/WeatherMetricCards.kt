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
                    val labels = listOf("N" to -90f, "E" to 0f, "S" to 90f, "W" to 180f)
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
    // Approximate moon phase based on current date
    val moonPhase = remember {
        val now = java.time.LocalDate.now()
        val knownNewMoon = java.time.LocalDate.of(2024, 1, 11)
        val daysSinceNew = java.time.temporal.ChronoUnit.DAYS.between(knownNewMoon, now) % 29.53058770576
        val phase = daysSinceNew / 29.53058770576
        phase
    }

    val (phaseName, illumination) = when {
        moonPhase < 0.03 || moonPhase > 0.97 -> "New Moon" to 0
        moonPhase < 0.22 -> "Waxing Crescent" to (moonPhase * 4 * 100).toInt()
        moonPhase < 0.28 -> "First Quarter" to 50
        moonPhase < 0.47 -> "Waxing Gibbous" to (50 + (moonPhase - 0.25) * 2 * 50).toInt()
        moonPhase < 0.53 -> "Full Moon" to 100
        moonPhase < 0.72 -> "Waning Gibbous" to (100 - (moonPhase - 0.5) * 2 * 50).toInt()
        moonPhase < 0.78 -> "Last Quarter" to 50
        else -> "Waning Crescent" to (50 - (moonPhase - 0.75) * 4 * 50).toInt()
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

            // Moon visualization
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f

                    // Moon base
                    drawCircle(
                        color = Color(0xFFF5F5DC),
                        radius = radius,
                        center = center
                    )

                    // Shadow overlay based on phase
                    val shadowX = (moonPhase * 2 - 1).toFloat()
                    drawCircle(
                        color = Color(0xFF1A1A2E).copy(alpha = 0.85f),
                        radius = radius,
                        center = Offset(center.x + shadowX * radius * 0.5f, center.y)
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
