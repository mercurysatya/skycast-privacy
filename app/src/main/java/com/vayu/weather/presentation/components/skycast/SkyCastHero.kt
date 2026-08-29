package com.vayu.weather.presentation.components.skycast

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.util.getWeatherIcon
import com.vayu.weather.presentation.util.localizedWeatherDescription
import com.vayu.weather.ui.theme.SkyCastTokens
import kotlin.math.roundToInt

/**
 * SkyCast premium hero — editorial weather aesthetic.
 *
 * Layout (compact, no location redundancy, no duplicate pills):
 *   • Large icon (smaller than the previous version)
 *   • Elegant temperature in a thin font
 *   • Condition
 *   • Feels like
 *   • H · L · Feels (the only key facts up here)
 *   • Vs-yesterday pill
 *
 * The actual location, alert count, and freshness are rendered by the
 * single top-bar header in the home screen.
 */
@Composable
fun SkyCastHero(
    info: WeatherInfo,
    isCelsius: Boolean,
    previousDayTempC: Double?,
    modifier: Modifier = Modifier
) {
    val condition = localizedWeatherDescription(info.current.weatherCode, info.current.isDay)
    val temp = convertTemp(info.current.temperature, isCelsius)
    val feels = info.current.apparentTemperature?.let { convertTemp(it, isCelsius) }
    val today = info.daily.firstOrNull()
    val high = today?.maxTemp?.let { convertTemp(it, isCelsius) }
    val low = today?.minTemp?.let { convertTemp(it, isCelsius) }

    val a11y = buildString {
        append(condition).append(", ")
        append(temp).append(" degrees")
        if (feels != null) append(", feels like ").append(feels).append(" degrees")
        if (high != null && low != null) append(", high ").append(high).append(", low ").append(low)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SkyCastTokens.Space20)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(SkyCastTokens.Space8))

        // Icon + temperature, side-by-side
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherIconBadge(info.current.weatherCode, info.current.isDay)
            }
            Spacer(modifier = Modifier.width(SkyCastTokens.Space12))
            Text(
                text = "$temp°",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                ),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(SkyCastTokens.Space4))

        Text(
            text = condition,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )

        if (feels != null) {
            Spacer(modifier = Modifier.height(SkyCastTokens.Space2))
            Text(
                text = "Feels like ${feels}°",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }

        Spacer(modifier = Modifier.height(SkyCastTokens.Space12))

        // Three key facts: H · L · Feels (no humidity/wind — those live in details)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                SkyCastTokens.Space8,
                Alignment.CenterHorizontally
            )
        ) {
            if (high != null) HeroPill(label = "H", value = "${high}°")
            if (low != null) HeroPill(label = "L", value = "${low}°")
            if (feels != null) HeroPill(label = "Feels", value = "${feels}°")
        }

        // Vs-yesterday pill
        previousDayTempC?.let { prev ->
            val current = info.current.temperature
            val diff = (current - prev).roundToInt()
            val (label, accent) = when {
                diff > 0 -> "Warmer by ${diff}°" to com.vayu.weather.ui.theme.SunsetRed
                diff < 0 -> "Cooler by ${kotlin.math.abs(diff)}°" to com.vayu.weather.ui.theme.SkyBlue
                else -> "Same as yesterday" to Color.White.copy(alpha = 0.5f)
            }
            Spacer(modifier = Modifier.height(SkyCastTokens.Space8))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = SkyCastTokens.Space12, vertical = SkyCastTokens.Space4)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(SkyCastTokens.Space8))
    }
}

@Composable
private fun HeroPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = SkyCastTokens.Space12, vertical = SkyCastTokens.Space6)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeatherIconBadge(weatherCode: Int, isDay: Boolean) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.0f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getWeatherIcon(weatherCode, isDay),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(56.dp)
        )
    }
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
