package com.vayu.weather.domain.model

object WeatherDescription {

    fun getWeatherDescription(weatherCode: Int, isDay: Boolean): String {
        return when (weatherCode) {
            0 -> if (isDay) "Clear Sky" else "Clear Night"
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
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Cloudy"
        }
    }
}
