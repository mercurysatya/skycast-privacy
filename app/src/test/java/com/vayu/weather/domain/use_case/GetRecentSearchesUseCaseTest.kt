package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetRecentSearchesUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var useCase: GetRecentSearchesUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = GetRecentSearchesUseCase(repository)
    }

    @Test
    fun `invoke returns recent searches from repository`() = runTest {
        val expectedCities = listOf(
            City(id = 1, name = "London", latitude = 51.5, longitude = -0.1, country = "UK", admin1 = null, countryCode = "GB"),
            City(id = 2, name = "Paris", latitude = 48.8, longitude = 2.3, country = "France", admin1 = null, countryCode = "FR")
        )
        whenever(repository.getRecentSearches(10)).thenReturn(flowOf(expectedCities))

        val result = useCase().first()

        assertEquals(expectedCities, result)
    }

    @Test
    fun `invoke with custom limit passes limit to repository`() = runTest {
        val expectedCities = listOf(
            City(id = 1, name = "London", latitude = 51.5, longitude = -0.1, country = "UK", admin1 = null, countryCode = "GB")
        )
        whenever(repository.getRecentSearches(5)).thenReturn(flowOf(expectedCities))

        val result = useCase(5).first()

        assertEquals(expectedCities, result)
    }
}
