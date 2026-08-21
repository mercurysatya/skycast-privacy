package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DeleteAllLocalDataUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var useCase: DeleteAllLocalDataUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = DeleteAllLocalDataUseCase(repository)
    }

    @Test
    fun `invoke deletes all local data via repository`() = runTest {
        useCase()

        verify(repository).deleteAllLocalData()
    }

    @Test
    fun `invoke calls repository exactly once`() = runTest {
        useCase()

        verify(repository, org.mockito.kotlin.times(1)).deleteAllLocalData()
    }
}
