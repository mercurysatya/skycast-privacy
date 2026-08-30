package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.data.converter.WindConverter
import com.vayu.weather.presentation.weather.WindUnit
import com.vayu.weather.ui.theme.CompassEast
import com.vayu.weather.ui.theme.CompassNorth
import com.vayu.weather.ui.theme.CompassSouth
import com.vayu.weather.ui.theme.CompassWest
import com.vayu.weather.ui.theme.SkyCastColors
import kotlin.math.roundToInt

/**
 * SkyCast wind compass.
 *
 * Renders a circular compass rose with a directional needle. Always shows a
 * text label in addition to the needle position so the value is accessible
 * to color-blind users.
 */
@Composable
fun SkyCastWindCompass(
    speedKph: Double?,
    directionDeg: Double?,
    gustsKph: Double?,
    windUnit: WindUnit = WindUnit.KPH,
    modifier: Modifier = Modifier
) {
    val direction = directionDeg ?: 0.0
    val speedKmh = speedKph ?: 0.0
    val color = SkyCastColors.forWindKph(speedKmh)
    val beaufort = beaufortLevel(speedKmh)
    val cardinal = cardinalDirection(direction)

    // Convert display values based on user preference
    val displaySpeed = when (windUnit) {
        WindUnit.KPH -> speedKmh.roundToInt()
        WindUnit.MPH -> WindConverter.kmhToMph(speedKmh).roundToInt()
        WindUnit.MS -> WindConverter.kmhToMs(speedKmh).roundToInt()
        WindUnit.KNOTS -> WindConverter.kmhToKnots(speedKmh).roundToInt()
    }
    val displayUnit = when (windUnit) {
        WindUnit.KPH -> "km/h"
        WindUnit.MPH -> "mph"
        WindUnit.MS -> "m/s"
        WindUnit.KNOTS -> "knots"
    }
    val displayGust = gustsKph?.let { gust ->
        when (windUnit) {
            WindUnit.KPH -> gust.roundToInt()
            WindUnit.MPH -> WindConverter.kmhToMph(gust).roundToInt()
            WindUnit.MS -> WindConverter.kmhToMs(gust).roundToInt()
            WindUnit.KNOTS -> WindConverter.kmhToKnots(gust).roundToInt()
        }
    }

    SkyCastCard(contentPadding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkyCastSectionHeader(title = "Wind", subtitle = "Beaufort scale · $cardinal")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val r = size.width / 2f
                        val center = Offset(r, r)
                        // Outer ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = r - 4f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                        // Inner ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = r * 0.7f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                        // Cardinal markers
                        for (i in 0 until 4) {
                            val angle = i * 90f - 90f
                            rotate(angle, center) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(center.x, center.y - r + 2f),
                                    end = Offset(center.x, center.y - r + 12f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                        // Needle (rotated to wind direction)
                        rotate(direction.toFloat(), center) {
                            drawLine(
                                color = color,
                                start = Offset(center.x, center.y + r * 0.55f),
                                end = Offset(center.x, center.y - r * 0.7f),
                                strokeWidth = 6f
                            )
                            drawCircle(
                                color = color,
                                radius = 4f,
                                center = Offset(center.x, center.y - r * 0.7f)
                            )
                        }
                        // Center dot
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = center
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$displaySpeed",
                        style = MaterialTheme.typography.displaySmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = displayUnit,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = beaufort,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (displayGust != null) {
                        Text(
                            text = "Gusts $displayGust $displayUnit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun cardinalDirection(deg: Double): String = when {
    deg < 22.5 || deg >= 337.5 -> "N"
    deg < 67.5 -> "NE"
    deg < 112.5 -> "E"
    deg < 157.5 -> "SE"
    deg < 202.5 -> "S"
    deg < 247.5 -> "SW"
    deg < 292.5 -> "W"
    else -> "NW"
}

private fun beaufortLevel(kph: Double): String = when {
    kph < 1 -> "Calm"
    kph < 6 -> "Light air"
    kph < 12 -> "Light breeze"
    kph < 20 -> "Gentle breeze"
    kph < 29 -> "Moderate breeze"
    kph < 39 -> "Fresh breeze"
    kph < 50 -> "Strong breeze"
    kph < 62 -> "Near gale"
    kph < 75 -> "Gale"
    kph < 89 -> "Strong gale"
    kph < 103 -> "Storm"
    kph < 118 -> "Violent storm"
    else -> "Hurricane"
}
