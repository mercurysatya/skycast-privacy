package com.vayu.weather.presentation.weather

import android.content.Context
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherDescription
import com.vayu.weather.domain.model.WeatherInfo
import kotlin.math.roundToInt

object WeatherShareFormatter {

    fun formatForShare(
        context: Context,
        cityName: String?,
        weatherInfo: WeatherInfo,
        isCelsius: Boolean
    ): String {
        val res = context.resources
        val temp = if (isCelsius) {
            weatherInfo.current.temperature.roundToInt().toString() + "°C"
        } else {
            ((weatherInfo.current.temperature * 9 / 5) + 32).roundToInt().toString() + "°F"
        }

        val description = WeatherDescription.getWeatherDescription(weatherInfo.current.weatherCode, weatherInfo.current.isDay)

        val humidity = weatherInfo.current.humidity?.let { "${it.roundToInt()}%" } ?: "--"
        val windSpeed = weatherInfo.current.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "--"

        val location = cityName ?: res.getString(R.string.share_current_location)

        return buildString {
            appendLine(res.getString(R.string.share_header))
            appendLine(res.getString(R.string.share_location, location))
            appendLine()
            appendLine(res.getString(R.string.share_temperature, temp))
            appendLine(res.getString(R.string.share_condition, description))
            appendLine(res.getString(R.string.share_humidity, humidity))
            appendLine(res.getString(R.string.share_wind, windSpeed))

            weatherInfo.current.apparentTemperature?.let {
                val feelsLike = if (isCelsius) {
                    it.roundToInt().toString() + "°C"
                } else {
                    ((it * 9 / 5) + 32).roundToInt().toString() + "°F"
                }
                appendLine(res.getString(R.string.share_feels_like, feelsLike))
            }

            weatherInfo.current.surfacePressure?.let {
                appendLine(res.getString(R.string.share_pressure, it.roundToInt()))
            }

            appendLine()
            appendLine(res.getString(R.string.share_powered_by))
        }
    }


}
