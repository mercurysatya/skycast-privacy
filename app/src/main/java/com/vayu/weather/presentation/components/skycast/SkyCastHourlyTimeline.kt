package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.WarmOrange
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * SkyCast premium hourly timeline.
 *
 * Shows a smooth temperature graph with per-hour tiles below. Tapping a tile
 * (or its column on the graph) selects that hour and the parent can show a
 * details panel.
 */
@Composable
fun SkyCastHourlyTimeline(
    hourly: List<HourlyWeather>,
    isCelsius: Boolean,
    selectedIndex: Int? = null,
    onSelectHour: (Int, HourlyWeather) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return

    val sorted = remember(hourly) { hourly.sortedBy { it.time } }
    val hoursToShow = remember(sorted) { sorted.take(24) }
    val hourWidth = 64.dp

    val temps = hoursToShow.map { it.temperature }
    val (minT, maxT) = remember(temps) {
        val min = temps.min()
        val max = temps.max()
        val pad = ((max - min) * 0.1).coerceAtLeast(0.5)
        (min - pad) to (max + pad)
    }
    val rainProbs = hoursToShow.map { it.precipitationProbability ?: 0 }
    val maxRainProb = remember(rainProbs) { (rainProbs.maxOrNull() ?: 0).coerceAtLeast(10) }

    var selected by remember(selectedIndex) { mutableIntStateOf(selectedIndex ?: 0) }
    val clamped = selected.coerceIn(0, hoursToShow.lastIndex)

    SkyCastCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkyCastSectionHeader(title = "Hourly", subtitle = "Tap a column for details")
                Text(
                    text = "${convertTemp(temps.max(), isCelsius)}° / ${convertTemp(temps.min(), isCelsius)}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Graph + per-hour tiles scroll together
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp)
            ) {
                hoursToShow.forEachIndexed { i, h ->
                    val isSelected = i == clamped
                    val time = parseHourlyTime(h.time)
                    val now = LocalDateTime.now()
                    val isNow = time != null &&
                        time.hour == now.hour && time.dayOfYear == now.dayOfYear
                    val isDayHour = try {
                        val hour = java.time.LocalTime.parse(h.time.substringAfter("T")).hour
                        hour in 6..18
                    } catch (_: Exception) { true }
                    val hourWeatherDesc = localizedWeatherDescription(h.weatherCode, isDayHour)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(hourWidth)
                            .clickable {
                                selected = i
                                onSelectHour(i, h)
                            }
                            .semantics {
                                val tLabel = when {
                                    isNow -> "Now"
                                    time != null -> time.format(DateTimeFormatter.ofPattern("h a"))
                                    else -> h.time.takeLast(5)
                                }
                                val tValue = if (isCelsius) "${h.temperature.roundToInt()}°C"
                                    else "${(h.temperature * 9.0 / 5.0 + 32).roundToInt()}°F"
                                val desc = buildString {
                                    append(tLabel)
                                    append(", ")
                                    append(tValue)
                                    append(", ")
                                    append(hourWeatherDesc)
                                    val p = h.precipitationProbability ?: 0
                                    if (p > 0) append(", ${p}% chance of rain")
                                }
                                contentDescription = desc
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        // Time label
                        Text(
                            text = when {
                                isNow -> "Now"
                                time != null -> time.format(DateTimeFormatter.ofPattern("h a"))
                                else -> h.time.takeLast(5)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNow) SkyBlue else Color.White.copy(alpha = 0.55f),
                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Icon
                        Icon(
                            imageVector = getWeatherIcon(h.weatherCode, isDayHour),
                            contentDescription = localizedWeatherDescription(h.weatherCode, isDayHour),
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Temperature graph column
                        GraphColumn(
                            index = i,
                            count = hoursToShow.size,
                            temperature = h.temperature,
                            minT = minT,
                            maxT = maxT,
                            precipPercent = h.precipitationProbability ?: 0,
                            maxPrecip = maxRainProb,
                            isCelsius = isCelsius,
                            isSelected = isSelected,
                            isNow = isNow,
                            width = hourWidth
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Rain probability
                        Text(
                            text = if ((h.precipitationProbability ?: 0) >= 20) "${h.precipitationProbability}%" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBlue.copy(alpha = if ((h.precipitationProbability ?: 0) >= 20) 0.95f else 0.3f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ── Selected hour details ──
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SkyCastTokens.RadiusLg))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(12.dp)
            ) {
                val sel = hoursToShow[clamped]
                val label = parseHourlyTime(sel.time)
                val timeLabel = label?.format(DateTimeFormatter.ofPattern("h a")) ?: sel.time.takeLast(5)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getWeatherIcon(sel.weatherCode, try {
                                val hour = java.time.LocalTime.parse(sel.time.substringAfter("T")).hour
                                hour in 6..18
                            } catch (_: Exception) { true }),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$timeLabel · ${localizedWeatherDescription(sel.weatherCode, try {
                                val hour = java.time.LocalTime.parse(sel.time.substringAfter("T")).hour
                                hour in 6..18
                            } catch (_: Exception) { true })}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HourlyDetailCol("Temp", "${convertTemp(sel.temperature, isCelsius)}°")
                        HourlyDetailCol("Feels", "${convertTemp(sel.apparentTemperature ?: sel.temperature, isCelsius)}°")
                        HourlyDetailCol("Rain", "${sel.precipitationProbability ?: 0}%")
                        HourlyDetailCol("Wind", sel.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "—")
                        HourlyDetailCol("Humidity", sel.humidity?.let { "${it.roundToInt()}%" } ?: "—")
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphColumn(
    index: Int,
    count: Int,
    temperature: Double,
    minT: Double,
    maxT: Double,
    precipPercent: Int,
    maxPrecip: Int,
    isCelsius: Boolean,
    isSelected: Boolean,
    isNow: Boolean,
    width: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(96.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Background column
        Box(
            modifier = Modifier
                .width(width - 12.dp)
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                val w = size.width
                val h = size.height
                val range = (maxT - minT).coerceAtLeast(0.001)
                val y = h - ((temperature - minT) / range * h).toFloat()
                // Vertical tick line
                drawLine(
                    color = Color.White.copy(alpha = if (isSelected) 0.4f else 0.1f),
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = if (isSelected) 3f else 1f
                )
                // Dot
                val dotColor = if (isSelected) SkyBlue else if (isNow) WarmOrange else Color.White.copy(alpha = 0.85f)
                drawCircle(dotColor, 6f, Offset(w / 2f, y))
                if (isSelected) {
                    drawCircle(dotColor.copy(alpha = 0.3f), 12f, Offset(w / 2f, y))
                }
            }
        }
        // Temperature label just above the dot
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            val w = size.width
            val h = size.height
            val range = (maxT - minT).coerceAtLeast(0.001)
            val y = h - ((temperature - minT) / range * h).toFloat()
            // Temp label position
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${convertTemp(temperature, isCelsius)}°",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) SkyBlue else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun HourlyDetailCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * Renders the smooth line graph connecting all hour points. Intended to be
 * rendered behind the per-hour tiles (the dots are already drawn per-column,
 * so this just adds the connecting line + area fill).
 */
@Composable
fun SkyCastHourlyGraph(
    hourly: List<HourlyWeather>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val sorted = remember(hourly) { hourly.sortedBy { it.time } }
    val temps = sorted.map { it.temperature }
    val (minT, maxT) = remember(temps) {
        if (temps.isEmpty()) return@remember 0.0 to 1.0
        val min = temps.min()
        val max = temps.max()
        val pad = ((max - min) * 0.1).coerceAtLeast(0.5)
        (min - pad) to (max + pad)
    }
    Canvas(modifier = modifier.height(80.dp).fillMaxWidth()) {
        if (sorted.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val range = (maxT - minT).coerceAtLeast(0.001)
        val step = w / (sorted.size - 1)
        val points = sorted.mapIndexed { i, hw ->
            val x = i * step
            val y = h - ((hw.temperature - minT) / range * h).toFloat()
            Offset(x, y)
        }
        // Smooth path
        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            val mid = Offset((prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
            path.quadraticBezierTo(prev.x, prev.y, mid.x, mid.y)
        }
        path.lineTo(points.last().x, points.last().y)
        // Fill area under the line
        val area = Path().apply {
            addPath(path)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(SkyBlue.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        // Stroke
        drawPath(
            path = path,
            color = SkyBlue,
            style = Stroke(width = 3f)
        )
    }
}

private fun parseHourlyTime(time: String): LocalDateTime? = try {
    LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
} catch (e: Exception) {
    null
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
