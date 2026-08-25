package com.vayu.weather.presentation.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.vayu.weather.BuildConfig
import com.vayu.weather.presentation.ConsentManager

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Never request or render ads without consent (Google EU User Consent Policy)
    if (!ConsentManager.canRequestAds(context)) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView ->
            adView.destroy()
        }
    )
}
