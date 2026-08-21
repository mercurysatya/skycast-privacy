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
                surfacePressure = current?.surfacePressure,
                windGusts = current?.windGusts,
                dewPoint = current?.dewPoint
            ),
            hourly = hourly?.toHourlyWeather() ?: emptyList(),
            daily = daily?.toDailyWeather() ?: emptyList()
        )
    } catch (e: Exception) {
        Log.e("WeatherMappers", "Error mapping WeatherDto to WeatherInfo", e)
        throw e
    }
}

fun HourlyWeatherDto.toHourlyWeather(): List<HourlyWeather> {
    return time.mapIndexedNotNull { index, timeStr ->
        try {
            HourlyWeather(
                time = timeStr,
                temperature = temperatures.getOrNull(index) ?: 0.0,
                weatherCode = weatherCodes.getOrNull(index) ?: 0,
                humidity = humidities?.getOrNull(index),
                pressure = pressures?.getOrNull(index),
                windSpeed = windSpeeds?.getOrNull(index),
                visibility = visibility?.getOrNull(index)
            )
        } catch (e: Exception) {
            Log.e("WeatherMappers", "Error mapping HourlyWeather at index $index", e)
            null
        }
    }
}

fun DailyWeatherDto.toDailyWeather(): List<DailyWeather> {
    return time.mapIndexedNotNull { index, dateStr ->
        try {
            DailyWeather(
                date = dateStr,
                weatherCode = weatherCodes.getOrNull(index) ?: 0,
                maxTemp = maxTemperatures.getOrNull(index) ?: 0.0,
                minTemp = minTemperatures.getOrNull(index) ?: 0.0,
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
