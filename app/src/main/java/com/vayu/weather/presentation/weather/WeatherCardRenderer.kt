package com.vayu.weather.presentation.weather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherDescription
import com.vayu.weather.domain.model.WeatherInfo
import kotlin.math.roundToInt

object WeatherCardRenderer {

    private const val CARD_WIDTH_DP = 600
    private const val CARD_HEIGHT_DP = 350

    fun generateWeatherCardBitmap(
        context: Context,
        cityName: String,
        weatherInfo: WeatherInfo,
        isCelsius: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val cardWidth = (CARD_WIDTH_DP * density).toInt()
        val cardHeight = (CARD_HEIGHT_DP * density).toInt()

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Compute weather-specific gradient colors
        val gradientColors = computeCardGradientColors(
            weatherInfo.current.weatherCode,
            weatherInfo.current.isDay
        )

        // Draw background gradient
        paint.shader = android.graphics.LinearGradient(
            0f, 0f,
            cardWidth.toFloat(), cardHeight.toFloat(),
            gradientColors,
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(
            0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
            32f * density, 32f * density, paint
        )

        // Draw decorative wave at bottom
        paint.shader = null
        paint.color = android.graphics.Color.argb(25, 255, 255, 255)
        val wavePath = android.graphics.Path().apply {
            moveTo(0f, cardHeight * 0.75f)
            cubicTo(
                cardWidth * 0.25f, cardHeight * 0.65f,
                cardWidth * 0.5f, cardHeight * 0.85f,
                cardWidth.toFloat(), cardHeight * 0.7f
            )
            lineTo(cardWidth.toFloat(), cardHeight.toFloat())
            lineTo(0f, cardHeight.toFloat())
            close()
        }
        canvas.drawPath(wavePath, paint)

        // --- Draw text content ---
        val marginX = 48f * density
        val startY = 60f * density

        // App name
        paint.color = android.graphics.Color.argb(160, 255, 255, 255)
        paint.textSize = 16f * density
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL
        )
        canvas.drawText(context.getString(R.string.app_name), marginX, startY, paint)

        // City name
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 28f * density
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
        )
        canvas.drawText(cityName, marginX, startY + 44f * density, paint)

        // Temperature
        val tempStr = if (isCelsius) {
            "${weatherInfo.current.temperature.roundToInt()}°C"
        } else {
            "${((weatherInfo.current.temperature * 9.0 / 5.0) + 32.0).roundToInt()}°F"
        }
        paint.textSize = 72f * density
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
        )
        canvas.drawText(tempStr, marginX, startY + 130f * density, paint)

        // Weather condition
        val description = WeatherDescription.getWeatherDescription(
            weatherInfo.current.weatherCode, weatherInfo.current.isDay
        )
        paint.textSize = 20f * density
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL
        )
        paint.color = android.graphics.Color.argb(220, 255, 255, 255)
        canvas.drawText(description, marginX, startY + 165f * density, paint)

        // Stats row (humidity, wind, feels like)
        val statsY = startY + 210f * density
        val humidity = weatherInfo.current.humidity?.let { "${it.roundToInt()}%" } ?: "--"
        val wind = weatherInfo.current.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "--"
        val feelsLike = weatherInfo.current.apparentTemperature?.let {
            if (isCelsius) "${it.roundToInt()}°C"
            else "${((it * 9.0 / 5.0) + 32.0).roundToInt()}°F"
        } ?: "--"

        paint.textSize = 16f * density
        paint.color = android.graphics.Color.argb(200, 255, 255, 255)
        canvas.drawText("💧 $humidity", marginX, statsY, paint)
        canvas.drawText("💨 $wind", marginX + 180f * density, statsY, paint)
        canvas.drawText("🌡️ $feelsLike", marginX + 360f * density, statsY, paint)

        // Bottom branding label
        paint.textSize = 12f * density
        paint.color = android.graphics.Color.argb(100, 255, 255, 255)
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC
        )
        canvas.drawText(context.getString(R.string.app_name), marginX, cardHeight - 24f * density, paint)

        return bitmap
    }

    /**
     * Returns an IntArray of ARGB colors for the card gradient based on weather code.
     */
    private fun computeCardGradientColors(weatherCode: Int, isDay: Boolean): IntArray {
        val (top, mid, bottom) = when {
            !isDay -> Triple(
                android.graphics.Color.parseColor("#0D1B3E"),
                android.graphics.Color.parseColor("#1A2850"),
                android.graphics.Color.parseColor("#122650")
            )
            weatherCode == 0 -> Triple(
                android.graphics.Color.parseColor("#1565C0"),
                android.graphics.Color.parseColor("#1976D2"),
                android.graphics.Color.parseColor("#2196F3")
            )
            weatherCode in listOf(1, 2, 3) -> Triple(
                android.graphics.Color.parseColor("#1976D2"),
                android.graphics.Color.parseColor("#1E88E5"),
                android.graphics.Color.parseColor("#42A5F5")
            )
            weatherCode in listOf(45, 48) -> Triple(
                android.graphics.Color.parseColor("#546E7A"),
                android.graphics.Color.parseColor("#607D8B"),
                android.graphics.Color.parseColor("#78909C")
            )
            weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> Triple(
                android.graphics.Color.parseColor("#37474F"),
                android.graphics.Color.parseColor("#455A64"),
                android.graphics.Color.parseColor("#546E7A")
            )
            weatherCode in listOf(71, 73, 75) -> Triple(
                android.graphics.Color.parseColor("#0D47A1"),
                android.graphics.Color.parseColor("#1565C0"),
                android.graphics.Color.parseColor("#1E88E5")
            )
            weatherCode in listOf(95, 96, 99) -> Triple(
                android.graphics.Color.parseColor("#1B2631"),
                android.graphics.Color.parseColor("#263238"),
                android.graphics.Color.parseColor("#37474F")
            )
            else -> Triple(
                android.graphics.Color.parseColor("#1565C0"),
                android.graphics.Color.parseColor("#1976D2"),
                android.graphics.Color.parseColor("#2196F3")
            )
        }
        return intArrayOf(top, mid, bottom)
    }
}
