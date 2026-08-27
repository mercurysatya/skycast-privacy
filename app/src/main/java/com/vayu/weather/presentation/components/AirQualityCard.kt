package com.vayu.weather.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vayu.weather.R
import com.vayu.weather.domain.model.AirQuality

@Composable
fun AirQualityCard(
    airQuality: AirQuality?,
    modifier: Modifier = Modifier
) {
    if (airQuality == null) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.air_quality),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = airQuality.aqiLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = aqiColor(airQuality.aqiColorIndex)
                        )
                        Text(
                            text = stringResource(R.string.european_aqi, airQuality.europeanAqi?.toString() ?: "--"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PollutantItem("PM2.5", airQuality.pm25, "μg/m³")
                    PollutantItem("PM10", airQuality.pm10, "μg/m³")
                    PollutantItem("NO₂", airQuality.nitrogenDioxide, "μg/m³")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PollutantItem("O₃", airQuality.ozone, "μg/m³")
                    PollutantItem("SO₂", airQuality.sulphurDioxide, "μg/m³")
                    PollutantItem("CO", airQuality.carbonMonoxide, "μg/m³")
                }
            }
        }
    }
}

@Composable
private fun PollutantItem(
    label: String,
    value: Double?,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.let { String.format("%.1f", it) } ?: "--",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun aqiColor(index: Int): Color = when (index) {
    1 -> Color(0xFF22C55E)
    2 -> Color(0xFF84CC16)
    3 -> Color(0xFFFBBF24)
    4 -> Color(0xFFF97316)
    5 -> Color(0xFFEF4444)
    6 -> Color(0xFFBE185D)
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun AirQualityPill(
    airQuality: AirQuality?,
    modifier: Modifier = Modifier
) {
    if (airQuality == null) return

    val aqi = airQuality.europeanAqi ?: airQuality.usAqi ?: 0
    val label = airQuality.aqiLabel
    val color = aqiColor(airQuality.aqiColorIndex)

    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Air,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label ($aqi)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
