package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.UserDataStore
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DeleteAllLocalDataUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var userDataStore: UserDataStore
    private lateinit var useCase: DeleteAllLocalDataUseCase

    @Before
    fun setup() {
        repository = mock()
        userDataStore = mock()
        useCase = DeleteAllLocalDataUseCase(repository, userDataStore)
    }

    @Test
    fun `invoke deletes room and settings data`() = runTest {
        useCase()

        verify(repository).deleteAllLocalData()
        verify(userDataStore).clearAll()
    }

    @Test
    fun `invoke calls each store exactly once`() = runTest {
        useCase()

        verify(repository, org.mockito.kotlin.times(1)).deleteAllLocalData()
        verify(userDataStore, org.mockito.kotlin.times(1)).clearAll()
    }

    @Test
    fun `invoke clears database before settings`() = runTest {
        useCase()

        inOrder(repository, userDataStore).apply {
            verify(repository).deleteAllLocalData()
            verify(userDataStore).clearAll()
        }
    }
}
