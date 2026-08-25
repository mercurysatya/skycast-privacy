package com.vayu.weather.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vayu.weather.R
import com.vayu.weather.domain.model.HourlyWeather

@Composable
fun TemperatureChart(
    hourlyData: List<HourlyWeather> = remember { mutableListOf() }
) {
    if (hourlyData.isEmpty()) return

    Text(
        text = "${hourlyData[0].temperature.toInt()}°",
        modifier = Modifier.padding(8.dp),
        color = MaterialTheme.colorScheme.onSurface
    )

    if (hourlyData.size > 1) {
        val minTemp = hourlyData.minOf { it.temperature }
        val maxTemp = hourlyData.maxOf { it.temperature }
        Text(
            text = "Range: ${minTemp.toInt()}° - ${maxTemp.toInt()}°",
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TemperatureChartView(
    hourlyData: List<HourlyWeather> = remember { mutableListOf() }
) {
    TemperatureChart(hourlyData = hourlyData)
}