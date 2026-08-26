package com.vayu.weather.presentation.ads

import android.content.res.Resources
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.vayu.weather.BuildConfig
import com.vayu.weather.presentation.ConsentManager

/**
 * Adaptive banner ad that fills the screen width.
 * Respects GDPR consent — no ad requests without consent.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!ConsentManager.canRequestAds(context)) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, AdSize.FULL_WIDTH))
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView ->
            adView.destroy()
        }
    )
}

/**
 * Smart banner for compact areas — smaller height, still fills width.
 */
@Composable
fun AdBannerCompact(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!ConsentManager.canRequestAds(context)) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.SMART_BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}
