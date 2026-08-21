package com.vayu.weather.data.remote

import com.vayu.weather.data.remote.dto.RainViewerResponseDto
import retrofit2.http.GET

interface RainViewerApi {

    @GET("public/weather-maps.json")
    suspend fun getRadarFrames(): RainViewerResponseDto
}
