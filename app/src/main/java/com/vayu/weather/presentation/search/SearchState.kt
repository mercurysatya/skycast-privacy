package com.vayu.weather.presentation.search

import com.vayu.weather.domain.model.City

data class SearchState(
    val searchQuery: String = "",
    val searchResults: List<City> = emptyList(),
    val recentSearches: List<City> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
