package com.vayu.weather.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirQualityDto(
    @Json(name = "current")
    val current: AirQualityCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentDto(
    @Json(name = "time")
    val time: String,
    @Json(name = "european_aqi")
    val europeanAqi: Int? = null,
    @Json(name = "us_aqi")
    val usAqi: Int? = null,
    @Json(name = "pm2_5")
    val pm25: Double? = null,
    @Json(name = "pm10")
    val pm10: Double? = null,
    @Json(name = "nitrogen_dioxide")
    val nitrogenDioxide: Double? = null,
    @Json(name = "ozone")
    val ozone: Double? = null,
    @Json(name = "sulphur_dioxide")
    val sulphurDioxide: Double? = null,
    @Json(name = "carbon_monoxide")
    val carbonMonoxide: Double? = null
)
