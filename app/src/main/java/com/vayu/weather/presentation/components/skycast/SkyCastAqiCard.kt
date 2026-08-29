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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.ui.theme.SkyCastColors
import com.vayu.weather.ui.theme.SkyCastTokens
import kotlin.math.roundToInt

/**
 * SkyCast Air Quality card.
 *
 * Renders a colored gauge for the active AQI score, a category label, a
 * short health guidance sentence, and a row of pollutant micro-bars. Always
 * supplements the gauge with a non-color identifier (the category label) so
 * the data is still accessible to color-blind users.
 */
@Composable
fun SkyCastAqiCard(
    airQuality: AirQuality?,
    modifier: Modifier = Modifier
) {
    val aqi = airQuality?.usAqi ?: airQuality?.europeanAqi
    val a11ySummary = if (airQuality != null && aqi != null) {
        "Air quality ${airQuality.aqiLabel}, $aqi"
    } else null

    SkyCastCard(contentPadding = PaddingValues(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (a11ySummary != null) {
                        Modifier.semantics(mergeDescendants = true) {
                            contentDescription = a11ySummary
                        }
                    } else Modifier
                )
        ) {
            SkyCastSectionHeader(
                title = "Air quality",
                subtitle = airQuality?.aqiLabel?.let { "Category: $it" }
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (airQuality == null || (airQuality.usAqi == null && airQuality.europeanAqi == null)) {
                Text(
                    text = "Air quality data is currently unavailable for this location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                return@Column
            }

            val isEuropean = airQuality.usAqi == null && airQuality.europeanAqi != null
            val aqi = airQuality.usAqi ?: airQuality.europeanAqi ?: 0
            val maxAqi = if (isEuropean) 100 else 500
            val fraction = (aqi.toFloat() / maxAqi).coerceIn(0f, 1f)
            val color = if (isEuropean) SkyCastColors.forEuAqi(aqi) else SkyCastColors.forUsAqi(aqi)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(96.dp)) {
                        val stroke = 14f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        val topLeft = Offset(stroke / 2f, stroke / 2f)
                        // Background arc
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                        // Filled portion (rainbow-like gradient)
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    com.vayu.weather.ui.theme.AqiGood,
                                    com.vayu.weather.ui.theme.AqiFair,
                                    com.vayu.weather.ui.theme.AqiModerate,
                                    com.vayu.weather.ui.theme.AqiPoor,
                                    com.vayu.weather.ui.theme.AqiVeryPoor,
                                    com.vayu.weather.ui.theme.AqiSevere
                                )
                            ),
                            startAngle = 135f,
                            sweepAngle = 270f * fraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$aqi",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "AQI",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = airQuality.aqiLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = healthGuidance(aqi),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Pollutants
            Spacer(modifier = Modifier.height(12.dp))
            val pollutants = listOf(
                Triple("PM2.5", airQuality.pm25, 75.0),
                Triple("PM10", airQuality.pm10, 150.0),
                Triple("NO₂", airQuality.nitrogenDioxide, 200.0),
                Triple("O₃", airQuality.ozone, 180.0),
                Triple("SO₂", airQuality.sulphurDioxide, 350.0),
                Triple("CO", airQuality.carbonMonoxide, 15000.0)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pollutants.forEach { (label, value, max) ->
                    PollutantBar(label = label, value = value, max = max)
                }
            }
        }
    }
}

@Composable
private fun PollutantBar(label: String, value: Double?, max: Double) {
    val fraction = ((value ?: 0.0) / max).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        fraction < 0.4f -> com.vayu.weather.ui.theme.AqiGood
        fraction < 0.7f -> com.vayu.weather.ui.theme.AqiModerate
        else -> com.vayu.weather.ui.theme.AqiPoor
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = value?.let { "${formatPollutant(label, it)}" } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(4.dp)
                    .background(color)
            )
        }
    }
}

private fun formatPollutant(label: String, value: Double): String {
    val v = if (value < 10) (value * 10).toInt() / 10.0 else value.roundToInt().toDouble()
    return when (label) {
        "CO" -> "${(v / 1000.0).let { if (it < 10) (it * 10).toInt() / 10.0 else it.roundToInt().toDouble() }} mg/m³"
        else -> "$v μg/m³"
    }
}

private fun healthGuidance(aqi: Int): String = when {
    aqi <= 50 -> "Air quality is satisfactory. Air pollution poses little or no risk."
    aqi <= 100 -> "Acceptable. Sensitive individuals may experience minor irritation."
    aqi <= 150 -> "Members of sensitive groups may experience health effects. Consider reducing prolonged outdoor exertion."
    aqi <= 200 -> "Some members of the general public may experience health effects; sensitive groups may experience more serious effects."
    aqi <= 300 -> "Health alert: risk of health effects is increased for everyone."
    else -> "Health warning of emergency conditions: everyone is more likely to be affected."
}
