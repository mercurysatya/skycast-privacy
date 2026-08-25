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
import com.vayu.weather.BuildConfig
import com.vayu.weather.presentation.ConsentManager

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    @Volatile
    private var mobileAdsInitialized = false
    private const val TAG = "AdManager"

    /** Idempotent Mobile Ads initialization - must only run after consent allows ads. */
    fun initializeMobileAds(context: Context) {
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

    fun loadInterstitial(context: Context) {
        if (!ConsentManager.canRequestAds(context)) {
            Log.d(TAG, "Skipping interstitial load - no consent to request ads")
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
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (!ConsentManager.canRequestAds(activity)) {
            onAdDismissed()
            return
        }
        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "Showing interstitial ad")
            interstitialAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    loadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad shown")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "No interstitial ad available to show")
            onAdDismissed()
        }
    }
}
