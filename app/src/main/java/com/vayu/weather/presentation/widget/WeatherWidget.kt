package com.vayu.weather.presentation.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherDescription
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "WeatherWidget"

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        val hasData = prefs.getBoolean("has_data", false)
        val tempCelsius = prefs.getFloat("temperature", 0f)
        val weatherCode = prefs.getInt("weather_code", 0)
        val isDay = prefs.getBoolean("is_day", true)
        val windKph = prefs.getFloat("wind_speed", 0f)
        val humidity = prefs.getFloat("humidity", 0f)
        val isFahrenheit = prefs.getBoolean("is_fahrenheit", false)
        val windUnit = prefs.getString("wind_unit", "KPH") ?: "KPH"

        Log.d(TAG, "provideGlance: hasData=$hasData, temp=$tempCelsius, code=$weatherCode")

        val res = context.resources
        val displayTemp = if (isFahrenheit) (tempCelsius * 9 / 5 + 32).roundToInt() else tempCelsius.roundToInt()
        val tempLabel = if (isFahrenheit) "°F" else "°C"
        val displayWind = when (windUnit) {
            "MPH" -> "${(windKph * 0.621371).roundToInt()}"
            "MS" -> "${(windKph / 3.6).roundToInt()}"
            "KNOTS" -> "${(windKph * 0.539957).roundToInt()}"
            else -> "${windKph.roundToInt()}"
        }
        val windLabel = when (windUnit) {
            "MPH" -> "mph"
            "MS" -> "m/s"
            "KNOTS" -> "kn"
            else -> "km/h"
        }

        // Load 3-day forecast
        val forecastDays = prefs.getInt("forecast_days", 0)
        Log.d(TAG, "forecast_days=$forecastDays")
        val forecast = mutableListOf<ForecastDay>()
        for (i in 0 until forecastDays) {
            val date = prefs.getString("day_${i}_date", null) ?: continue
            val minTemp = prefs.getFloat("day_${i}_min_temp", 0f)
            val maxTemp = prefs.getFloat("day_${i}_max_temp", 0f)
            val code = prefs.getInt("day_${i}_weather_code", 0)
            val precip = prefs.getInt("day_${i}_precipitation", 0)

            Log.d(TAG, "Day $i: date=$date, min=$minTemp, max=$maxTemp, code=$code, precip=$precip")

            val displayMin = if (isFahrenheit) (minTemp * 9 / 5 + 32).roundToInt() else minTemp.roundToInt()
            val displayMax = if (isFahrenheit) (maxTemp * 9 / 5 + 32).roundToInt() else maxTemp.roundToInt()

            val dayLabel = try {
                val localDate = LocalDate.parse(date)
                val today = LocalDate.now()
                when (localDate) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> localDate.format(DateTimeFormatter.ofPattern("EEE"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse date: $date", e)
                date.takeLast(2)
            }

            forecast.add(
                ForecastDay(
                    dayLabel = dayLabel,
                    minTemp = displayMin,
                    maxTemp = displayMax,
                    weatherCode = code,
                    precipitation = precip
                )
            )
        }
        Log.d(TAG, "Loaded ${forecast.size} forecast days")

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) {
                    if (hasData) {
                        // Header
                        Text(
                            text = res.getString(R.string.app_name),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Temperature
                        Text(
                            text = "$displayTemp$tempLabel",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Weather condition
                        Text(
                            text = getWeatherDescription(weatherCode),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = GlanceModifier.height(12.dp))

                        // Humidity & Wind row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${humidity.toInt()}%",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = res.getString(R.string.humidity),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.width(32.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayWind $windLabel",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = res.getString(R.string.wind),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // 3-Day Forecast
                        if (forecast.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.height(16.dp))

                            Text(
                                text = res.getString(R.string.widget_3day_forecast),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.fillMaxWidth()
                            )

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            forecast.forEach { day ->
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day.dayLabel,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 11.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Text(
                                        text = getWeatherDescription(day.weatherCode),
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight(),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${day.minTemp}°/${day.maxTemp}°",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    if (day.precipitation > 0) {
                                        Text(
                                            text = "${day.precipitation}%",
                                            style = TextStyle(
                                                color = GlanceTheme.colors.primary,
                                                fontSize = 11.sp
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )
                                    } else {
                                        Spacer(modifier = GlanceModifier.defaultWeight())
                                    }
                                }
                            }
                        }
                    } else {
                        // Loading state
                        Text(
                            text = res.getString(R.string.app_name),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = res.getString(R.string.widget_loading),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class ForecastDay(
    val dayLabel: String,
    val minTemp: Int,
    val maxTemp: Int,
    val weatherCode: Int,
    val precipitation: Int
)

private fun getWeatherDescription(code: Int): String {
    return WeatherDescription.getWeatherDescription(code, isDay = true)
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Trigger immediate widget refresh when added or updated
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "WeatherWidgetImmediateUpdate",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.vayu.weather.data.worker.WeatherWidgetWorker>()
                .build()
        )
    }
}
