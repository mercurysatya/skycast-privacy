package com.vayu.weather.presentation.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.use_case.GetFavoritesUseCase
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import com.vayu.weather.domain.use_case.RemoveFavoriteUseCase
import com.vayu.weather.domain.use_case.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteWithWeather(
    val city: City,
    val weather: WeatherInfo?,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * SkyCast Favorites — extends the original [FavoritesViewModel] with weather
 * snapshots for each saved location so the favorites list can render rich
 * cards (temperature, condition, H/L, precipitation, AQI, alerts).
 *
 * Weather is fetched on a debounced refresh cycle; failing to fetch one city
 * does not affect the others.
 */
@HiltViewModel
class FavoritesWithWeatherViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    var favorites by mutableStateOf<List<FavoriteWithWeather>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            getFavoritesUseCase().debounce(300L).collectLatest { cities ->
                val snapshot = favorites.associateBy { it.city.id }
                favorites = cities.map { city ->
                    snapshot[city.id]?.copy(city = city) ?: FavoriteWithWeather(city, null, isLoading = true)
                }
                cities.forEach { refresh(it) }
            }
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

    fun refreshAll() {
        favorites.forEach { refresh(it.city) }
    }

    fun removeFavorite(city: City) {
        viewModelScope.launch {
            removeFavoriteUseCase(city)
        }
    }

    fun toggleFavorite(city: City) {
        viewModelScope.launch {
            toggleFavoriteUseCase(city)
        }
    }

    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val current = favorites.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        favorites = current
    }

    private fun update(city: City, block: (FavoriteWithWeather) -> FavoriteWithWeather) {
        favorites = favorites.map { if (it.city.id == city.id) block(it) else it }
    }
}
