package com.vayu.weather.presentation.search

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import com.vayu.weather.domain.use_case.AddRecentSearchUseCase
import com.vayu.weather.domain.use_case.ClearRecentSearchesUseCase
import com.vayu.weather.domain.use_case.GetRecentSearchesUseCase
import com.vayu.weather.domain.use_case.SearchCityUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var searchCityUseCase: SearchCityUseCase
    private lateinit var getRecentSearchesUseCase: GetRecentSearchesUseCase
    private lateinit var addRecentSearchUseCase: AddRecentSearchUseCase
    private lateinit var clearRecentSearchesUseCase: ClearRecentSearchesUseCase
    private lateinit var repository: WeatherRepository
    private lateinit var viewModel: SearchViewModel

    private val london = City(
        id = 1L,
        name = "London",
        latitude = 51.5,
        longitude = -0.1,
        country = "United Kingdom",
        admin1 = "England",
        countryCode = "GB"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        searchCityUseCase = mock()
        getRecentSearchesUseCase = mock()
        addRecentSearchUseCase = mock()
        clearRecentSearchesUseCase = mock()
        repository = mock()
        whenever(getRecentSearchesUseCase()).thenReturn(flowOf(emptyList()))
        viewModel = SearchViewModel(
            searchCityUseCase = searchCityUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            addRecentSearchUseCase = addRecentSearchUseCase,
            clearRecentSearchesUseCase = clearRecentSearchesUseCase,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearing a completed query resets loading and results`() = runTest(dispatcher) {
        whenever(searchCityUseCase(any())).thenReturn(Result.success(listOf(london)))

        viewModel.onQueryChange("London")
        advanceTimeBy(501)
        advanceUntilIdle()

        assertFalse(viewModel.state.isLoading)
        assertTrue(viewModel.state.searchResults.isNotEmpty())

        viewModel.onQueryChange("")

        assertFalse("Blank query must not leave the spinner running", viewModel.state.isLoading)
        assertTrue(viewModel.state.searchResults.isEmpty())
    }
}
