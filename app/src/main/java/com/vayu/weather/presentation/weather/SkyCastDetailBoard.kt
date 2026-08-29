package com.vayu.weather.presentation.weather

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.components.skycast.SkyCastCard
import com.vayu.weather.presentation.components.skycast.SkyCastMetricCard
import com.vayu.weather.presentation.components.skycast.SkyCastSectionHeader
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import com.vayu.weather.ui.theme.WarmOrange
import com.vayu.weather.ui.theme.WarningAmber
import kotlin.math.roundToInt

/**
 * SkyCast detail board — a complete grid of secondary weather metrics.
 *
 * Replaces the ad-hoc card list in the detail screen with a consistent
 * 2-column responsive grid driven by the same SkyCastMetricCard used on
 * the home dashboard.
 */
@Composable
fun SkyCastDetailBoard(
    info: WeatherInfo,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkyCastSectionHeader(title = "Weather details", subtitle = "All measurements")
        Spacer(modifier = Modifier.height(4.dp))

        val temp = convertTemp(info.current.temperature, isCelsius)
        val feels = info.current.apparentTemperature?.let { convertTemp(it, isCelsius) }
        val humidity = info.current.humidity?.roundToInt()
        val dew = info.current.dewPoint?.let { convertTemp(it, isCelsius) }
        val pressure = info.current.surfacePressure?.roundToInt()
        val visibility = info.current.visibility?.let { v ->
            if (v < 1000) "${(v / 100.0).roundToInt() * 100}m"
            else "${(v / 1000.0).let { if (it - it.toInt() >= 0.5) it.toInt() + 1 else it.toInt() }} km"
        }
        val wind = info.current.windSpeed?.roundToInt()
        val gusts = info.current.windGusts?.roundToInt()
        val cloud = info.current.visibility?.let { (100 - (it / 10000).toInt().coerceIn(0, 100)) }
        val rain = info.hourly.firstOrNull()?.precipitationProbability ?: 0

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Thermostat,
                label = "Temperature",
                value = "${temp}°",
                accent = WarmOrange,
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Thermostat,
                label = "Feels like",
                value = feels?.let { "${it}°" } ?: "—",
                accent = WarningAmber,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.WaterDrop,
                label = "Humidity",
                value = humidity?.let { "$it%" } ?: "—",
                accent = SkyBlue,
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.AcUnit,
                label = "Dew point",
                value = dew?.let { "${it}°" } ?: "—",
                accent = SkyBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Air,
                label = "Wind",
                value = wind?.let { "$it" } ?: "—",
                subtitle = "km/h",
                accent = com.vayu.weather.ui.theme.SoftLavender,
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Speed,
                label = "Gusts",
                value = gusts?.let { "$it" } ?: "—",
                subtitle = "km/h",
                accent = com.vayu.weather.ui.theme.SoftLavender,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.Compress,
                label = "Pressure",
                value = pressure?.let { "$it" } ?: "—",
                subtitle = "hPa",
                accent = FreshGreen,
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Visibility,
                label = "Visibility",
                value = visibility?.let { "$it" } ?: "—",
                subtitle = "km",
                accent = FreshGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkyCastMetricCard(
                icon = Icons.Rounded.WaterDrop,
                label = "Rain probability",
                value = "$rain%",
                accent = SkyBlue,
                modifier = Modifier.weight(1f)
            )
            SkyCastMetricCard(
                icon = Icons.Rounded.Compress,
                label = "Cloud cover",
                value = "${cloud ?: 0}%",
                accent = com.vayu.weather.ui.theme.StormGray,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
