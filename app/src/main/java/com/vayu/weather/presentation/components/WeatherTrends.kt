package com.vayu.weather.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.presentation.weather.getWeatherIcon
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.WarmOrange
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class TrendType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TEMPERATURE("Temp", Icons.Rounded.Thermostat),
    WIND("Wind", Icons.Rounded.Air),
    PRECIPITATION("Rain", Icons.Rounded.WaterDrop)
}

@Composable
fun WeatherTrends(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean = true,
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val trendType = TrendType.entries[selectedTab]

    // Compute chart values based on selected tab
    val chartValues = remember(sorted, trendType, isCelsius) {
        sorted.map { hour ->
            when (trendType) {
                TrendType.TEMPERATURE -> {
                    if (isCelsius) hour.temperature.toFloat()
                    else ((hour.temperature * 9.0 / 5.0) + 32.0).toFloat()
                }
                TrendType.WIND -> hour.windSpeed?.toFloat() ?: 0f
                TrendType.PRECIPITATION -> (hour.precipitationProbability ?: 0).toFloat()
            }
        }
    }
    val unit = when (trendType) {
        TrendType.TEMPERATURE -> if (isCelsius) "°C" else "°F"
        TrendType.WIND -> "km/h"
        TrendType.PRECIPITATION -> "%"
    }
    val minVal = chartValues.min()
    val maxVal = chartValues.max()
    val minIndex = chartValues.indexOf(minVal)
    val maxIndex = chartValues.indexOf(maxVal)

    // Selected point for tap
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Animate chart drawing
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(sorted) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // Capture theme colors before Canvas (non-composable scope)
    val primaryColor = MaterialTheme.colorScheme.primary
    val inverseSurfaceColor = MaterialTheme.colorScheme.inverseSurface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrendType.entries.forEachIndexed { index, type ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(type.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(type.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (trendType) {
                        TrendType.TEMPERATURE -> stringResource(R.string.temperature_trends)
                        TrendType.WIND -> "Wind Speed"
                        TrendType.PRECIPITATION -> "Precipitation"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${minVal.roundToInt()}$unit — ${maxVal.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(chartValues) {
                        detectTapGestures { offset ->
                            val chartWidth = size.width - 40f
                            val startX = 20f
                            val fraction = ((offset.x - startX) / chartWidth).coerceIn(0f, 1f)
                            val index = (fraction * (chartValues.size - 1)).roundToInt()
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 30f  // Space for temp labels
                val paddingRight = 16f
                val paddingTop = 30f   // Space for selected value
                val paddingBottom = 30f // Space for time labels
                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom
                val range = (maxVal - minVal).coerceAtLeast(1f)

                // ── Day/Night background shading ──
                sorted.forEachIndexed { index, hour ->
                    val x = paddingLeft + chartWidth * index / (sorted.size - 1).coerceAtLeast(1)
                    val barWidth = chartWidth / sorted.size
                    val isDay = try {
                        val timeStr = hour.time.substringAfter("T").take(5)
                        val h = timeStr.split(":")[0].toIntOrNull() ?: 12
                        h in 6..18
                    } catch (_: Exception) { true }

                    drawRect(
                        color = if (isDay)
                            Color(0xFFFFF8E1).copy(alpha = 0.15f)
                        else
                            Color(0xFF1A237E).copy(alpha = 0.1f),
                        topLeft = Offset(x - barWidth / 2, paddingTop),
                        size = Size(barWidth, chartHeight)
                    )
                }

                // ── Grid lines ──
                for (i in 0..4) {
                    val y = paddingTop + chartHeight * i / 4f
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.15f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f
                    )
                }

                // ── Calculate point positions ──
                val points = chartValues.mapIndexed { index, temp ->
                    val x = paddingLeft + chartWidth * index / (sorted.size - 1).coerceAtLeast(1)
                    val normalizedTemp = (temp - minVal) / range
                    val y = paddingTop + chartHeight * (1f - normalizedTemp)
                    Offset(x, y)
                }

                // Animated visible points
                val visibleCount = (points.size * animProgress.value).toInt().coerceAtMost(points.size)
                val visiblePoints = points.take(visibleCount)

                if (visiblePoints.size >= 2) {
                    // ── Gradient area fill ──
                    val areaPath = Path().apply {
                        moveTo(visiblePoints.first().x, paddingTop + chartHeight)
                        visiblePoints.forEach { lineTo(it.x, it.y) }
                        lineTo(visiblePoints.last().x, paddingTop + chartHeight)
                        close()
                    }

                    // Gradient from SkyBlue to transparent
                    drawPath(
                        areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SkyBlue.copy(alpha = 0.25f),
                                SkyBlue.copy(alpha = 0.02f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )

                    // ── Smooth bezier line ──
                    val linePath = Path().apply {
                        moveTo(visiblePoints.first().x, visiblePoints.first().y)
                        for (i in 1 until visiblePoints.size) {
                            val prev = visiblePoints[i - 1]
                            val curr = visiblePoints[i]
                            val controlX1 = prev.x + (curr.x - prev.x) * 0.4f
                            val controlX2 = curr.x - (curr.x - prev.x) * 0.4f
                            cubicTo(controlX1, prev.y, controlX2, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        linePath,
                        SkyBlue,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // ── Data points ──
                    visiblePoints.forEachIndexed { index, point ->
                        val isSelected = selectedIndex == index
                        val isMin = index == minIndex
                        val isMax = index == maxIndex

                        if (isMin || isMax || isSelected) {
                            // Highlight ring
                            drawCircle(
                                color = if (isMax) WarmOrange.copy(alpha = 0.3f)
                                    else if (isMin) SkyBlue.copy(alpha = 0.3f)
                                    else primaryColor.copy(alpha = 0.3f),
                                radius = if (isSelected) 16f else 12f,
                                center = point
                            )
                            // Dot
                            drawCircle(
                                color = if (isMax) WarmOrange
                                    else if (isMin) SkyBlue
                                    else primaryColor,
                                radius = if (isSelected) 6f else 5f,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = point
                            )
                        }
                    }

                    // ── Selected value tooltip ──
                    selectedIndex?.let { idx ->
                        if (idx < visiblePoints.size) {
                            val point = visiblePoints[idx]
                            val temp = chartValues[idx]

                            // Tooltip background
                            val tooltipWidth = 90f
                            val tooltipHeight = 40f
                            val tooltipX = (point.x - tooltipWidth / 2).coerceIn(0f, width - tooltipWidth)
                            val tooltipY = (point.y - tooltipHeight - 10f).coerceAtLeast(0f)

                            drawRoundRect(
                                color = inverseSurfaceColor.copy(alpha = 0.9f),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(8f)
                            )

                            // Tooltip text
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 14f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                paint.textSize = 28f
                                drawText(
                                    "${temp.roundToInt()}$unit",
                                    tooltipX + tooltipWidth / 2,
                                    tooltipY + 28f,
                                    paint
                                )
                            }
                        }
                    }

                    // ── Min/Max labels ──
                    if (minIndex < points.size) {
                        val p = points[minIndex]
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#38BDF8")
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                            drawText(
                                "${minVal.roundToInt()}$unit",
                                p.x,
                                p.y + 32f,
                                paint
                            )
                        }
                    }
                    if (maxIndex < points.size && maxIndex != minIndex) {
                        val p = points[maxIndex]
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#F97316")
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                            drawText(
                                "${maxVal.roundToInt()}$unit",
                                p.x,
                                p.y - 14f,
                                paint
                            )
                        }
                    }

                    // ── Time labels on x-axis ──
                    val labelInterval = (sorted.size / 6).coerceAtLeast(1)
                    sorted.forEachIndexed { index, hour ->
                        if (index % labelInterval == 0 && index < points.size) {
                            val x = points[index].x
                            val timeLabel = try {
                                val timeStr = hour.time.substringAfter("T").take(5)
                                val h = timeStr.split(":")[0].toIntOrNull() ?: 0
                                when {
                                    h == 0 -> "12a"
                                    h < 12 -> "${h}a"
                                    h == 12 -> "12p"
                                    else -> "${h - 12}p"
                                }
                            } catch (_: Exception) { "--" }

                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 20f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                drawText(
                                    timeLabel,
                                    x,
                                    height - 6f,
                                    paint
                                )
                            }
                        }
                    }
                }
            }

            // ── Legend row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(SkyBlue)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Low: ${minVal.roundToInt()}$unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = "Tap chart for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(WarmOrange)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "High: ${maxVal.roundToInt()}$unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
