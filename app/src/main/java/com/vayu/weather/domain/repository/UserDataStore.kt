package com.vayu.weather.domain.repository

/**
 * Local user-data store that is not backed by Room.
 *
 * The data layer owns the concrete implementation; the deletion use case only
 * needs a single, testable "clear everything" operation. Implementations must
 * be idempotent so a failed or interrupted deletion can be retried safely.
 */
interface UserDataStore {
    suspend fun clearAll()
}
