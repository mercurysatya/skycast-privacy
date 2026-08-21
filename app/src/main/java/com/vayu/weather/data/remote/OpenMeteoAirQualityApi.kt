package com.vayu.weather.data.remote

import com.vayu.weather.data.remote.dto.AirQualityDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoAirQualityApi {

    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("current") current: String = "european_aqi,us_aqi,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide",
        @Query("timezone") timezone: String = "auto"
    ): AirQualityDto
}
