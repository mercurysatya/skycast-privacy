package com.vayu.weather.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RainViewerResponseDto(
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated") val generated: Long? = null,
    @Json(name = "host") val host: String? = null,
    @Json(name = "radar") val radar: RainViewerRadarDto? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerRadarDto(
    @Json(name = "past") val past: List<RainViewerFrameDto>? = null,
    @Json(name = "nowcast") val nowcast: List<RainViewerFrameDto>? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerFrameDto(
    @Json(name = "time") val time: Long,
    @Json(name = "path") val path: String
)
