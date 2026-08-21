package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetAirQualityUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(lat: Double, long: Double): Result<AirQuality> {
        return repository.getAirQuality(lat, long)
    }
}
