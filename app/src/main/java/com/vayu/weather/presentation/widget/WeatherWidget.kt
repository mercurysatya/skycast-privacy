package com.vayu.weather.presentation.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as UnitColorProvider
import com.vayu.weather.data.worker.WeatherWidgetWorker
import com.vayu.weather.domain.model.WeatherInfo
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TAG = "WeatherWidget"

// ── Premium palette (dark-mode-first; light-mode handled by tint) ──
private val skyTextPrimary = ColorProvider(Color(0xFFFFFFFF), Color(0xFF0F172A))
private val skyTextSecondary = ColorProvider(Color(0xB3FFFFFF), Color(0xCC1E293B))
private val skyTextTertiary = ColorProvider(Color(0x80FFFFFF), Color(0x991E293B))
private val skyAccent = ColorProvider(Color(0xFF60A5FA), Color(0xFF1D4ED8))
private val skyAccentWarm = ColorProvider(Color(0xFFFBBF24), Color(0xFFD97706))
private val skyAccentRain = ColorProvider(Color(0xFF7DD3FC), Color(0xFF0284C7))
private val skyGlassBg = ColorProvider(Color(0xCC0F172A), Color(0xF2FFFFFF))
private val skyAlert = ColorProvider(Color(0xFFEF4444), Color(0xFFDC2626))

/**
 * Glance app widget.
 *
 * Reads a single [WidgetSnapshot] from SharedPreferences. The snapshot is
 * written by [WeatherWidgetWorker] after a successful API fetch (or kept
 * intact on failure, so the widget always has something to show).
 *
 * Three sizes are supported via [SizeMode] and the system "resize" gesture:
 *   - COMPACT  ≈ 2×2 launcher tile (location + temp + condition + H/L)
 *   - STANDARD ≈ 3×2 (compact + feels like + rain chance + 3-hour strip)
 *   - DETAILED ≈ 4×3 or wider (full hourly strip + humidity + wind + UV)
 */
class WeatherWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(WeatherWidgetWorker.KEY_SNAPSHOT, null)
        val snapshot = WidgetSnapshot.decode(raw)
        val widgetSize = prefs.getString("widget_size", "MEDIUM") ?: "MEDIUM"
        val openApp = actionStartActivity(
            android.content.ComponentName(context, com.vayu.weather.MainActivity::class.java)
        )

        Log.d(TAG, "provideGlance: hasData=${snapshot.hasData}, size=$widgetSize, stale=${snapshot.isStale}")

        provideContent {
            GlanceTheme {
                // The widget body fills the available launcher slot — the
                // system handles resizing; we just choose the right layout
                // by reading the user-selected size preference.
                when (widgetSize) {
                    "SMALL" -> WidgetLayouts.Compact(snapshot, openApp)
                    "LARGE" -> WidgetLayouts.Detailed(snapshot, openApp)
                    else -> WidgetLayouts.Standard(snapshot, openApp)
                }
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "WeatherWidgetImmediateUpdate",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<WeatherWidgetWorker>().build()
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
//  Three layouts
// ════════════════════════════════════════════════════════════════════════

object WidgetLayouts {

    @Composable
    fun Compact(snapshot: WidgetSnapshot, openApp: androidx.glance.action.Action) {
        val info = snapshot.info
        val bg = if (info != null) bgFor(info.current.weatherCode, info.current.isDay) else skyGlassBg

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(bg)
                .padding(14.dp)
                .clickable(openApp)
        ) {
            if (info == null) {
                EmptyState()
            } else {
                val temp = if (snapshot.isCelsius) info.current.temperature.roundToInt()
                else (info.current.temperature * 9.0 / 5.0 + 32).roundToInt()
                val today = info.daily.firstOrNull()
                val high = today?.maxTemp?.let { if (snapshot.isCelsius) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val low = today?.minTemp?.let { if (snapshot.isCelsius) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }

                Text(
                    text = snapshot.cityName.ifBlank { "Current location" },
                    style = TextStyle(color = skyTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$temp°",
                        style = TextStyle(color = skyTextPrimary, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = weatherEmoji(info.current.weatherCode),
                        style = TextStyle(color = skyTextPrimary, fontSize = 28.sp)
                    )
                }
                Text(
                    text = weatherLabel(info.current.weatherCode, info.current.isDay),
                    style = TextStyle(color = skyTextSecondary, fontSize = 11.sp),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(6.dp))
                Row {
                    if (high != null && low != null) {
                        Text(
                            text = "H $high°  L $low°",
                            style = TextStyle(color = skyTextSecondary, fontSize = 11.sp)
                        )
                    } else {
                        Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }

    @Composable
    fun Standard(snapshot: WidgetSnapshot, openApp: androidx.glance.action.Action) {
        val info = snapshot.info
        val bg = if (info != null) bgFor(info.current.weatherCode, info.current.isDay) else skyGlassBg

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(bg)
                .padding(16.dp)
                .clickable(openApp)
        ) {
            if (info == null) {
                EmptyState()
            } else {
                val isC = snapshot.isCelsius
                val temp = if (isC) info.current.temperature.roundToInt()
                else (info.current.temperature * 9.0 / 5.0 + 32).roundToInt()
                val feels = info.current.apparentTemperature?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val today = info.daily.firstOrNull()
                val high = today?.maxTemp?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val low = today?.minTemp?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val rainProb = info.hourly.minByOrNull {
                    kotlin.math.abs(
                        java.time.LocalDateTime.parse(it.time).toEpochSecond(java.time.ZoneOffset.UTC) -
                        java.time.Instant.now().epochSecond
                    )
                }?.precipitationProbability ?: 0
                val label = weatherLabel(info.current.weatherCode, info.current.isDay)

                Text(
                    text = snapshot.cityName.ifBlank { "Current location" },
                    style = TextStyle(color = skyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$temp°",
                        style = TextStyle(color = skyTextPrimary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    Column {
                        Text(
                            text = weatherEmoji(info.current.weatherCode),
                            style = TextStyle(color = skyTextPrimary, fontSize = 26.sp)
                        )
                        Text(
                            text = label,
                            style = TextStyle(color = skyTextSecondary, fontSize = 12.sp),
                            maxLines = 1
                        )
                    }
                }
                if (feels != null) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = "Feels like $feels°",
                        style = TextStyle(color = skyTextSecondary, fontSize = 12.sp)
                    )
                }
                Spacer(GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    if (high != null && low != null) {
                        Text(
                            text = "H $high°  L $low°",
                            style = TextStyle(color = skyTextSecondary, fontSize = 11.sp)
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    if (rainProb >= 30) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💧", style = TextStyle(fontSize = 12.sp))
                            Spacer(GlanceModifier.width(2.dp))
                            Text(
                                text = "$rainProb%",
                                style = TextStyle(color = skyAccentRain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                // Short hourly strip
                val strip = info.hourly.sortedBy { it.time }.take(4)
                if (strip.isNotEmpty()) {
                    Spacer(GlanceModifier.height(10.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        strip.forEachIndexed { idx, h ->
                            val label2 = hourLabel(h.time, isFirst = idx == 0)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = label2, style = TextStyle(color = skyTextTertiary, fontSize = 10.sp))
                                Spacer(GlanceModifier.height(2.dp))
                                Text(text = weatherEmoji(h.weatherCode), style = TextStyle(fontSize = 14.sp))
                                Spacer(GlanceModifier.height(2.dp))
                                val t = if (isC) h.temperature.roundToInt() else (h.temperature * 9.0 / 5.0 + 32).roundToInt()
                                Text(text = "$t°", style = TextStyle(color = skyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                            if (idx < strip.lastIndex) Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                FreshnessFooter(snapshot = snapshot)
            }
        }
    }

    @Composable
    fun Detailed(snapshot: WidgetSnapshot, openApp: androidx.glance.action.Action) {
        val info = snapshot.info
        val bg = if (info != null) bgFor(info.current.weatherCode, info.current.isDay) else skyGlassBg

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(bg)
                .padding(16.dp)
                .clickable(openApp)
        ) {
            if (info == null) {
                EmptyState()
            } else {
                val isC = snapshot.isCelsius
                val temp = if (isC) info.current.temperature.roundToInt()
                else (info.current.temperature * 9.0 / 5.0 + 32).roundToInt()
                val feels = info.current.apparentTemperature?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val today = info.daily.firstOrNull()
                val high = today?.maxTemp?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val low = today?.minTemp?.let { if (isC) it.roundToInt() else (it * 9.0 / 5.0 + 32).roundToInt() }
                val humidity = info.current.humidity?.roundToInt() ?: 0
                val wind = info.current.windSpeed?.roundToInt() ?: 0
                val uv = today?.uvIndex?.roundToInt() ?: 0

                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = snapshot.cityName.ifBlank { "Current location" },
                        style = TextStyle(color = skyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = if (info.current.isDay) "☀" else "🌙",
                        style = TextStyle(color = skyTextSecondary, fontSize = 14.sp)
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$temp°",
                        style = TextStyle(color = skyTextPrimary, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    Column {
                        Text(
                            text = "${weatherEmoji(info.current.weatherCode)} ${weatherLabel(info.current.weatherCode, info.current.isDay)}",
                            style = TextStyle(color = skyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                        if (feels != null) {
                            Text(
                                text = "Feels $feels°",
                                style = TextStyle(color = skyTextSecondary, fontSize = 12.sp)
                            )
                        }
                    }
                }
                if (high != null && low != null) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = "H $high° / L $low°",
                        style = TextStyle(color = skyTextSecondary, fontSize = 12.sp)
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                // Hourly + rain prob strip
                val strip = info.hourly.sortedBy { it.time }.take(5)
                if (strip.isNotEmpty()) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        strip.forEachIndexed { idx, h ->
                            val label2 = hourLabel(h.time, isFirst = idx == 0)
                            val t = if (isC) h.temperature.roundToInt() else (h.temperature * 9.0 / 5.0 + 32).roundToInt()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = label2, style = TextStyle(color = skyTextTertiary, fontSize = 9.sp))
                                Spacer(GlanceModifier.height(2.dp))
                                Text(text = weatherEmoji(h.weatherCode), style = TextStyle(fontSize = 14.sp))
                                Text(text = "$t°", style = TextStyle(color = skyTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                val p = h.precipitationProbability ?: 0
                                if (p >= 30) {
                                    Text(text = "$p%", style = TextStyle(color = skyAccentRain, fontSize = 9.sp))
                                } else {
                                    Spacer(GlanceModifier.height(9.dp))
                                }
                            }
                            if (idx < strip.lastIndex) Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
                Spacer(GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Stat("RH", "$humidity%")
                    Spacer(GlanceModifier.defaultWeight())
                    Stat("Wind", "$wind ${snapshot.windUnitLabel}")
                    Spacer(GlanceModifier.defaultWeight())
                    Stat("UV", if (uv > 0) "$uv" else "—")
                }
                Spacer(GlanceModifier.defaultWeight())
                FreshnessFooter(snapshot = snapshot)
            }
        }
    }

    @Composable
    private fun Stat(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = TextStyle(color = skyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(text = label, style = TextStyle(color = skyTextTertiary, fontSize = 9.sp))
        }
    }

    @Composable
    private fun EmptyState() {
        Text(
            text = "SkyCast",
            style = TextStyle(color = skyTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Open the app to load weather",
            style = TextStyle(color = skyTextSecondary, fontSize = 12.sp)
        )
    }

    @Composable
    private fun FreshnessFooter(snapshot: WidgetSnapshot) {
        if (!snapshot.hasData) return
        val label = snapshot.freshnessLabel()
        Text(
            text = label,
            style = TextStyle(color = skyTextTertiary, fontSize = 9.sp)
        )
    }
}

// ── Helpers (reused by all layouts) ─────────────────────────────────────

private fun bgFor(code: Int, isDay: Boolean): UnitColorProvider = when {
    code in 95..99 -> ColorProvider(Color(0xFF1A1A2E), Color(0xFFE0E7FF))
    code in 71..77 -> ColorProvider(Color(0xFF3F5573), Color(0xFFE0E7FF))
    code in 65..67 -> ColorProvider(Color(0xFF1E3A5F), Color(0xFFDBEAFE))
    code in 51..64 -> ColorProvider(Color(0xFF243B55), Color(0xFFE0F2FE))
    code in 80..82 -> ColorProvider(Color(0xFF1E3A5F), Color(0xFFDBEAFE))
    code in 85..86 -> ColorProvider(Color(0xFF3F5573), Color(0xFFE0E7FF))
    code in 45..48 -> ColorProvider(Color(0xFF3D4A5A), Color(0xFFF1F5F9))
    code == 0 && !isDay -> ColorProvider(Color(0xFF1A237E), Color(0xFF312E81))
    code == 0 && isDay -> ColorProvider(Color(0xFF1565C0), Color(0xFFDBEAFE))
    code in 1..2 -> ColorProvider(Color(0xFF1B3A6B), Color(0xFFDBEAFE))
    else -> ColorProvider(Color(0xFF1B3A6B), Color(0xFFDBEAFE))
}

private fun weatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"; 1 -> "🌤️"; 2 -> "⛅"; 3 -> "☁️"
    45, 48 -> "🌫️"; 51, 53, 55 -> "🌦️"; 56, 57 -> "🌧️"
    61, 63, 65 -> "🌧️"; 66, 67 -> "🌧️"
    71, 73, 75 -> "❄️"; 77 -> "🌨️"
    80, 81, 82 -> "🌧️"; 85, 86 -> "🌨️"
    95, 96, 99 -> "⛈️"
    else -> "🌤️"
}

private fun weatherLabel(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "Clear" else "Clear"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51 -> "Drizzle"
    53, 55 -> "Drizzle"
    61 -> "Light rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    71 -> "Light snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    80 -> "Showers"
    81, 82 -> "Heavy showers"
    95, 96, 99 -> "Thunder"
    else -> "Cloudy"
}

private fun hourLabel(time: String, isFirst: Boolean): String {
    if (isFirst) return "Now"
    val hour = try {
        java.time.LocalDateTime.parse(time).hour
    } catch (e: Exception) { return time.takeLast(5) }
    return when (hour) {
        0 -> "12a"
        in 1..11 -> "${hour}a"
        12 -> "12p"
        else -> "${hour - 12}p"
    }
}
