package com.vayu.weather.presentation.components.skycast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.ui.theme.FreshGreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.WarmOrange
import com.vayu.weather.ui.theme.WarningAmber
import kotlin.math.roundToInt

private data class MetricInfo(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color,
    val explanation: String,
    val detail: String?
)

/**
 * Tappable metric details. Opens a [ModalBottomSheet] explaining the metric
 * the user just tapped. We deliberately show what the provider actually
 * returned — no fabricated trend or historical data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyCastMetricDetailSheet(
    metric: String?,
    info: WeatherInfo,
    isCelsius: Boolean,
    onDismiss: () -> Unit
) {
    if (metric == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val data = remember(metric, info) { metricData(metric, info, isCelsius) }
    if (data == null) {
        onDismiss()
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(data.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = data.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = data.value,
                        style = MaterialTheme.typography.headlineSmall,
                        color = data.accent
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Text(
                text = data.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (data.detail != null) {
                Spacer(Modifier.size(12.dp))
                Text(
                    text = data.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

private fun metricData(
    metric: String,
    info: WeatherInfo,
    isCelsius: Boolean
): MetricInfo? = when (metric) {
    "humidity" -> {
        val h = info.current.humidity?.roundToInt() ?: return null
        MetricInfo(
            title = "Humidity",
            value = "$h%",
            icon = Icons.Rounded.WaterDrop,
            accent = SkyBlue,
            explanation = "Relative humidity tells you how saturated the air is at the current temperature. " +
                "Higher values mean the air holds more moisture; lower values mean the air is dry.",
            detail = interpretHumidity(h, info.current.temperature, isCelsius)
        )
    }
    "wind" -> {
        val wind = info.current.windSpeed?.roundToInt() ?: return null
        val dir = info.current.windDirection?.let { formatCardinal(it) } ?: "—"
        MetricInfo(
            title = "Wind",
            value = "$wind km/h",
            icon = Icons.Rounded.Air,
            accent = com.vayu.weather.ui.theme.SoftLavender,
            explanation = "Wind speed at 10 m above ground. Direction is the compass bearing the wind is coming from. " +
                "Values are provider-reported for the current observation — not extrapolated.",
            detail = "Direction: $dir"
        )
    }
    "pressure" -> {
        val p = info.current.surfacePressure?.roundToInt() ?: return null
        MetricInfo(
            title = "Pressure",
            value = "$p hPa",
            icon = Icons.Rounded.Compress,
            accent = WarmOrange,
            explanation = "Surface atmospheric pressure. A falling barometer generally signals an incoming low-pressure system; " +
                "rising pressure usually means improving weather.",
            detail = interpretPressure(p)
        )
    }
    "visibility" -> {
        val v = info.current.visibility?.let { (it / 1000).roundToInt() } ?: return null
        MetricInfo(
            title = "Visibility",
            value = "$v km",
            icon = Icons.Rounded.Visibility,
            accent = FreshGreen,
            explanation = "The furthest horizontal distance at which objects can be clearly identified. " +
                "Lower values usually mean fog, precipitation, or haze.",
            detail = interpretVisibility(v)
        )
    }
    "dew" -> {
        val dewC = info.current.dewPoint ?: return null
        val dew = if (isCelsius) dewC.roundToInt() else (dewC * 9.0 / 5.0 + 32).roundToInt()
        val tempC = info.current.temperature
        val temp = if (isCelsius) tempC.roundToInt() else (tempC * 9.0 / 5.0 + 32).roundToInt()
        val spread = temp - dew
        MetricInfo(
            title = "Dew point",
            value = "$dew°",
            icon = Icons.Rounded.WaterDrop,
            accent = WarningAmber,
            explanation = "The temperature at which the air becomes saturated and dew forms. " +
                "A small spread between the air temperature and the dew point means the air is humid; " +
                "a large spread means it is dry.",
            detail = "Spread to air temp: $spread°. ${if (spread < 3) "Air is saturated — fog likely." else if (spread > 10) "Air is dry." else "Comfortable."}"
        )
    }
    else -> null
}

private fun formatCardinal(deg: Double): String = when {
    deg < 22.5 || deg >= 337.5 -> "North"
    deg < 67.5 -> "Northeast"
    deg < 112.5 -> "East"
    deg < 157.5 -> "Southeast"
    deg < 202.5 -> "South"
    deg < 247.5 -> "Southwest"
    deg < 292.5 -> "West"
    else -> "Northwest"
}

private fun interpretHumidity(h: Int, tempC: Double, isCelsius: Boolean): String = when {
    h < 30 -> "Very dry air. May cause skin or throat irritation."
    h < 50 -> "Comfortable range."
    h < 70 -> "Slightly humid. Most people will feel fine."
    h > 85 -> "Muggy — feels stickier than the thermometer suggests, especially above ${if (isCelsius) "26°" else "79°"}."
    else -> "Humid — feels noticeably heavier."
}

private fun interpretPressure(p: Int): String = when {
    p >= 1020 -> "High pressure — typically fair, stable weather."
    p >= 1013 -> "Normal pressure — changeable but quiet conditions likely."
    p >= 1005 -> "Slightly low — fronts and precipitation possible."
    else -> "Low pressure — unsettled weather, rain or storms likely."
}

private fun interpretVisibility(km: Int): String = when {
    km >= 20 -> "Excellent — typical of clear, dry air."
    km >= 10 -> "Good — most outdoor activities unaffected."
    km >= 4 -> "Reduced — fog, haze, or light precipitation likely."
    km >= 1 -> "Poor — driving will be difficult."
    else -> "Near zero — dense fog or heavy precipitation."
}
