package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearRecentSearchesUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var useCase: ClearRecentSearchesUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = ClearRecentSearchesUseCase(repository)
    }

    @Test
    fun `invoke clears recent searches via repository`() = runTest {
        useCase()

        verify(repository).clearRecentSearches()
    }

    @Test
    fun `invoke calls repository exactly once`() = runTest {
        useCase()

        verify(repository, org.mockito.kotlin.times(1)).clearRecentSearches()
    }
}
