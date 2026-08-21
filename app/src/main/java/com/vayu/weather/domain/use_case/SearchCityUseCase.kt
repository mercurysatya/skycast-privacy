package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class SearchCityUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(query: String): Result<List<City>> {
        return repository.searchCity(query)
    }
}
