package com.vayu.weather.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.use_case.ClearWeatherCacheUseCase
import com.vayu.weather.domain.use_case.DeleteAllLocalDataUseCase
import com.vayu.weather.presentation.weather.SettingsState
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.WidgetSize
import com.vayu.weather.presentation.weather.WindUnit
import com.vayu.weather.presentation.weather.AlertSeverity
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vayu.weather.data.billing.BillingManager
import com.vayu.weather.data.billing.BillingOffer
import com.vayu.weather.data.local.DataDeletionGuard
import com.vayu.weather.data.worker.WeatherAlertWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsManager: SettingsManager,
    private val deleteAllLocalDataUseCase: DeleteAllLocalDataUseCase,
    private val clearWeatherCacheUseCase: ClearWeatherCacheUseCase,
    private val billingManager: BillingManager,
    private val deletionGuard: DataDeletionGuard
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    /** Genuine Play Billing state for the paywall (offers, entitlement, messages). */
    val billingState = billingManager.state

    init {
        viewModelScope.launch {
            _state.value = settingsManager.loadAllPreferences()
        }
        // Verified entitlement is the only source of truth: re-query Play on
        // start (covers reinstall restore + revocation) and propagate every
        // change into the DataStore cache, UI state and ad gating.
        viewModelScope.launch {
            billingManager.refresh()
        }
        viewModelScope.launch {
            billingManager.state
                .map { it.entitlement.isPremium }
                .distinctUntilChanged()
                .collect { active ->
                    settingsManager.setPremiumFromBilling(active)
                    _state.update { it.copy(isPremium = active) }
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
            _state.update { it.copy(themeMode = mode) }
        }
    }

    /**
     * Opens the genuine Google Play purchase sheet for [offer].
     * There is no local unlock path: Premium is granted only after Play
     * reports a verified PURCHASED state (see [BillingManager]).
     */
    fun launchPremiumPurchase(activity: android.app.Activity, offer: BillingOffer) {
        viewModelScope.launch {
            billingManager.launchPurchase(activity, offer)
        }
    }

    /** Re-queries Play and reports the result — the real "restore purchase". */
    fun restorePremiumPurchases() {
        viewModelScope.launch {
            billingManager.restorePurchases()
        }
    }

    /** Re-verifies the entitlement (called on app start and resume). */
    fun refreshBilling() {
        viewModelScope.launch {
            billingManager.refresh()
        }
    }

    fun clearBillingMessage() {
        billingManager.clearMessage()
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val newUnit = if (_state.value.temperatureUnit == TemperatureUnit.CELSIUS)
                TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
            settingsManager.setTemperatureUnit(newUnit)
            _state.update { it.copy(temperatureUnit = newUnit) }
        }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch {
            settingsManager.setWindUnit(unit)
            _state.update { it.copy(windUnit = unit) }
        }
    }

    fun setUseDynamicColor(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setUseDynamicColor(value)
            _state.update { it.copy(useDynamicColor = value) }
        }
    }

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationsEnabled(value)
            _state.update { it.copy(notificationsEnabled = value) }
        }
    }

    fun setRainAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setRainAlertThreshold(value)
            _state.update { it.copy(rainAlertThreshold = value) }
        }
    }

    fun setWindAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setWindAlertThreshold(value)
            _state.update { it.copy(windAlertThreshold = value) }
        }
    }

    fun setEnableWindAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableWindAlerts(value)
            _state.update { it.copy(enableWindAlerts = value) }
        }
    }

    fun setUvAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setUvAlertThreshold(value)
            _state.update { it.copy(uvAlertThreshold = value) }
        }
    }

    fun setEnableUvAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableUvAlerts(value)
            _state.update { it.copy(enableUvAlerts = value) }
        }
    }

    fun setHeatAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setHeatAlertThreshold(value)
            _state.update { it.copy(heatAlertThreshold = value) }
        }
    }

    fun setEnableHeatAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableHeatAlerts(value)
            _state.update { it.copy(enableHeatAlerts = value) }
        }
    }

    fun setColdAlertThreshold(value: Int) {
        viewModelScope.launch {
            settingsManager.setColdAlertThreshold(value)
            _state.update { it.copy(coldAlertThreshold = value) }
        }
    }

    fun setEnableColdAlerts(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnableColdAlerts(value)
            _state.update { it.copy(enableColdAlerts = value) }
        }
    }

    // Quiet hours setters
    fun setQuietHoursEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setQuietHoursEnabled(value)
            _state.update { it.copy(quietHoursEnabled = value) }
        }
    }

    fun setQuietHoursStartHour(value: Int) {
        viewModelScope.launch {
            settingsManager.setQuietHoursStartHour(value)
            _state.update { it.copy(quietHoursStartHour = value) }
        }
    }

    fun setQuietHoursStartMinute(value: Int) {
        viewModelScope.launch {
            settingsManager.setQuietHoursStartMinute(value)
            _state.update { it.copy(quietHoursStartMinute = value) }
        }
    }

    fun setQuietHoursEndHour(value: Int) {
        viewModelScope.launch {
            settingsManager.setQuietHoursEndHour(value)
            _state.update { it.copy(quietHoursEndHour = value) }
        }
    }

    fun setQuietHoursEndMinute(value: Int) {
        viewModelScope.launch {
            settingsManager.setQuietHoursEndMinute(value)
            _state.update { it.copy(quietHoursEndMinute = value) }
        }
    }

    // Per-day notification time setters
    fun setNotificationTime1Enabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationTime1Enabled(value)
            _state.update { it.copy(notificationTime1Enabled = value) }
        }
    }

    fun setNotificationTime1Hour(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime1Hour(value)
            _state.update { it.copy(notificationTime1Hour = value) }
        }
    }

    fun setNotificationTime1Minute(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime1Minute(value)
            _state.update { it.copy(notificationTime1Minute = value) }
        }
    }

    fun setNotificationTime2Enabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationTime2Enabled(value)
            _state.update { it.copy(notificationTime2Enabled = value) }
        }
    }

    fun setNotificationTime2Hour(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime2Hour(value)
            _state.update { it.copy(notificationTime2Hour = value) }
        }
    }

    fun setNotificationTime2Minute(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime2Minute(value)
            _state.update { it.copy(notificationTime2Minute = value) }
        }
    }

    fun setNotificationTime3Enabled(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationTime3Enabled(value)
            _state.update { it.copy(notificationTime3Enabled = value) }
        }
    }

    fun setNotificationTime3Hour(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime3Hour(value)
            _state.update { it.copy(notificationTime3Hour = value) }
        }
    }

    fun setNotificationTime3Minute(value: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationTime3Minute(value)
            _state.update { it.copy(notificationTime3Minute = value) }
        }
    }

    suspend fun isOnboardingComplete(): Boolean =
        settingsManager.isOnboardingComplete()

    fun setOnboardingComplete() {
        viewModelScope.launch {
            settingsManager.setOnboardingComplete(true)
        }
    }

    fun deleteAllData() {
        if (_state.value.isDeleting) return
        _state.update {
            it.copy(
                isDeleting = true,
                dataDeleted = false,
                dataDeletionFailed = false
            )
        }
        viewModelScope.launch {
            try {
                deletionGuard.runDeletion {
                    // Let any in-flight worker finish its guarded write, then
                    // wipe it along with the stores. Workers that start during
                    // the wipe are rejected by the guard.
                    deleteAllLocalDataUseCase()
                    // The settings store is empty now. Keep notifications off
                    // explicitly because an absent key would default to on.
                    settingsManager.setNotificationsEnabled(false)
                    // Posted notifications can contain the deleted city and
                    // weather; remove them along with the underlying data.
                    cancelPostedNotifications()
                }
                // Forget the pre-deletion entitlement before re-querying Play.
                // A failed query must leave Premium off instead of re-caching
                // the stale in-memory value (refresh() keeps it by design).
                billingManager.resetEntitlement()
                refreshAllWidgets()
                _state.value = SettingsState(
                    notificationsEnabled = false,
                    isDeleting = false,
                    dataDeleted = true
                )
                // Re-query Play without delaying the deletion result. An
                // active subscription may re-grant Premium; a transport
                // failure leaves it off until the next successful query.
                launch {
                    try {
                        billingManager.refresh()
                        _state.update {
                            it.copy(
                                isPremium = billingManager.state.value.entitlement.isPremium
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Premium re-query after deletion failed", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Local data deletion failed", e)
                _state.update {
                    it.copy(isDeleting = false, dataDeletionFailed = true)
                }
            }
        }
    }

    /** Consumes the one-shot deletion snackbar event. */
    fun clearDeletionResult() {
        _state.update {
            it.copy(dataDeleted = false, dataDeletionFailed = false)
        }
    }

    private fun cancelPostedNotifications() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }

    fun setCheckIntervalHours(hours: Int) {
        viewModelScope.launch {
            settingsManager.setCheckIntervalHours(hours)
            _state.update { it.copy(checkIntervalHours = hours) }
            rescheduleAlertWorker(hours)
        }
    }

    private fun rescheduleAlertWorker(intervalHours: Int) {
        val workRequest = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WeatherAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun setSeverityFilter(severity: AlertSeverity) {
        viewModelScope.launch {
            settingsManager.setSeverityFilter(severity.name)
            _state.update { it.copy(severityFilter = severity) }
        }
    }

    fun setWidgetSize(size: WidgetSize) {
        viewModelScope.launch {
            settingsManager.setWidgetSize(size.name)
            _state.update { it.copy(widgetSize = size) }
            // Write to widget SharedPreferences so the widget can read it
            appContext.getSharedPreferences("weather_widget_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("widget_size", size.name).apply()
            refreshAllWidgets()
        }
    }

    private fun refreshAllWidgets() {
        viewModelScope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(com.vayu.weather.presentation.widget.WeatherWidget::class.java)
                glanceIds.forEach { glanceId ->
                    com.vayu.weather.presentation.widget.WeatherWidget().update(appContext, glanceId)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to refresh widgets", e)
            }
        }
    }

    fun clearWeatherCache() {
        viewModelScope.launch {
            clearWeatherCacheUseCase()
        }
    }

    fun setUse24hClock(value: Boolean) {
        viewModelScope.launch {
            settingsManager.setUse24hClock(value)
            _state.update { it.copy(use24hClock = value) }
        }
    }

    fun setPressureUnit(unit: String) {
        viewModelScope.launch {
            settingsManager.setPressureUnit(unit)
            _state.update { it.copy(pressureUnit = unit) }
        }
    }

    fun setPrecipitationUnit(unit: String) {
        viewModelScope.launch {
            settingsManager.setPrecipitationUnit(unit)
            _state.update { it.copy(precipitationUnit = unit) }
        }
    }

    fun setSectionVisibility(section: String, visible: Boolean) {
        viewModelScope.launch {
            when (section) {
                "hourly" -> { settingsManager.setShowHourlyForecast(visible); _state.update { it.copy(showHourlyForecast = visible) } }
                "sun_moon" -> { settingsManager.setShowSunMoon(visible); _state.update { it.copy(showSunMoon = visible) } }
                "air_quality" -> { settingsManager.setShowAirQuality(visible); _state.update { it.copy(showAirQuality = visible) } }
                "weather_details" -> { settingsManager.setShowWeatherDetails(visible); _state.update { it.copy(showWeatherDetails = visible) } }
            }
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
