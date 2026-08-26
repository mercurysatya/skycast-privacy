package com.vayu.weather.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import com.vayu.weather.domain.model.WeatherInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Generates a premium weather share card as a Bitmap.
 * The card shows: city, temperature, condition, high/low, hourly mini-forecast, and branding.
 */
fun generateWeatherShareCard(
    context: Context,
    cityName: String,
    weatherInfo: WeatherInfo,
    isCelsius: Boolean
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val current = weatherInfo.current
    val today = weatherInfo.daily.firstOrNull()
    val hourly = weatherInfo.hourly.sortedBy { it.time }.take(8)

    val temp = if (isCelsius) current.temperature.roundToInt()
        else (current.temperature * 9.0 / 5.0 + 32.0).roundToInt()
    val unit = if (isCelsius) "°C" else "°F"
    val high = today?.let {
        if (isCelsius) it.maxTemp.roundToInt() else (it.maxTemp * 9.0 / 5.0 + 32.0).roundToInt()
    }
    val low = today?.let {
        if (isCelsius) it.minTemp.roundToInt() else (it.minTemp * 9.0 / 5.0 + 32.0).roundToInt()
    }

    // Background gradient based on weather
    val bgColors = computeShareColors(current.weatherCode, current.isDay)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            bgColors, null, Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Subtle decorative circles
    val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(20, 255, 255, 255)
    }
    canvas.drawCircle(width * 0.8f, height * 0.15f, 200f, decorPaint)
    canvas.drawCircle(width * 0.2f, height * 0.85f, 300f, decorPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(180, 255, 255, 255)
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    // ── City name ──
    textPaint.textSize = 56f
    canvas.drawText(cityName, width / 2f, 240f, textPaint)

    // ── Timestamp ──
    subTextPaint.textSize = 32f
    val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d • h:mm a"))
    canvas.drawText(timeStr, width / 2f, 300f, subTextPaint)

    // ── Weather icon text (emoji-style) ──
    textPaint.textSize = 140f
    val iconText = getWeatherEmoji(current.weatherCode, current.isDay)
    canvas.drawText(iconText, width / 2f, 520f, textPaint)

    // ── Large temperature ──
    textPaint.textSize = 200f
    canvas.drawText("${temp}${unit}", width / 2f, 740f, textPaint)

    // ── Condition ──
    textPaint.textSize = 48f
    canvas.drawText(getConditionText(current.weatherCode), width / 2f, 820f, textPaint)

    // ── High / Low ──
    if (high != null && low != null) {
        textPaint.textSize = 42f
        canvas.drawText("H: ${high}${unit}  L: ${low}${unit}", width / 2f, 900f, subTextPaint)
    }

    // ── Feels like ──
    current.apparentTemperature?.let { apparent ->
        val feelsLike = if (isCelsius) apparent.roundToInt()
            else (apparent * 9.0 / 5.0 + 32.0).roundToInt()
        subTextPaint.textSize = 36f
        canvas.drawText("Feels like ${feelsLike}${unit}", width / 2f, 960f, subTextPaint)
    }

    // ── Divider line ──
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(40, 255, 255, 255)
        strokeWidth = 2f
    }
    canvas.drawLine(100f, 1020f, width - 100f, 1020f, dividerPaint)

    // ── Hourly mini forecast ──
    subTextPaint.textSize = 28f
    canvas.drawText("Next Hours", width / 2f, 1080f, subTextPaint)

    val startX = 80f
    val itemWidth = (width - 160f) / hourly.size.coerceAtLeast(1)
    hourly.forEachIndexed { index, hour ->
        val x = startX + itemWidth * index + itemWidth / 2

        // Time
        subTextPaint.textSize = 24f
        val hourLabel = try {
            java.time.LocalTime.parse(hour.time.substringAfter("T"))
                .format(DateTimeFormatter.ofPattern("ha"))
        } catch (_: Exception) { "--" }
        canvas.drawText(hourLabel, x, 1140f, subTextPaint)

        // Temp
        textPaint.textSize = 32f
        val hTemp = if (isCelsius) hour.temperature.roundToInt()
            else (hour.temperature * 9.0 / 5.0 + 32.0).roundToInt()
        canvas.drawText("${hTemp}°", x, 1200f, textPaint)
    }

    // ── Weather metrics row ──
    val metrics = mutableListOf<String>()
    current.humidity?.let { metrics.add("💧 ${it.roundToInt()}%") }
    current.windSpeed?.let { metrics.add("💨 ${it.roundToInt()} km/h") }
    today?.uvIndex?.let { metrics.add("☀️ UV ${it.roundToInt()}") }
    current.visibility?.let { metrics.add("👁 ${(it / 1000).roundToInt()} km") }

    if (metrics.isNotEmpty()) {
        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            textSize = 32f
        }
        val metricStartX = width / 2f - (metrics.size - 1) * 120f / 2f
        metrics.take(4).forEachIndexed { i, m ->
            canvas.drawText(m, metricStartX + i * 240f, 1320f, metricPaint)
        }
    }

    // ── Bottom branding ──
    subTextPaint.textSize = 28f
    canvas.drawText("SkyCast Weather", width / 2f, height - 120f, subTextPaint)
    subTextPaint.textSize = 22f
    canvas.drawText("Powered by Open-Meteo", width / 2f, height - 70f, subTextPaint)

    return bitmap
}

