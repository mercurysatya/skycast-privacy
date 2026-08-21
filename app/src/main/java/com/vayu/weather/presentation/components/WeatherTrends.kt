package com.vayu.weather.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

    val currentHour = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
    }
    val chartData = remember(hourlyData) {
        val startIndex = hourlyData.indexOfFirst { it.time >= currentHour }
        if (startIndex >= 0) hourlyData.drop(startIndex).take(24) else hourlyData.take(24)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.temperature_trends),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

        AndroidView(
            factory = { context ->
                Log.d("WeatherTrends", "Creating LineChart")
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setTouchEnabled(true)
                    setPinchZoom(true)
                    xAxis.textColor = onSurfaceColor
                    axisLeft.textColor = onSurfaceColor
                    axisRight.isEnabled = false
                    setNoDataText("Loading chart...")
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

                    val dataSet = LineDataSet(entries, "Temperature").apply {
                        color = primaryColor
                        setCircleColor(primaryColor)
                        lineWidth = 2f
                        circleRadius = 4f
                        setDrawCircleHole(false)
                        valueTextColor = onSurfaceColor
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                } catch (e: Exception) {
                    Log.e("WeatherTrends", "Error updating chart", e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}
