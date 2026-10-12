package com.vayu.weather.presentation.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.vayu.weather.BuildConfig
import com.vayu.weather.presentation.ConsentManager

/**
 * Central ad manager for SkyCast Weather.
 *
 * Strategy:
 * - Banner ads: persistent on Weather dashboard, Search, and Map screens
 * - Interstitial ads: shown on Weather→Detail transitions, frequency-capped at 1 per 3 minutes
 * - Rewarded ads: optional, unlocks detailed 10-day forecast, user-initiated
 */
object AdManager {
    @Volatile private var interstitialAd: InterstitialAd? = null
    @Volatile private var rewardedAd: RewardedAd? = null

    @Volatile
    private var mobileAdsInitialized = false

    /**
     * Bumped whenever the consent state changes so banner composables
     * (which read `canRequestAds` during composition) re-evaluate and appear
     * once consent allows ads — previously an EEA user who granted consent
     * never saw a banner until some unrelated recomposition.
     */
    var consentVersion = androidx.compose.runtime.mutableStateOf(0)
        private set

    fun onConsentChanged() {
        consentVersion.value++
    }

    /**
     * The single cached banner view. Compose disposes and recreates
     * AndroidViews as they scroll off a LazyColumn or as screens switch,
     * and every recreation issued a brand-new ad request. Reusing one
     * AdView keeps banner requests at a policy-friendly minimum.
     */
    @Volatile
    private var bannerAdView: com.google.android.gms.ads.AdView? = null

    /** Returns the shared banner AdView, creating and loading it once. */
    fun getOrCreateBannerView(context: Context): com.google.android.gms.ads.AdView {
        bannerAdView?.let { return it }
        synchronized(this) {
            bannerAdView?.let { return it }
            val view = com.google.android.gms.ads.AdView(context.applicationContext).apply {
                setAdSize(
                    com.google.android.gms.ads.AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        com.google.android.gms.ads.AdSize.FULL_WIDTH
                    )
                )
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
            }
            bannerAdView = view
            return view
        }
    }

    // Frequency capping: last interstitial shown time
    @Volatile private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 180_000L // 3 minutes

    // Track interstitial show count to limit per session
    @Volatile private var interstitialShowCount = 0
    private const val MAX_INTERSTITIALS_PER_SESSION = 10

    private const val TAG = "AdManager"

    /** Premium members never load or see an ad. */
    private fun adsDisabled(): Boolean =
        com.vayu.weather.presentation.premium.PremiumManager.isPremium

    /** Idempotent Mobile Ads initialization �?" must only run after consent allows ads. */
    fun initializeMobileAds(context: Context) {
        if (adsDisabled()) {
            Log.d(TAG, "Skipping Mobile Ads init - premium entitlement active")
            return
        }
        if (mobileAdsInitialized) return
        synchronized(this) {
            if (mobileAdsInitialized) return
            try {
                MobileAds.initialize(context.applicationContext) { status ->
                    Log.d(TAG, "AdMob initialized: ${status.adapterStatusMap}")
                }
                mobileAdsInitialized = true
            } catch (e: Exception) {
                Log.w(TAG, "MobileAds initialization failed", e)
            }
        }
    }

    // ======================== INTERSTITIAL ========================

    fun loadInterstitial(context: Context) {
        if (adsDisabled()) {
            Log.d(TAG, "Skipping interstitial load - premium entitlement active")
            return
        }
        if (!ConsentManager.canRequestAds(context)) {
            Log.d(TAG, "Skipping interstitial load — no consent")
            return
        }
        Log.d(TAG, "Loading interstitial ad")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /**
     * Show interstitial ad with frequency capping.
     * Respects: 3-minute interval, 10 per session limit, consent.
     * Calls onAdDismissed when done (whether ad showed or not).
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (adsDisabled()) {
            onAdDismissed()
            return
        }
        if (!ConsentManager.canRequestAds(activity)) {
            onAdDismissed()
            return
        }

        // Frequency cap
        val now = System.currentTimeMillis()
        if (now - lastInterstitialTime < INTERSTITIAL_MIN_INTERVAL_MS) {
            Log.d(TAG, "Interstitial skipped — frequency cap (${INTERSTITIAL_MIN_INTERVAL_MS / 1000}s cooldown)")
            onAdDismissed()
            return
        }

        if (interstitialShowCount >= MAX_INTERSTITIALS_PER_SESSION) {
            Log.d(TAG, "Interstitial skipped — session limit ($MAX_INTERSTITIALS_PER_SESSION)")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "Showing interstitial ad (show #${interstitialShowCount + 1})")
            interstitialAd = null
            lastInterstitialTime = now
            interstitialShowCount++

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    onAdDismissed()
                    preloadNext(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad shown successfully")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                    onAdDismissed()
                    preloadNext(activity)
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "No interstitial ad available — continuing")
            onAdDismissed()
        }
    }

    // ======================== REWARDED ========================

    fun loadRewardedAd(context: Context) {
        if (adsDisabled()) {
            Log.d(TAG, "Skipping rewarded ad load - premium entitlement active")
            return
        }
        if (!ConsentManager.canRequestAds(context)) {
            Log.d(TAG, "Skipping rewarded ad load — no consent")
            return
        }
        Log.d(TAG, "Loading rewarded ad")
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed: ${adError.message}")
                    rewardedAd = null
                }
            }
        )
    }

    /**
     * Show rewarded ad. User must opt-in (tap button).
     * Returns true if reward was granted.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onAdDismissed: () -> Unit
    ) {
        if (!ConsentManager.canRequestAds(activity)) {
            onAdDismissed()
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            Log.d(TAG, "Showing rewarded ad")
            rewardedAd = null

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    onAdDismissed()
                    preloadNext(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad shown")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    onAdDismissed()
                    preloadNext(activity)
                }
            }

            ad.show(activity) { rewardItem: RewardItem ->
                Log.d(TAG, "Reward granted: ${rewardItem.amount} ${rewardItem.type}")
                onRewardGranted()
            }
        } else {
            Log.d(TAG, "No rewarded ad available")
            onAdDismissed()
        }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null

    /** Preload next ad set — call after showing an interstitial. */
    fun preloadNext(context: Context) {
        if (interstitialAd == null) loadInterstitial(context)
        if (rewardedAd == null) loadRewardedAd(context)
    }
}
