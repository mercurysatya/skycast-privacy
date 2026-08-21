package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearWeatherCacheUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var useCase: ClearWeatherCacheUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = ClearWeatherCacheUseCase(repository)
    }

    @Test
    fun `invoke clears weather cache via repository`() = runTest {
        useCase()

        verify(repository).clearWeatherCache()
    }

    @Test
    fun `invoke calls repository exactly once`() = runTest {
        useCase()

        verify(repository, org.mockito.kotlin.times(1)).clearWeatherCache()
    }
}
