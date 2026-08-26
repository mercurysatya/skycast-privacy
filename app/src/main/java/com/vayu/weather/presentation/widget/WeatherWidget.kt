package com.vayu.weather.presentation.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.vayu.weather.domain.model.WeatherDescription
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "WeatherWidget"

private val white = ColorProvider(Color.White, Color.White)
private val whiteAlpha85 = ColorProvider(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.85f))
private val whiteAlpha70 = ColorProvider(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.7f))
private val whiteAlpha60 = ColorProvider(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f))
private val whiteAlpha55 = ColorProvider(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.55f))
private val rainBlue = ColorProvider(Color(0xFF90CAF9), Color(0xFF90CAF9))

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
        val cityName = prefs.getString("city_name", "") ?: ""
        val feelsLikeC = prefs.getFloat("apparent_temperature", tempCelsius)
        val uvIndex = prefs.getFloat("uv_index", 0f)

        Log.d(TAG, "provideGlance: hasData=$hasData, temp=$tempCelsius, code=$weatherCode")

        val displayTemp = if (isFahrenheit) (tempCelsius * 9 / 5 + 32).roundToInt() else tempCelsius.roundToInt()
        val feelsLike = if (isFahrenheit) (feelsLikeC * 9 / 5 + 32).roundToInt() else feelsLikeC.roundToInt()
        val tempLabel = if (isFahrenheit) "°F" else "°C"
        val displayWind = when (windUnit) {
            "MPH" -> "${(windKph * 0.621371).roundToInt()}"
            "MS" -> "${(windKph / 3.6).roundToInt()}"
            "KNOTS" -> "${(windKph * 0.539957).roundToInt()}"
            else -> "${windKph.roundToInt()}"
        }
        val windLabel = when (windUnit) {
            "MPH" -> "mph"; "MS" -> "m/s"; "KNOTS" -> "kn"; else -> "km/h"
        }

        val forecastDays = prefs.getInt("forecast_days", 0)
        val forecast = mutableListOf<ForecastDay>()
        for (i in 0 until forecastDays) {
            val date = prefs.getString("day_${i}_date", null) ?: continue
            val minTemp = prefs.getFloat("day_${i}_min_temp", 0f)
            val maxTemp = prefs.getFloat("day_${i}_max_temp", 0f)
            val code = prefs.getInt("day_${i}_weather_code", 0)
            val precip = prefs.getInt("day_${i}_precipitation", 0)
            val displayMin = if (isFahrenheit) (minTemp * 9 / 5 + 32).roundToInt() else minTemp.roundToInt()
            val displayMax = if (isFahrenheit) (maxTemp * 9 / 5 + 32).roundToInt() else maxTemp.roundToInt()
            val dayLabel = try {
                val localDate = LocalDate.parse(date)
                val today = LocalDate.now()
                when (localDate) {
                    today -> "Today"; today.plusDays(1) -> "Tmrw"
                    else -> localDate.format(DateTimeFormatter.ofPattern("EEE"))
                }
            } catch (e: Exception) { date.takeLast(2) }
            forecast.add(ForecastDay(dayLabel, displayMin, displayMax, code, precip))
        }

        val widgetSize = prefs.getString("widget_size", "MEDIUM") ?: "MEDIUM"
        val bgColor = getWeatherBgColor(weatherCode, isDay)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(bgColor)
                        .padding(16.dp)
                        .clickable(actionStartActivity(
                            android.content.ComponentName(context, com.vayu.weather.MainActivity::class.java)
                        )),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) {
                    if (hasData) {
                        when (widgetSize) {
                            "SMALL" -> SmallWidgetContent(displayTemp, tempLabel, weatherCode, cityName)
                            "LARGE" -> LargeWidgetContent(displayTemp, tempLabel, weatherCode, humidity, displayWind, windLabel, forecast, cityName, feelsLike, uvIndex, isDay)
                            else -> MediumWidgetContent(displayTemp, tempLabel, weatherCode, humidity, displayWind, windLabel, forecast, cityName, feelsLike, isDay)
                        }
                    } else {
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        Text(text = "SkyCast", style = TextStyle(color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(text = "Tap to load weather", style = TextStyle(color = whiteAlpha60, fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallWidgetContent(displayTemp: Int, tempLabel: String, weatherCode: Int, cityName: String) {
    if (cityName.isNotEmpty()) {
        Text(text = cityName, style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(modifier = GlanceModifier.height(4.dp))
    }
    Text(text = "$displayTemp$tempLabel", style = TextStyle(color = white, fontSize = 40.sp, fontWeight = FontWeight.Bold))
    Spacer(modifier = GlanceModifier.height(2.dp))
    Text(text = "${getWeatherEmoji(weatherCode)} ${getWeatherDescription(weatherCode)}", style = TextStyle(color = whiteAlpha85, fontSize = 11.sp), maxLines = 1)
}

@Composable
private fun MediumWidgetContent(displayTemp: Int, tempLabel: String, weatherCode: Int, humidity: Float, displayWind: String, windLabel: String, forecast: List<ForecastDay>, cityName: String, feelsLike: Int, isDay: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (cityName.isNotEmpty()) {
                Text(text = cityName, style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
        Text(text = if (isDay) "☀ Day" else "🌙 Night", style = TextStyle(color = whiteAlpha60, fontSize = 10.sp))
    }
    Spacer(modifier = GlanceModifier.height(4.dp))
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text = "$displayTemp$tempLabel", style = TextStyle(color = white, fontSize = 48.sp, fontWeight = FontWeight.Bold))
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.padding(bottom = 6.dp)) {
            Text(text = "${getWeatherEmoji(weatherCode)} ${getWeatherDescription(weatherCode)}", style = TextStyle(color = whiteAlpha85, fontSize = 12.sp), maxLines = 1)
            Text(text = "Feels like $feelsLike$tempLabel", style = TextStyle(color = whiteAlpha60, fontSize = 11.sp))
        }
    }
    Spacer(modifier = GlanceModifier.height(12.dp))
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        WStatItem(label = "Humidity", value = "${humidity.toInt()}%")
        Spacer(modifier = GlanceModifier.width(24.dp))
        WStatItem(label = "Wind", value = "$displayWind $windLabel")
    }
    if (forecast.isNotEmpty()) {
        Spacer(modifier = GlanceModifier.height(14.dp))
        forecast.take(3).forEach { day ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = day.dayLabel, style = TextStyle(color = white, fontSize = 12.sp), modifier = GlanceModifier.defaultWeight())
                Text(text = "${getWeatherEmoji(day.weatherCode)} ${getWeatherDescription(day.weatherCode)}", style = TextStyle(color = whiteAlpha70, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
                Text(text = "${day.minTemp}° / ${day.maxTemp}°", style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun LargeWidgetContent(displayTemp: Int, tempLabel: String, weatherCode: Int, humidity: Float, displayWind: String, windLabel: String, forecast: List<ForecastDay>, cityName: String, feelsLike: Int, uvIndex: Float, isDay: Boolean) {
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (cityName.isNotEmpty()) {
                Text(text = cityName, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
        Text(text = if (isDay) "☀ Day" else "🌙 Night", style = TextStyle(color = whiteAlpha60, fontSize = 11.sp))
    }
    Spacer(modifier = GlanceModifier.height(2.dp))
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text = "$displayTemp$tempLabel", style = TextStyle(color = white, fontSize = 56.sp, fontWeight = FontWeight.Bold))
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.padding(bottom = 8.dp)) {
            Text(text = "${getWeatherEmoji(weatherCode)} ${getWeatherDescription(weatherCode)}", style = TextStyle(color = whiteAlpha85, fontSize = 14.sp), maxLines = 1)
            Text(text = "Feels like $feelsLike$tempLabel", style = TextStyle(color = whiteAlpha60, fontSize = 12.sp))
        }
    }
    Spacer(modifier = GlanceModifier.height(14.dp))
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        WStatItem(label = "Humidity", value = "${humidity.toInt()}%")
        Spacer(modifier = GlanceModifier.width(16.dp))
        WStatItem(label = "Wind", value = "$displayWind $windLabel")
        Spacer(modifier = GlanceModifier.width(16.dp))
        WStatItem(label = "UV", value = if (uvIndex > 0) "${uvIndex.roundToInt()}" else "--")
    }
    Spacer(modifier = GlanceModifier.height(16.dp))
    if (forecast.isNotEmpty()) {
        Text(text = "3-Day Forecast", style = TextStyle(color = whiteAlpha70, fontSize = 11.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.fillMaxWidth())
        Spacer(modifier = GlanceModifier.height(6.dp))
        forecast.take(3).forEach { day ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = day.dayLabel, style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
                Text(text = "${getWeatherEmoji(day.weatherCode)} ${getWeatherDescription(day.weatherCode)}", style = TextStyle(color = whiteAlpha70, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
                Text(text = "${day.minTemp}° / ${day.maxTemp}°", style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                if (day.precipitation > 0) {
                    Text(text = "💧${day.precipitation}%", style = TextStyle(color = rainBlue, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight())
                } else {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun WStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Text(text = label, style = TextStyle(color = whiteAlpha55, fontSize = 10.sp))
    }
}

private fun getWeatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"; 1 -> "🌤️"; 2 -> "⛅"; 3 -> "☁️"
    45, 48 -> "🌫️"; 51, 53, 55 -> "🌦️"; 56, 57 -> "🌧️"
    61, 63, 65 -> "🌧️"; 66, 67 -> "🌧️"
    71, 73, 75 -> "❄️"; 77 -> "🌨️"
    80, 81, 82 -> "🌧️"; 85, 86 -> "🌨️"
    95 -> "⛈️"; 96, 99 -> "⛈️"
    else -> "🌤️"
}

private fun getWeatherBgColor(code: Int, isDay: Boolean): Color = when {
    code in listOf(95, 96, 99) -> Color(0xFF1a1a2e)
    code in listOf(61, 63, 65, 66, 67, 80, 81, 82) -> Color(0xFF1e3a5f)
    code in listOf(71, 73, 75, 77, 85, 86) -> Color(0xFF4a6580)
    code == 0 && isDay -> Color(0xFF1565C0)
    code == 0 && !isDay -> Color(0xFF1a237e)
    code in listOf(45, 48) -> Color(0xFF546E7A)
    isDay -> Color(0xFF1976D2)
    else -> Color(0xFF1a237e)
}

private data class ForecastDay(
    val dayLabel: String, val minTemp: Int, val maxTemp: Int,
    val weatherCode: Int, val precipitation: Int
)

private fun getWeatherDescription(code: Int): String = WeatherDescription.getWeatherDescription(code, isDay = true)

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "WeatherWidgetImmediateUpdate",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.vayu.weather.data.worker.WeatherWidgetWorker>().build()
        )
    }
}
