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
 * Purchase flow is intentionally decoupled from the UI: [PremiumScreen]
 * talks to SettingsViewModel#setPremium, which writes through to DataStore
 * and then updates [isPremium] via [SettingsManager]. Swapping the local
 * unlock for Google Play Billing therefore only touches SettingsManager.
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

    data class Plan(
        val id: String,
        val title: String,
        val price: String,
        val period: String,
        val note: String? = null,
        val bestValue: Boolean = false
    )

    val plans: List<Plan> = listOf(
        Plan(
            id = "monthly",
            title = "Monthly",
            price = "₹49",
            period = "/ month"
        ),
        Plan(
            id = "annual",
            title = "Annual",
            price = "₹299",
            period = "/ year",
            note = "₹25 per month · billed once",
            bestValue = true
        ),
        Plan(
            id = "lifetime",
            title = "Lifetime",
            price = "₹799",
            period = "one-time",
            note = "Pay once, keep forever"
        )
    )

    val benefits: List<Pair<String, String>> = listOf(
        "Ad-free everywhere" to "No banners or interstitials, on every screen.",
        "Extended 48-hour outlook" to "Hour-by-hour detail for two full days, unlocked.",
        "Every premium layer" to "Radar, cloud cover and map layers without limits.",
        "Priority alerts" to "Severe-weather alerts surface first on the dashboard.",
        "Support independent weather" to "Keeps SkyCast fast, private and free of trackers."
    )
}
