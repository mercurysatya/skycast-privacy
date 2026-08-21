package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class ClearWeatherCacheUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke() {
        repository.clearWeatherCache()
    }
}
