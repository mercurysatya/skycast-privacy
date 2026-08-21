package com.vayu.weather.presentation.favorites

import com.vayu.weather.domain.model.City

data class FavoritesState(
    val favorites: List<City> = emptyList()
)
