package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(city: City): Boolean {
        val isFavorite = repository.isFavoriteCity(city.id)
        if (isFavorite) {
            repository.removeFavoriteCity(city)
            return false
        } else {
            repository.addFavoriteCity(city)
            return true
        }
    }
}
