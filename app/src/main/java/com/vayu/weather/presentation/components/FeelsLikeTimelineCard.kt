package com.vayu.weather.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.vayu.weather.R
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.presentation.weather.getWeatherIcon
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.WarmOrange
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 24-hour "feels like" timeline.
 *
 * Plots apparent temperature over the next 24 hours. When the user has not
 * supplied an `apparentTemperature` value per hour (the API doesn't always
 * return one), the actual temperature is used as a fallback.
 */
@Composable
fun FeelsLikeTimelineCard(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    if (hourlyData.isEmpty()) return

    val sorted = remember(hourlyData) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
        val start = hourlyData.indexOfFirst { it.time >= now }
        val data = if (start >= 0) hourlyData.drop(start).take(24) else hourlyData.take(24)
        data.sortedBy { it.time }
    }
    if (sorted.isEmpty()) return

    val values = remember(sorted, isCelsius) {
        sorted.map { hour ->
            val raw = hour.apparentTemperature ?: hour.temperature
            if (isCelsius) raw.toFloat() else ((raw * 9.0 / 5.0) + 32.0).toFloat()
        }
    }
    val actualValues = remember(sorted, isCelsius) {
        sorted.map { hour ->
            if (isCelsius) hour.temperature.toFloat()
            else ((hour.temperature * 9.0 / 5.0) + 32.0).toFloat()
        }
    }
    val unit = if (isCelsius) "°C" else "°F"
    val minVal = values.min()
    val maxVal = values.max()
    val minIndex = values.indexOf(minVal)
    val maxIndex = values.indexOf(maxVal)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "feels_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics {
                contentDescription = "24 hour feels-like temperature trend, ranging from ${minVal.roundToInt()}$unit to ${maxVal.roundToInt()}$unit"
            },
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
                    text = stringResource(R.string.feels_like_timeline),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SkyBlue))
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarmOrange))
                    Text(
                        text = "Feels",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            val chartWidth = size.width - 32f
                            val startX = 16f
                            val fraction = ((offset.x - startX) / chartWidth).coerceIn(0f, 1f)
                            val idx = (fraction * (values.size - 1)).roundToInt()
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 16f
                val paddingRight = 16f
                val paddingTop = 20f
                val paddingBottom = 28f
                val chartWidth = w - paddingLeft - paddingRight
                val chartHeight = h - paddingTop - paddingBottom
                val range = (maxVal - minVal).coerceAtLeast(1f)

                // Actual temperature series (SkyBlue)
                val actualPath = Path().apply {
                    val firstX = paddingLeft
                    val firstY = paddingTop + chartHeight * (1f - (actualValues.first() - minVal) / range)
                    moveTo(firstX, firstY)
                    actualValues.forEachIndexed { i, v ->
                        val x = paddingLeft + chartWidth * i / (actualValues.size - 1).coerceAtLeast(1)
                        val y = paddingTop + chartHeight * (1f - (v - minVal) / range)
                        if (i > 0) {
                            val prev = actualValues[i - 1]
                            val prevY = paddingTop + chartHeight * (1f - (prev - minVal) / range)
                            val prevX = paddingLeft + chartWidth * (i - 1) / (actualValues.size - 1).coerceAtLeast(1)
                            val cx1 = prevX + (x - prevX) * 0.4f
                            val cx2 = x - (x - prevX) * 0.4f
                            cubicTo(cx1, prevY, cx2, y, x, y)
                        }
                    }
                }
                drawPath(
                    actualPath,
                    SkyBlue.copy(alpha = 0.7f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Feels-like series (WarmOrange, gradient fill)
                val feelsPath = Path().apply {
                    moveTo(paddingLeft, paddingTop + chartHeight)
                    values.forEachIndexed { i, v ->
                        val x = paddingLeft + chartWidth * i / (values.size - 1).coerceAtLeast(1)
                        val y = paddingTop + chartHeight * (1f - (v - minVal) / range)
                        lineTo(x, y)
                    }
                    lineTo(paddingLeft + chartWidth, paddingTop + chartHeight)
                    close()
                }
                val visibleFeelsPath = Path().apply {
                    moveTo(paddingLeft, paddingTop + chartHeight)
                    val visibleCount = (values.size * animProgress).toInt().coerceAtMost(values.size)
                    values.take(visibleCount).forEachIndexed { i, v ->
                        val x = paddingLeft + chartWidth * i / (values.size - 1).coerceAtLeast(1)
                        val y = paddingTop + chartHeight * (1f - (v - minVal) / range)
                        lineTo(x, y)
                    }
                    if (visibleCount > 0) {
                        val lastX = paddingLeft + chartWidth * (visibleCount - 1) / (values.size - 1).coerceAtLeast(1)
                        lineTo(lastX, paddingTop + chartHeight)
                    }
                    close()
                }
                drawPath(
                    visibleFeelsPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(WarmOrange.copy(alpha = 0.35f), WarmOrange.copy(alpha = 0.02f)),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Feels-like line on top
                val visibleFeelsLine = Path().apply {
                    val visibleCount = (values.size * animProgress).toInt().coerceAtMost(values.size)
                    if (visibleCount == 0) return@apply
                    val firstX = paddingLeft
                    val firstY = paddingTop + chartHeight * (1f - (values.first() - minVal) / range)
                    moveTo(firstX, firstY)
                    for (i in 1 until visibleCount) {
                        val v = values[i]
                        val x = paddingLeft + chartWidth * i / (values.size - 1).coerceAtLeast(1)
                        val y = paddingTop + chartHeight * (1f - (v - minVal) / range)
                        val prev = values[i - 1]
                        val prevY = paddingTop + chartHeight * (1f - (prev - minVal) / range)
                        val prevX = paddingLeft + chartWidth * (i - 1) / (values.size - 1).coerceAtLeast(1)
                        val cx1 = prevX + (x - prevX) * 0.4f
                        val cx2 = x - (x - prevX) * 0.4f
                        cubicTo(cx1, prevY, cx2, y, x, y)
                    }
                }
                drawPath(
                    visibleFeelsLine,
                    WarmOrange,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Highlight min/max dots
                listOf(minIndex, maxIndex).distinct().forEach { idx ->
                    val x = paddingLeft + chartWidth * idx / (values.size - 1).coerceAtLeast(1)
                    val y = paddingTop + chartHeight * (1f - (values[idx] - minVal) / range)
                    drawCircle(WarmOrange.copy(alpha = 0.3f), 8f, Offset(x, y))
                    drawCircle(WarmOrange, 4f, Offset(x, y))
                }

                // Hour labels (every 6 hours)
                val labelInterval = (sorted.size / 4).coerceAtLeast(1)
                sorted.forEachIndexed { i, hour ->
                    if (i % labelInterval == 0) {
                        val x = paddingLeft + chartWidth * i / (sorted.size - 1).coerceAtLeast(1)
                        val timeLabel = try {
                            val timeStr = hour.time.substringAfter("T").take(5)
                            val hr = timeStr.split(":")[0].toIntOrNull() ?: 0
                            when {
                                hr == 0 -> "12a"; hr < 12 -> "${hr}a"
                                hr == 12 -> "12p"; else -> "${hr - 12}p"
                            }
                        } catch (_: Exception) { "" }
                        // Draw time label using native canvas (same as WeatherTrends.kt)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#94A3B8")
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(timeLabel, x, h - 6f, paint)
                        }
                    }
                }

                // Selection indicator
                selectedIndex?.let { idx ->
                    if (idx < values.size) {
                        val x = paddingLeft + chartWidth * idx / (values.size - 1).coerceAtLeast(1)
                        val v = values[idx]
                        val actualV = actualValues[idx]
                        val y = paddingTop + chartHeight * (1f - (v - minVal) / range)
                        val actualY = paddingTop + chartHeight * (1f - (actualV - minVal) / range)
                        // Vertical line
                        drawLine(
                            Color.White.copy(alpha = 0.3f),
                            Offset(x, paddingTop),
                            Offset(x, paddingTop + chartHeight),
                            strokeWidth = 1f
                        )
                        // Dots
                        drawCircle(WarmOrange, 6f, Offset(x, y))
                        drawCircle(Color.White, 2.5f, Offset(x, y))
                        drawCircle(SkyBlue, 5f, Offset(x, actualY))
                        // Value label using native canvas (same as WeatherTrends.kt)
                        val labelText = "${v.roundToInt()}$unit  /  ${actualV.roundToInt()}$unit"
                        val labelY = (minOf(y, actualY) - 12f).coerceAtLeast(14f)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                            drawText(labelText, x, labelY, paint)
                        }
                    }
                }
            }
        }
    }
}