private fun computeShareColors(code: Int, isDay: Boolean): IntArray {
    if (!isDay) return intArrayOf(
        android.graphics.Color.parseColor("#0F172A"),
        android.graphics.Color.parseColor("#1E293B"),
        android.graphics.Color.parseColor("#0F172A")
    )
    return when (code) {
        0 -> intArrayOf(
            android.graphics.Color.parseColor("#0EA5E9"),
            android.graphics.Color.parseColor("#38BDF8"),
            android.graphics.Color.parseColor("#7DD3FC")
        )
        1, 2 -> intArrayOf(
            android.graphics.Color.parseColor("#475569"),
            android.graphics.Color.parseColor("#64748B"),
            android.graphics.Color.parseColor("#94A3B8")
        )
        3 -> intArrayOf(
            android.graphics.Color.parseColor("#475569"),
            android.graphics.Color.parseColor("#64748B"),
            android.graphics.Color.parseColor("#94A3B8")
        )
        45, 48 -> intArrayOf(
            android.graphics.Color.parseColor("#64748B"),
            android.graphics.Color.parseColor("#94A3B8"),
            android.graphics.Color.parseColor("#CBD5E1")
        )
        in 51..55, in 61..65, in 80..82 -> intArrayOf(
            android.graphics.Color.parseColor("#1E293B"),
            android.graphics.Color.parseColor("#334155"),
            android.graphics.Color.parseColor("#475569")
        )
        in 71..75 -> intArrayOf(
            android.graphics.Color.parseColor("#64748B"),
            android.graphics.Color.parseColor("#94A3B8"),
            android.graphics.Color.parseColor("#CBD5E1")
        )
        in 95..99 -> intArrayOf(
            android.graphics.Color.parseColor("#1E1B4B"),
            android.graphics.Color.parseColor("#312E81"),
            android.graphics.Color.parseColor("#4338CA")
        )
        else -> intArrayOf(
            android.graphics.Color.parseColor("#38BDF8"),
            android.graphics.Color.parseColor("#64748B"),
            android.graphics.Color.parseColor("#94A3B8")
        )
    }
}

private fun getWeatherEmoji(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1 -> "🌤"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫"
    51, 53, 55 -> "🌦"
    61, 63, 65 -> "🌧"
    71, 73, 75 -> "❄️"
    80, 81, 82 -> "🌧"
    95, 96, 99 -> "⛈"
    else -> "☁️"
}

private fun getConditionText(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mainly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51 -> "Light Drizzle"
    53 -> "Moderate Drizzle"
    55 -> "Dense Drizzle"
    61 -> "Slight Rain"
    63 -> "Moderate Rain"
    65 -> "Heavy Rain"
    71 -> "Slight Snow"
    73 -> "Moderate Snow"
    75 -> "Heavy Snow"
    80 -> "Rain Showers"
    81 -> "Moderate Showers"
    82 -> "Violent Showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with Hail"
    99 -> "Severe Thunderstorm"
    else -> "Cloudy"
}
