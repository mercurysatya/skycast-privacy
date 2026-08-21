package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(lat: Double, long: Double): Result<WeatherInfo> {
        return repository.getWeatherData(lat, long)
    }
}
