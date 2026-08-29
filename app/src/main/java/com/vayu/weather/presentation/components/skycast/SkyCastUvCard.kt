package com.vayu.weather.presentation.components.skycast

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.ui.theme.SkyCastColors
import com.vayu.weather.ui.theme.SkyCastTokens
import kotlin.math.roundToInt

/**
 * SkyCast UV Index card.
 *
 * Falls back to the day's peak UV from the daily forecast when the current
 * hour's UV is not available. Provides a category label, safe-exposure
 * guidance and a small hourly strip for context.
 *
 * Note: Safe-exposure numbers are general guidance and not medical advice.
 */
@Composable
fun SkyCastUvCard(
    currentUv: Double?,
    dailyPeakUv: Double?,
    hourly: List<HourlyWeather>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    val uv = currentUv ?: dailyPeakUv ?: 0.0
    val color = SkyCastColors.forUvIndex(uv)
    val category = uvCategory(uv)

    SkyCastCard(contentPadding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkyCastSectionHeader(title = "UV index", subtitle = "Peak today: ${(dailyPeakUv ?: 0.0).roundToInt()}")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "${uv.roundToInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = safeExposure(uv),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SPF ${spfRecommendation(uv)}+ sunscreen recommended.",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }

            // Color scale
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                com.vayu.weather.ui.theme.UvLow,
                                com.vayu.weather.ui.theme.UvModerate,
                                com.vayu.weather.ui.theme.UvHigh,
                                com.vayu.weather.ui.theme.UvVeryHigh,
                                com.vayu.weather.ui.theme.UvExtreme
                            )
                        )
                    )
            ) {
                // Indicator
                val pos = (uv.toFloat() / 11f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth(pos),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(2.dp)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

private fun uvCategory(uv: Double): String = when {
    uv < 3 -> "Low"
    uv < 6 -> "Moderate"
    uv < 8 -> "High"
    uv < 11 -> "Very High"
    else -> "Extreme"
}

private fun safeExposure(uv: Double): String = when {
    uv < 3 -> "Low risk. No protection needed."
    uv < 6 -> "Moderate. Stay in shade near midday."
    uv < 8 -> "High. Reduce time in the sun between 10am–4pm."
    uv < 11 -> "Very high. Extra protection — shirt, sunscreen, hat."
    else -> "Extreme. Avoid sun exposure between 10am–4pm."
}

private fun spfRecommendation(uv: Double): Int = when {
    uv < 3 -> 15
    uv < 6 -> 30
    uv < 8 -> 30
    uv < 11 -> 50
    else -> 50
}
