package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Persists a drag-reorder of the favorites list. The full ordered list is
 * passed in; indices are rewritten 0..n-1 inside a single transaction.
 */
class ReorderFavoritesUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(cities: List<City>) {
        repository.reorderFavoriteCities(cities)
    }
}
