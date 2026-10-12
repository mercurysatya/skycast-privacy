package com.vayu.weather.presentation.premium

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * SkyCast Premium entitlement.
 *
 * Persistence lives in [com.vayu.weather.data.local.SettingsManager]; this
 * object only mirrors the flag so non-composable call sites (AdManager,
 * AdBanner, workers) can read it synchronously without a coroutine.
 *
 * The entitlement is produced exclusively by verified Google Play purchase
 * state ([com.vayu.weather.data.billing.BillingManager]): Play reports a
 * PURCHASED purchase, it is acknowledged, and only then is the flag set via
 * SettingsManager#setPremiumFromBilling. There is no local unlock.
 */
object PremiumManager {

    /**
     * Snapshot state so Compose call sites (AdBanner, unlock cards) recompose
     * the moment the entitlement changes, while non-composable code can still
     * read it synchronously.
     */
    var isPremium by mutableStateOf(false)
        private set
    fun sync(value: Boolean) {
        isPremium = value
    }

    val benefits: List<Pair<String, String>> = listOf(
        "Ad-free everywhere" to "No banners or interstitials, on every screen.",
        "Extended 48-hour outlook" to "Hour-by-hour detail for two full days, unlocked.",
        "Every premium layer" to "Radar, cloud cover and map layers without limits.",
        "Priority alerts" to "Severe-weather alerts surface first on the dashboard.",
        "Support independent weather" to "Keeps SkyCast fast, private and free of trackers."
    )
}
