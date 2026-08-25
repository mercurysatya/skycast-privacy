package com.vayu.weather.presentation.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.use_case.GetFavoritesUseCase
import com.vayu.weather.domain.use_case.RemoveFavoriteUseCase
import com.vayu.weather.domain.use_case.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    var state by mutableStateOf(FavoritesState())
        private set

    init {
        viewModelScope.launch {
            getFavoritesUseCase().collectLatest { cities ->
                state = state.copy(favorites = cities)
            }
        }
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
        val current = state.favorites.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        state = state.copy(favorites = current)
        // TODO: persist new order to database if order matters
    }
}
