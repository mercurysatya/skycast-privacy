package com.vayu.weather.presentation.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.vayu.weather.presentation.ConsentManager
import com.vayu.weather.presentation.premium.PremiumManager

/**
 * Adaptive banner ad that fills the screen width.
 * Respects GDPR consent — no ad requests without consent.
 *
 * The AdView is a process-wide singleton owned by [AdManager]: Compose
 * disposes/recreates AndroidViews when they leave a LazyColumn or when the
 * user switches tabs, and each recreation used to trigger a fresh ad request.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Subscribing to consentVersion makes this composable re-evaluate once
    // the UMP flow completes and ads become allowed.
    @Suppress("UNUSED_EXPRESSION") AdManager.consentVersion.value
    if (PremiumManager.isPremium) return
    if (!ConsentManager.canRequestAds(context)) return

    val adView = remember { AdManager.getOrCreateBannerView(context) }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
        onRelease = {
            // The cached view outlives individual screens on purpose; a new
            // parent may attach it again later. Do NOT destroy() here.
        }
    )
}
