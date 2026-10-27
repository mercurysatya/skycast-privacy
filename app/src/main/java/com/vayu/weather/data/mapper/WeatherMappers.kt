package com.vayu.weather.data.mapper

import android.util.Log
import com.vayu.weather.data.remote.dto.AirQualityCurrentDto
import com.vayu.weather.data.remote.dto.DailyWeatherDto
import com.vayu.weather.data.remote.dto.HourlyWeatherDto
import com.vayu.weather.data.remote.dto.WeatherDto
import com.vayu.weather.data.remote.dto.GeocodingResultDto
import com.vayu.weather.domain.model.*

fun WeatherDto.toWeatherInfo(): WeatherInfo {
    return try {
        WeatherInfo(
            current = CurrentWeather(
                time = current?.time ?: "",
                temperature = current?.temperature ?: 0.0,
                humidity = current?.humidity,
                weatherCode = current?.weatherCode ?: 0,
                windSpeed = current?.windSpeed,
                windDirection = current?.windDirection,
                apparentTemperature = current?.apparentTemperature,
                isDay = current?.isDay == 1,
                visibility = current?.visibility,
                // Prefer sea-level pressure: the hourly trend chart plots
                // pressure_msl, and the "High/Normal/Low pressure" thresholds
                // (~1013 hPa) are only meaningful at sea level. Mixing surface
                // pressure into the card showed two different "current pressure"
                // numbers on the same screen and mislabeled elevated cities.
                surfacePressure = current?.pressureMsl ?: current?.surfacePressure,
                windGusts = current?.windGusts,
                dewPoint = current?.dewPoint,
                cloudCover = current?.cloudCover,
                uvIndex = current?.uvIndex
            ),
            hourly = hourly?.toHourlyWeather() ?: emptyList(),
            daily = daily?.toDailyWeather() ?: emptyList(),
            timezone = timezone
        )
    } catch (e: Exception) {
        Log.e("WeatherMappers", "Error mapping WeatherDto to WeatherInfo", e)
        throw e
    }
}

fun HourlyWeatherDto.toHourlyWeather(): List<HourlyWeather> {
    return time.mapIndexedNotNull { index, timeStr ->
        if (timeStr == null) return@mapIndexedNotNull null
        // Missing temperature means the provider omitted this hour's data;
        // fabricating 0.0 would render "0°, Clear sky" cards.
        val temperature = temperatures.getOrNull(index) ?: return@mapIndexedNotNull null
        try {
            HourlyWeather(
                time = timeStr,
                temperature = temperature,
                weatherCode = weatherCodes.getOrNull(index) ?: 0,
                humidity = humidities?.getOrNull(index),
                pressure = pressures?.getOrNull(index),
                windSpeed = windSpeeds?.getOrNull(index),
                visibility = visibility?.getOrNull(index),
                precipitationProbability = precipitationProbabilities?.getOrNull(index),
                windDirection = windDirections?.getOrNull(index),
                precipitation = precipitations?.getOrNull(index),
                apparentTemperature = apparentTemperatures?.getOrNull(index)
            )
        } catch (e: Exception) {
            Log.e("WeatherMappers", "Error mapping HourlyWeather at index $index", e)
            null
        }
    }
}

fun DailyWeatherDto.toDailyWeather(): List<DailyWeather> {
    return time.mapIndexedNotNull { index, dateStr ->
        if (dateStr == null) return@mapIndexedNotNull null
        // Same as hourly: a null min/max must drop the day, not become 0 °C
        // (which triggered false "Cold Alert (0 °C)" notifications).
        val maxTemp = maxTemperatures.getOrNull(index) ?: return@mapIndexedNotNull null
        val minTemp = minTemperatures.getOrNull(index) ?: return@mapIndexedNotNull null
        try {
            DailyWeather(
                date = dateStr,
                weatherCode = weatherCodes.getOrNull(index) ?: 0,
                maxTemp = maxTemp,
                minTemp = minTemp,
                uvIndex = uvIndices?.getOrNull(index),
                precipitationProbability = precipitationProbabilities?.getOrNull(index),
                sunrise = sunrise?.getOrNull(index),
                sunset = sunset?.getOrNull(index)
            )
        } catch (e: Exception) {
            Log.e("WeatherMappers", "Error mapping DailyWeather at index $index", e)
            null
        }
    }
}

fun AirQualityCurrentDto.toAirQuality(): AirQuality {
    return AirQuality(
        europeanAqi = europeanAqi,
        usAqi = usAqi,
        pm25 = pm25,
        pm10 = pm10,
        nitrogenDioxide = nitrogenDioxide,
        ozone = ozone,
        sulphurDioxide = sulphurDioxide,
        carbonMonoxide = carbonMonoxide
    )
}

fun GeocodingResultDto.toCity(): City {
    return City(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        admin1 = admin1,
        countryCode = countryCode
    )
}
