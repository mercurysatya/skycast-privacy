package com.vayu.weather.domain.model

data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
    val countryCode: String?
)
