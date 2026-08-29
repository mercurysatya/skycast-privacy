package com.vayu.weather.data.remote

import com.vayu.weather.data.remote.dto.WeatherDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,apparent_temperature,is_day,visibility,surface_pressure,wind_gusts_10m,dew_point_2m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,weather_code,pressure_msl,wind_speed_10m,visibility,precipitation_probability,wind_direction_10m,precipitation,apparent_temperature",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max,sunrise,sunset",
        @Query("timezone") timezone: String = "auto"
    ): WeatherDto

    /** Lightweight call for tap-to-weather on map */
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day",
        @Query("timezone") timezone: String = "auto"
    ): WeatherDto
}
