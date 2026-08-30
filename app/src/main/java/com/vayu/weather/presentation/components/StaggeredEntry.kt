package com.vayu.weather.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Wraps content with a staggered fade + slide-up entrance animation.
 *
 * Usage:
 * ```
 * StaggeredEntry(delayIndex = 3) {
 *     MySection()
 * }
 * ```
 *
 * Each call with a unique `delayIndex` will animate in 60ms after the previous one.
 * Honors `LocalReduceMotion` (system animation scale = 0).
 */
@Composable
fun StaggeredEntry(
    delayIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = isReduceMotionEnabled()
    var visible by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = if (reduceMotion) 0 else delayIndex * 60, easing = FastOutSlowInEasing),
        label = "staggered_$delayIndex"
    )

    LaunchedEffect(delayIndex) {
        if (!reduceMotion) {
            // Small delay so the first frame renders
            delay(16)
        }
        visible = true
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = animProgress
                translationY = (1f - animProgress) * 16f
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun isReduceMotionEnabled(): Boolean {
    val view = LocalView.current
    return remember(view) {
        try {
            val resolver = view.context.contentResolver
            val durationScale = android.provider.Settings.Global.getFloat(
                resolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            durationScale == 0f
        } catch (_: Exception) { false }
    }
}