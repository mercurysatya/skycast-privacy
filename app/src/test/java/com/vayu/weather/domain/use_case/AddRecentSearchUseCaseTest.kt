package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AddRecentSearchUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var useCase: AddRecentSearchUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = AddRecentSearchUseCase(repository)
    }

    @Test
    fun `invoke adds city to recent searches`() = runTest {
        val city = City(id = 1, name = "London", latitude = 51.5, longitude = -0.1, country = "UK", admin1 = null, countryCode = "GB")

        useCase(city)

        verify(repository).addRecentSearch(city)
    }
}
