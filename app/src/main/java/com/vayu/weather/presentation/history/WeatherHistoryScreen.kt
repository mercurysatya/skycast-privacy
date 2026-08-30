package com.vayu.weather.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherHistoryDay
import com.vayu.weather.domain.model.WeatherDescription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ---- Color palette ----
private val CardBg = Color.White.copy(alpha = 0.1f)
private val AccentBlue = Color(0xFF38BDF8)
private val WarmOrange = Color(0xFFF97316)
private val CoolBlue = Color(0xFF38BDF8)
private val ChartRed = Color(0xFFEF4444)
private val ChartBlue = Color(0xFF38BDF8)
private val Green = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherHistoryScreen(
    viewModel: WeatherHistoryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weather_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.clear_all))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.clear_history_title)) },
                text = { Text(stringResource(R.string.clear_history_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }) {
                        Text(stringResource(R.string.clear_all))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Range selector chips
            RangeChipRow(
                selected = state.selectedRange,
                onSelect = viewModel::setRange
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.8f))
                }
            } else if (state.dailyData.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Stats cards
                    state.stats?.let { stats ->
                        item {
                            StatsRow(stats)
                        }
                    }

                    // Temperature trend chart
                    item {
                        TemperatureChart(state.dailyData)
                    }

                    // Humidity trend chart
                    if (state.dailyData.any { it.humidity != null }) {
                        item {
                            HumidityChart(state.dailyData)
                        }
                    }

                    // Daily breakdown list
                    item {
                        Text(
                            text = stringResource(R.string.daily_breakdown),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    items(state.dailyData.reversed(), contentType = { "historyDay" }) { day ->
                        DailyHistoryRow(day)
                    }

                    // Ad banner at bottom
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        com.vayu.weather.presentation.ads.AdBanner()
                    }
                }
            }
        }
    }
}

// ============================================================
// RANGE CHIP ROW
// ============================================================

@Composable
private fun RangeChipRow(
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue.copy(alpha = 0.25f),
                    selectedLabelColor = AccentBlue,
                    containerColor = CardBg,
                    labelColor = Color.White.copy(alpha = 0.7f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.White.copy(alpha = 0.1f),
                    selectedBorderColor = AccentBlue.copy(alpha = 0.4f),
                    enabled = true,
                    selected = range == selected
                )
            )
        }
    }
}

// ============================================================
// STATS ROW
// ============================================================

@Composable
private fun StatsRow(stats: HistoryStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {        StatCard(
            label = stringResource(R.string.avg_temp),
            value = "${stats.avgTemp.roundToInt()}°",
            color = AccentBlue,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.high),
            value = "${stats.maxTemp.roundToInt()}°",
            color = ChartRed,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.low),
            value = "${stats.minTemp.roundToInt()}°",
            color = ChartBlue,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.days_tracked),
            value = "${stats.daysTracked}",
            color = Green,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

// ============================================================
// TEMPERATURE CHART (Canvas-drawn)
// ============================================================

