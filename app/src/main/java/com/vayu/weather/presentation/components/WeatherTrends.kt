package com.vayu.weather.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vayu.weather.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.ui.theme.SkyBlue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WeatherTrends(
    hourlyData: List<HourlyWeather>,
    isCelsius: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (hourlyData.isEmpty()) {
        Log.w("WeatherTrends", "Hourly data is empty, skipping chart")
        return
    }

    val chartData = remember(hourlyData) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
        val startIndex = hourlyData.indexOfFirst { it.time >= now }
        if (startIndex >= 0) hourlyData.drop(startIndex).take(24) else hourlyData.take(24)
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val primaryColorCompose = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.temperature_trends),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (chartData.isNotEmpty()) {
                    val min = chartData.minOf { it.temperature }.roundToInt()
                    val max = chartData.maxOf { it.temperature }.roundToInt()
                    "$min° - $max°"
                } else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        AndroidView(
            factory = { context ->
                Log.d("WeatherTrends", "Creating LineChart")
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setTouchEnabled(true)
                    setPinchZoom(true)
                    setDrawGridBackground(false)
                    setBackgroundColor(surfaceColor)
                    xAxis.textColor = onSurfaceColor
                    xAxis.setDrawGridLines(false)
                    xAxis.setDrawAxisLine(false)
                    axisLeft.textColor = onSurfaceColor
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = onSurfaceColor and 0x20FFFFFF
                    axisLeft.setDrawAxisLine(false)
                    axisRight.isEnabled = false
                    setNoDataText("Loading chart...")
                    setViewPortOffsets(0f, 8f, 0f, 16f)
                }
            },
            update = { chart ->
                Log.d("WeatherTrends", "Updating LineChart with ${chartData.size} items")
                try {
                    val entries = chartData.mapIndexed { index, hourly ->
                        val temp = if (isCelsius) hourly.temperature.toFloat()
                        else ((hourly.temperature * 9 / 5) + 32).toFloat()
                        Entry(index.toFloat(), temp)
                    }

                    if (entries.isEmpty()) {
                        chart.clear()
                        return@AndroidView
                    }

                    val minTemp = entries.minOf { it.y }
                    val maxTemp = entries.maxOf { it.y }

                    val dataSet = LineDataSet(entries, "Temperature").apply {
                        color = primaryColor
                        setCircleColor(primaryColor)
                        lineWidth = 3f
                        circleRadius = 5f
                        setDrawCircleHole(false)
                        circleHoleColor = primaryColor
                        valueTextColor = onSurfaceColor
                        valueTextSize = 0f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawFilled(true)
                        fillDrawable = android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(primaryColor and 0x40FFFFFF, primaryColor and 0x05FFFFFF)
                        )
                        setDrawHighlightIndicators(false)
                    }

                    chart.data = LineData(dataSet)
                    chart.axisLeft.axisMinimum = minTemp - 2f
                    chart.axisLeft.axisMaximum = maxTemp + 2f
                    chart.axisLeft.setLabelCount(4, true)
                    chart.xAxis.setLabelCount(6, true)
                    chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val idx = value.toInt().coerceIn(0, chartData.lastIndex)
                            return chartData[idx].time.take(2) + "h"
                        }
                    }
                    chart.invalidate()
                } catch (e: Exception) {
                    Log.e("WeatherTrends", "Error updating chart", e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}
