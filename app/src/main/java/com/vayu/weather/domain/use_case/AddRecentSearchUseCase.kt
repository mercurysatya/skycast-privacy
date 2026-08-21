package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class AddRecentSearchUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(city: City) {
        repository.addRecentSearch(city)
    }
}
