package com.vayu.weather.domain.model

data class AirQuality(
    val europeanAqi: Int?,
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val sulphurDioxide: Double?,
    val carbonMonoxide: Double?
) {
    val aqiLabel: String
        get() {
            return when {
                europeanAqi != null -> europeanAqiLabel(europeanAqi)
                usAqi != null -> usAqiLabel(usAqi)
                else -> "--"
            }
        }

    val aqiColorIndex: Int
        get() {
            return when {
                europeanAqi != null -> europeanAqiColorIndex(europeanAqi)
                usAqi != null -> usAqiColorIndex(usAqi)
                else -> 0
            }
        }

    private fun europeanAqiLabel(aqi: Int): String = when {
        aqi <= 20 -> "Good"
        aqi <= 40 -> "Fair"
        aqi <= 60 -> "Moderate"
        aqi <= 80 -> "Poor"
        aqi <= 100 -> "Very Poor"
        else -> "Extremely Poor"
    }

    private fun europeanAqiColorIndex(aqi: Int): Int = when {
        aqi <= 20 -> 1
        aqi <= 40 -> 2
        aqi <= 60 -> 3
        aqi <= 80 -> 4
        aqi <= 100 -> 5
        else -> 6
    }

    private fun usAqiLabel(aqi: Int): String = when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy for Sensitive Groups"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }

    private fun usAqiColorIndex(aqi: Int): Int = when {
        aqi <= 50 -> 1
        aqi <= 100 -> 2
        aqi <= 150 -> 3
        aqi <= 200 -> 4
        aqi <= 300 -> 5
        else -> 6
    }
}
