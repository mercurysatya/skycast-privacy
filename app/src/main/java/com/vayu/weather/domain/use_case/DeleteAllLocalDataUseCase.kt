package com.vayu.weather.domain.use_case

import com.vayu.weather.domain.repository.UserDataStore
import com.vayu.weather.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Deletes every category of locally stored user data.
 *
 * Room, the in-memory AQI cache and the widget/streak stores are cleared by
 * [WeatherRepository.deleteAllLocalData]; [UserDataStore] clears the settings
 * DataStore (preferences, last location, onboarding and Premium cache). Both
 * operations are idempotent, so a failed wipe can be retried.
 */
class DeleteAllLocalDataUseCase @Inject constructor(
    private val repository: WeatherRepository,
    private val userDataStore: UserDataStore
) {
    suspend operator fun invoke() {
        // Delete database/widget data first: if this fails, the settings store
        // is left intact and the user can retry without losing preferences.
        repository.deleteAllLocalData()
        userDataStore.clearAll()
    }
}
