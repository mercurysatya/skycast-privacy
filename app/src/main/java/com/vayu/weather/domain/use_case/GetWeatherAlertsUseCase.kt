package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWeatherAlertsUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(limit: Int = 50): Flow<List<WeatherAlert>> {
        return repository.getWeatherAlerts(limit)
    }
}
