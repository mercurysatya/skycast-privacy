package com.vayu.weather.presentation.ads

/**
 * Guard that collapses concurrent/repeated ad-load triggers into a single
 * in-flight load per ad slot.
 *
 * Why: at cold start the app fires roughly two consecutive preloads — the
 * `ON_RESUME` observer and the UMP consent callback complete seconds apart,
 * much faster than a typical ad response. Without gating, both
 * `loadInterstitial()` calls issue real network ad requests and the first
 * response is discarded when `interstitialAd` is overwritten (same for
 * rewarded), doubling AdMob load requests for zero benefit.
 */
class AdLoadGate {

    private var inFlight = false

    /**
     * Claims the slot: exactly one caller gets `true` between a successful
     * [acquire] and its later [release]; all others get `false` and must skip
     * their load.
     */
    fun acquire(): Boolean = synchronized(this) {
        val start = !inFlight
        inFlight = true
        start
    }

    /** Frees the slot once the in-flight load settled (loaded or failed). */
    fun release() = synchronized(this) {
        inFlight = false
    }
}
