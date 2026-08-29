package com.vayu.weather.presentation.compare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.use_case.GetFavoritesUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SkyCast Compare ViewModel.
 *
 * Holds the list of cities the user wants to compare (max 4) and triggers a
 * weather refresh for each one. Failures on one city do not block the others.
 */
@HiltViewModel
class CompareViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getWeatherUseCase: GetWeatherUseCase
) : ViewModel() {

    var selected by mutableStateOf<List<CityWeather>>(emptyList())
        private set

    init {
        // Seed the comparison list with the first four favorites so the screen
        // is immediately useful when entered from Favorites.
        viewModelScope.launch {
            getFavoritesUseCase().collectLatest { cities ->
                if (selected.isEmpty() && cities.isNotEmpty()) {
                    val initial = cities.take(MAX_CITIES)
                    selected = initial.map { CityWeather(it, null, isLoading = true) }
                    initial.forEach { refresh(it) }
                }
            }
        }
    }

    fun add(city: City) {
        if (selected.any { it.city.id == city.id }) return
        if (selected.size >= MAX_CITIES) return
        selected = selected + CityWeather(city, null, isLoading = true)
        refresh(city)
    }

    fun remove(city: City) {
        selected = selected.filter { it.city.id != city.id }
    }

    fun select(city: City) {
        // The parent uses this to actually navigate to that city's dashboard.
        // Locally we just re-anchor the comparison to start with this city.
        if (selected.none { it.city.id == city.id }) {
            add(city)
        } else {
            // Move to front
            val match = selected.first { it.city.id == city.id }
            selected = (listOf(match) + selected.filter { it.city.id != city.id })
        }
    }

    fun refresh(city: City) {
        viewModelScope.launch {
            update(city) { it.copy(isLoading = true, error = null) }
            getWeatherUseCase(city.latitude, city.longitude)
                .onSuccess { info ->
                    update(city) { it.copy(weather = info, isLoading = false) }
                }
                .onFailure { e ->
                    update(city) { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun update(city: City, block: (CityWeather) -> CityWeather) {
        selected = selected.map { if (it.city.id == city.id) block(it) else it }
    }

    companion object {
        const val MAX_CITIES = 4
    }
}
