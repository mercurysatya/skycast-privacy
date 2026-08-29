package com.vayu.weather.presentation.compare

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo

/**
 * A city + its current weather + loading/error state used by the
 * SkyCast Compare screen. Held in a list inside [CompareViewModel].
 */
data class CityWeather(
    val city: City,
    val weather: WeatherInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
