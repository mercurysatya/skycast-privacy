package com.vayu.weather.presentation.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.use_case.AddRecentSearchUseCase
import com.vayu.weather.domain.use_case.ClearRecentSearchesUseCase
import com.vayu.weather.domain.use_case.GetRecentSearchesUseCase
import com.vayu.weather.domain.use_case.SearchCityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchCityUseCase: SearchCityUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val addRecentSearchUseCase: AddRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val repository: com.vayu.weather.domain.repository.WeatherRepository
) : ViewModel() {

    var state by mutableStateOf(SearchState())
        private set

    private var searchJob: Job? = null

    init {
        // Clean up duplicates created before fuzzy dedup existed, then observe the list
        viewModelScope.launch {
            runCatching { repository.dedupeRecentSearches() }
            loadRecentSearches()
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            getRecentSearchesUseCase().collectLatest { recentSearches ->
                state = state.copy(recentSearches = recentSearches)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        Log.d("SearchViewModel", "Query changed: $newQuery")
        state = state.copy(searchQuery = newQuery, error = null)
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            state = state.copy(searchResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(500L)
            Log.d("SearchViewModel", "Starting search for: $newQuery")
            state = state.copy(isLoading = true)
            searchCityUseCase(newQuery)
                .onSuccess { cities ->
                    Log.d("SearchViewModel", "Search success: ${cities.size} results")
                    state = state.copy(
                        searchResults = cities,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { e ->
                    Log.e("SearchViewModel", "Search failure", e)
                    state = state.copy(
                        isLoading = false,
                        error = "Search failed."
                    )
                }
        }
    }

    fun addToRecentSearches(city: City) {
        viewModelScope.launch {
            addRecentSearchUseCase(city)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            clearRecentSearchesUseCase()
        }
    }
}
