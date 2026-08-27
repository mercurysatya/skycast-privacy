package com.vayu.weather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.ui.theme.SkyBlue
import kotlin.math.roundToInt

@Composable
fun PressureTrendChart(
    hourlyData: List<HourlyWeather>,
    modifier: Modifier = Modifier
) {
    val sorted = remember(hourlyData) {
        hourlyData.sortedBy { it.time }.filter { it.pressure != null }.take(24)
    }
    if (sorted.isEmpty()) return

    val pressures = remember(sorted) { sorted.mapNotNull { it.pressure } }
    if (pressures.isEmpty()) return
    val minPressure = pressures.min()
    val maxPressure = pressures.max()
    val currentPressure = pressures.last()

    val trend = remember(pressures) {
        when {
            pressures.size < 2 -> "Stable"
            pressures.last() > pressures.first() + 2 -> "Rising"
            pressures.last() < pressures.first() - 2 -> "Falling"
            else -> "Stable"
        }
    }

    val trendDescription = remember(trend) {
        when (trend) {
            "Rising" -> "Rising — Fair weather expected"
            "Falling" -> "Falling — Rain or storm possible"
            else -> "Stable — Weather should remain unchanged"
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(sorted) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pressure Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${currentPressure.roundToInt()} hPa",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val trendColor = when (trend) {
                "Rising" -> MaterialTheme.colorScheme.tertiary
                "Falling" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
            Text(
                text = trendDescription,
                style = MaterialTheme.typography.labelSmall,
                color = trendColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 8f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2
                val range = (maxPressure - minPressure).coerceAtLeast(1.0)

                // Grid lines
                for (i in 0..3) {
                    val y = padding + chartHeight * i / 3f
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1f
                    )
                }

                if (sorted.size < 2) return@Canvas

                val points = sorted.mapIndexed { index, hour ->
                    val x = padding + chartWidth * index / (sorted.size - 1).coerceAtLeast(1)
                    val normalizedPressure = (((hour.pressure ?: 1013.0) - minPressure) / range).toFloat()
                    val y = padding + chartHeight * (1f - normalizedPressure.coerceIn(0f, 1f))
                    Offset(x, y)
                }

                val animatedEndIndex = (points.size * animProgress.value).toInt().coerceAtMost(points.size - 1)
                val visiblePoints = points.take(animatedEndIndex + 1)

                if (visiblePoints.size >= 2) {
                    // Area fill (simplified — no gradient shader needed)
                    val areaPath = Path().apply {
                        moveTo(visiblePoints.first().x, height)
                        visiblePoints.forEach { lineTo(it.x, it.y) }
                        lineTo(visiblePoints.last().x, height)
                        close()
                    }
                    drawPath(areaPath, SkyBlue.copy(alpha = 0.15f))

                    // Line
                    val linePath = Path().apply {
                        visiblePoints.forEachIndexed { i, point ->
                            if (i == 0) moveTo(point.x, point.y)
                            else lineTo(point.x, point.y)
                        }
                    }
                    drawPath(linePath, SkyBlue, style = Stroke(width = 3f, cap = StrokeCap.Round))

                    // Current point
                    val lastPoint = visiblePoints.last()
                    drawCircle(SkyBlue, radius = 6f, center = lastPoint)
                    drawCircle(Color.White, radius = 3f, center = lastPoint)
                }
            }

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${minPressure.roundToInt()} hPa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = "${maxPressure.roundToInt()} hPa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