@Composable
private fun TemperatureChart(dailyData: List<WeatherHistoryDay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.temperature_trends),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val allTemps = dailyData.flatMap { listOf(it.minTemp, it.maxTemp) }
            val minVal = (allTemps.min() - 3).toFloat()
            val maxVal = (allTemps.max() + 3).toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val stepX = if (dailyData.size > 1) chartWidth / (dailyData.size - 1) else chartWidth

                // Draw grid lines
                val gridColor = Color.White.copy(alpha = 0.08f)
                for (i in 0..4) {
                    val y = chartHeight * i / 4f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 1f)
                }

                // Helper to map temp to Y
                fun tempToY(temp: Double): Float {
                    val range = maxVal - minVal
                    if (range == 0f) return chartHeight / 2f
                    return chartHeight - ((temp.toFloat() - minVal) / range) * chartHeight
                }

                // Draw max temp area fill
                if (dailyData.size >= 2) {
                    val maxPath = Path().apply {
                        moveTo(0f, tempToY(dailyData[0].maxTemp))
                        dailyData.drop(1).forEachIndexed { i, day ->
                            lineTo(stepX * (i + 1), tempToY(day.maxTemp))
                        }
                        lineTo(stepX * (dailyData.size - 1), chartHeight)
                        lineTo(0f, chartHeight)
                        close()
                    }
                    drawPath(
                        maxPath,
                        brush = Brush.verticalGradient(
                            listOf(ChartRed.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                }

                // Draw max temp line
                if (dailyData.size >= 2) {
                    val maxLinePath = Path().apply {
                        moveTo(0f, tempToY(dailyData[0].maxTemp))
                        dailyData.drop(1).forEachIndexed { i, day ->
                            lineTo(stepX * (i + 1), tempToY(day.maxTemp))
                        }
                    }
                    drawPath(maxLinePath, color = ChartRed, style = Stroke(width = 2.5f))
                }

                // Draw min temp line
                if (dailyData.size >= 2) {
                    val minLinePath = Path().apply {
                        moveTo(0f, tempToY(dailyData[0].minTemp))
                        dailyData.drop(1).forEachIndexed { i, day ->
                            lineTo(stepX * (i + 1), tempToY(day.minTemp))
                        }
                    }
                    drawPath(minLinePath, color = ChartBlue, style = Stroke(width = 2.5f))
                }

                // Draw data points
                dailyData.forEachIndexed { i, day ->
                    val x = stepX * i
                    // Max dot
                    drawCircle(ChartRed, radius = 4f, center = Offset(x, tempToY(day.maxTemp)))
                    // Min dot
                    drawCircle(ChartBlue, radius = 4f, center = Offset(x, tempToY(day.minTemp)))
                }

                // Y-axis labels
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(150, 255, 255, 255)
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                for (i in 0..4) {
                    val temp = minVal + (maxVal - minVal) * i / 4f
                    val y = chartHeight - (chartHeight * i / 4f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${temp.roundToInt()}°",
                        -8f,
                        y + 8f,
                        textPaint
                    )
                }

                // X-axis labels (show every Nth date)
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val labelStep = (dailyData.size / 5).coerceAtLeast(1)
                dailyData.forEachIndexed { i, day ->
                    if (i % labelStep == 0 || i == dailyData.size - 1) {
                        val label = try {
                            LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("M/d"))
                        } catch (_: Exception) { day.date.takeLast(5) }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            stepX * i,
                            chartHeight + 32f,
                            labelPaint
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(ChartRed)
                }
                Text(
                    text = stringResource(R.string.high),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                )
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(ChartBlue)
                }
                Text(
                    text = stringResource(R.string.low),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

// ============================================================
// HUMIDITY CHART
// ============================================================

@Composable
private fun HumidityChart(dailyData: List<WeatherHistoryDay>) {
    val humidityData = remember(dailyData) {
        dailyData.filter { it.humidity != null }
    }
    if (humidityData.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.humidity_trends),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val stepX = if (humidityData.size > 1) chartWidth / (humidityData.size - 1) else chartWidth

                fun humToY(hum: Double): Float {
                    return chartHeight - (hum.toFloat() / 100f) * chartHeight
                }

                // Fill
                if (humidityData.size >= 2) {
                    val path = Path().apply {
                        moveTo(0f, humToY(humidityData[0].humidity ?: 50.0))
                        humidityData.drop(1).forEachIndexed { i, day ->
                            lineTo(stepX * (i + 1), humToY(day.humidity ?: 50.0))
                        }
                        lineTo(stepX * (humidityData.size - 1), chartHeight)
                        lineTo(0f, chartHeight)
                        close()
                    }
                    drawPath(
                        path,
                        brush = Brush.verticalGradient(
                            listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    // Line
                    val linePath = Path().apply {
                        moveTo(0f, humToY(humidityData[0].humidity ?: 50.0))
                        humidityData.drop(1).forEachIndexed { i, day ->
                            lineTo(stepX * (i + 1), humToY(day.humidity ?: 50.0))
                        }
                    }
                    drawPath(linePath, color = AccentBlue, style = Stroke(width = 2.5f))
                }

                // Points
                humidityData.forEachIndexed { i, day ->
                    drawCircle(AccentBlue, radius = 3.5f, center = Offset(stepX * i, humToY(day.humidity ?: 50.0)))
                }
            }
        }
    }
}

// ============================================================
// DAILY BREAKDOWN ROW
// ============================================================

@Composable
private fun DailyHistoryRow(day: WeatherHistoryDay) {
    val displayDate = remember(day.date) {
        try {
            val ld = LocalDate.parse(day.date)
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            when (ld) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> ld.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            }
        } catch (_: Exception) { day.date }
    }

    val description = remember(day.weatherCode) {
        WeatherDescription.getWeatherDescription(day.weatherCode, true)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.width(100.dp),
            maxLines = 1
        )

        // Condition
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Humidity
        day.humidity?.let { hum ->
            Icon(
                Icons.Rounded.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = AccentBlue.copy(alpha = 0.7f)
            )
            Text(
                text = "${hum.roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp, end = 8.dp)
            )
        }

        // Temp range
        Text(
            text = "${day.minTemp.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            color = ChartBlue
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Mini temp bar
        val globalMin = -10.0
        val globalMax = 50.0
        Canvas(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
        ) {
            val range = (globalMax - globalMin).toFloat()
            val startFrac = ((day.minTemp - globalMin) / range).coerceIn(0.0, 1.0).toFloat()
            val widthFrac = ((day.maxTemp - day.minTemp) / range).coerceIn(0.05, 1.0).toFloat()

            drawRoundRect(
                color = Color.White.copy(alpha = 0.1f),
                cornerRadius = CornerRadius(4f),
                size = Size(size.width, size.height)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(CoolBlue, WarmOrange)),
                topLeft = Offset(size.width * startFrac, 0f),
                size = Size(size.width * widthFrac, size.height),
                cornerRadius = CornerRadius(4f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${day.maxTemp.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// ============================================================
// EMPTY STATE
// ============================================================

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_history_data),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_data_will_appear),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
