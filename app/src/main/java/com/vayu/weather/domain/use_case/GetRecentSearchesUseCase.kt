package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<City>> {
        return repository.getRecentSearches(limit)
    }
}
