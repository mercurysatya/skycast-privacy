package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class DeleteWeatherAlertUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(alertId: Long) {
        repository.deleteWeatherAlert(alertId)
    }
}
