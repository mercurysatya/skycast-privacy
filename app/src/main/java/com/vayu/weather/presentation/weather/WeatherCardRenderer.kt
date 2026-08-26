package com.vayu.weather.presentation.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object WeatherCardRenderer {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1350

    fun generateWeatherCardBitmap(
        context: Context,
        cityName: String,
        weatherInfo: WeatherInfo,
        isCelsius: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density

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

        // ── Background gradient based on weather ──
        val bgColors = computeCardGradientColors(current.weatherCode, current.isDay)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_HEIGHT.toFloat(),
                bgColors, null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // ── Decorative circles ──
        val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(15, 255, 255, 255)
        }
        canvas.drawCircle(CARD_WIDTH * 0.85f, CARD_HEIGHT * 0.12f, 180f, decorPaint)
        canvas.drawCircle(CARD_WIDTH * 0.15f, CARD_HEIGHT * 0.88f, 250f, decorPaint)

        // ── Wave decoration at bottom ──
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(20, 255, 255, 255)
        }
        val wavePath = Path().apply {
            moveTo(0f, CARD_HEIGHT * 0.82f)
            cubicTo(
                CARD_WIDTH * 0.25f, CARD_HEIGHT * 0.76f,
                CARD_WIDTH * 0.5f, CARD_HEIGHT * 0.88f,
                CARD_WIDTH.toFloat(), CARD_HEIGHT * 0.78f
            )
            lineTo(CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat())
            lineTo(0f, CARD_HEIGHT.toFloat())
            close()
        }
        canvas.drawPath(wavePath, wavePaint)

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

        // ── App branding ──
        subTextPaint.textSize = 28f
        canvas.drawText(context.getString(R.string.app_name), CARD_WIDTH / 2f, 60f, subTextPaint)

        // ── City name ──
        textPaint.textSize = 52f
        canvas.drawText(cityName, CARD_WIDTH / 2f, 130f, textPaint)

        // ── Timestamp ──
        subTextPaint.textSize = 28f
        val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d • h:mm a"))
        canvas.drawText(timeStr, CARD_WIDTH / 2f, 175f, subTextPaint)

        // ── Large temperature ──
        textPaint.textSize = 180f
        canvas.drawText("${temp}${unit}", CARD_WIDTH / 2f, 400f, textPaint)

        // ── Condition ──
        textPaint.textSize = 44f
        canvas.drawText(getConditionText(current.weatherCode), CARD_WIDTH / 2f, 470f, textPaint)

        // ── High / Low ──
        if (high != null && low != null) {
            textPaint.textSize = 38f
            canvas.drawText("H: ${high}${unit}  L: ${low}${unit}", CARD_WIDTH / 2f, 530f, subTextPaint)
        }

        // ── Feels like ──
        current.apparentTemperature?.let { apparent ->
            val feelsLike = if (isCelsius) apparent.roundToInt()
                else (apparent * 9.0 / 5.0 + 32.0).roundToInt()
            subTextPaint.textSize = 32f
            canvas.drawText("Feels like ${feelsLike}${unit}", CARD_WIDTH / 2f, 580f, subTextPaint)
        }

        // ── Divider ──
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 255, 255, 255)
            strokeWidth = 2f
        }
        canvas.drawLine(80f, 630f, CARD_WIDTH - 80f, 630f, dividerPaint)

        // ── Hourly mini forecast ──
        subTextPaint.textSize = 26f
        canvas.drawText("Next Hours", CARD_WIDTH / 2f, 680f, subTextPaint)

        val startX = 60f
        val itemWidth = (CARD_WIDTH - 120f) / hourly.size.coerceAtLeast(1)
        hourly.forEachIndexed { index, hour ->
            val x = startX + itemWidth * index + itemWidth / 2

            subTextPaint.textSize = 22f
            val hourLabel = try {
                java.time.LocalTime.parse(hour.time.substringAfter("T"))
                    .format(DateTimeFormatter.ofPattern("ha"))
            } catch (_: Exception) { "--" }
            canvas.drawText(hourLabel, x, 730f, subTextPaint)

            textPaint.textSize = 30f
            val hTemp = if (isCelsius) hour.temperature.roundToInt()
                else (hour.temperature * 9.0 / 5.0 + 32.0).roundToInt()
            canvas.drawText("${hTemp}°", x, 780f, textPaint)
        }

        // ── Weather metrics row ──
        val metrics = mutableListOf<String>()
        current.humidity?.let { metrics.add("Humidity ${it.roundToInt()}%") }
        current.windSpeed?.let { metrics.add("Wind ${it.roundToInt()} km/h") }
        today?.uvIndex?.let { metrics.add("UV ${it.roundToInt()}") }
        current.surfacePressure?.let { metrics.add("${it.roundToInt()} hPa") }

        if (metrics.isNotEmpty()) {
            val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(200, 255, 255, 255)
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                textSize = 26f
            }
            val metricStartX = CARD_WIDTH / 2f - (metrics.size - 1) * 130f / 2f
            metrics.take(4).forEachIndexed { i, m ->
                canvas.drawText(m, metricStartX + i * 260f, 860f, metricPaint)
            }
        }

        // ── Sunrise / Sunset ──
        today?.sunrise?.let { sr ->
            today.sunset?.let { ss ->
                val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(160, 255, 255, 255)
                    typeface = Typeface.DEFAULT
                    textAlign = Paint.Align.CENTER
                    textSize = 24f
                }
                canvas.drawText(
                    "🌅 ${sr.take(5)}  —  🌇 ${ss.take(5)}",
                    CARD_WIDTH / 2f, 920f, sunPaint
                )
            }
        }

        // ── Bottom branding ──
        subTextPaint.textSize = 24f
        canvas.drawText("SkyCast Weather", CARD_WIDTH / 2f, CARD_HEIGHT - 40f, subTextPaint)

        return bitmap
    }

    private fun computeCardGradientColors(code: Int, isDay: Boolean): IntArray {
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
}
