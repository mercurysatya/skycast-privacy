package com.vayu.weather.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Non-intrusive weather background.
 *
 * Unlike the previous implementation, this version renders animations in two
 * non-text zones:
 *   1. A top band (the upper ~25% of the screen) — animated clouds, sun rays,
 *      or night stars.
 *   2. A bottom band (the lower ~10% of the screen) — subtle puddles, rain
 *      accumulation, or snow drift.
 *
 * The center of the screen (where the temperature hero lives) is kept clear so
 * the giant temperature text is always legible.
 *
 * Honors `LocalReduceMotion` via the system `ANIMATOR_DURATION_SCALE` check.
 */
@Composable
fun WeatherBackgroundV2(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = isReduceMotionEnabled()
    val bgColors = remember(weatherCode, isDay) { computeSkyGradient(weatherCode, isDay) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = bgColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Top band — animations only in the top 28% of the screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            if (!reduceMotion) {
                when {
                    isDay && weatherCode == 0 -> SunnyTopBand()
                    isDay && weatherCode in 1..3 -> CloudyTopBand()
                    isDay && weatherCode in 45..48 -> FogTopBand()
                    isDay && (weatherCode in 51..55 || weatherCode in 61..65 || weatherCode in 80..82) -> RainTopBand(heavy = weatherCode in 65..82)
                    isDay && weatherCode in 71..75 -> SnowTopBand()
                    isDay && weatherCode in 95..99 -> StormTopBand()
                    !isDay -> NightTopBand(weatherCode)
                }
            }
        }

        // Bottom band — very subtle motion (puddle ripples, snow drift)
        if (!reduceMotion) {
            when {
                weatherCode in 51..55 || weatherCode in 61..65 || weatherCode in 80..82 -> RainyBottomBand()
                weatherCode in 71..75 -> SnowBottomBand()
            }
        }
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

// ============================================================
// TOP-BAND ANIMATIONS (low opacity, never overlap hero text)
// ============================================================

@Composable
private fun SunnyTopBand() {
    val infinite = rememberInfiniteTransition(label = "sun_top")
    val rayAngle by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "sun_ray"
    )
    val pulse by infinite.animateFloat(0.92f, 1f,
        infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sun_pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Sun in the top-right corner only — far from the centered temperature
        val cx = size.width * 0.85f
        val cy = size.height * 0.35f
        val r = size.minDimension * 0.06f

        // Soft glow
        drawCircle(Color(0x40FFD54F), r * 4f * pulse, Offset(cx, cy))
        drawCircle(Color(0x25FFD54F), r * 7f * pulse, Offset(cx, cy))

        // Subtle rays
        for (i in 0 until 12) {
            val rad = (rayAngle + i * 30f) * (PI.toFloat() / 180f)
            val inner = r * 1.1f
            val outer = r * 1.8f * pulse
            drawLine(
                Color(0x20FFD54F),
                Offset(cx + inner * cos(rad), cy + inner * sin(rad)),
                Offset(cx + outer * cos(rad), cy + outer * sin(rad)),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun CloudyTopBand() {
    val clouds = remember {
        List(5) {
            CloudDriftTop(
                x = Random.nextFloat(),
                speed = 0.08f + Random.nextFloat() * 0.15f,
                size = 0.08f + Random.nextFloat() * 0.10f,
                alpha = 0.10f + Random.nextFloat() * 0.10f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "cloud_top")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloud_drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (c in clouds) {
            val x = ((progress * c.speed + c.x) % 1.0f + 1.0f) % 1.0f
            val cx = x * size.width
            // Only place clouds in the top 30% of the band
            val cy = c.size * size.height * 1.5f
            val sr = c.size * size.width
            drawCircle(Color.White.copy(alpha = c.alpha), sr, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = c.alpha * 0.85f), sr * 0.75f, Offset(cx + sr * 0.4f, cy - sr * 0.1f))
            drawCircle(Color.White.copy(alpha = c.alpha * 0.75f), sr * 0.65f, Offset(cx - sr * 0.4f, cy + sr * 0.05f))
        }
    }
}

@Composable
private fun RainTopBand(heavy: Boolean) {
    val dropCount = if (heavy) 70 else 40
    val drops = remember(dropCount) {
        List(dropCount) {
            DropTop(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.4f,
                length = 0.02f + Random.nextFloat() * 0.03f,
                phase = Random.nextFloat()
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "rain_top")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "rain_fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (d in drops) {
            val y = ((progress * d.speed + d.phase) % 1.0f + 1.0f) % 1.0f
            val screenY = y * size.height
            val screenX = d.x * size.width
            val len = d.length * size.height
            drawLine(
                Color.White.copy(alpha = 0.14f),
                Offset(screenX, screenY),
                Offset(screenX - len * 0.3f, screenY + len),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
private fun SnowTopBand() {
    val flakes = remember {
        List(35) {
            FlakeTop(
                x = Random.nextFloat(),
                speed = 0.1f + Random.nextFloat() * 0.2f,
                size = 1.5f + Random.nextFloat() * 2.5f,
                sway = 6f + Random.nextFloat() * 10f,
                phase = Random.nextFloat() * 360f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "snow_top")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "snow_fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (f in flakes) {
            val y = ((progress * f.speed) % 1.0f + 1.0f) % 1.0f
            val rad = (progress * 360f + f.phase) * (PI.toFloat() / 180f)
            val swayOffset = sin(rad) * f.sway
            val screenX = f.x * size.width + swayOffset
            val screenY = y * size.height
            drawCircle(Color.White.copy(alpha = 0.6f), f.size, Offset(screenX, screenY))
        }
    }
}

@Composable
private fun FogTopBand() {
    val layers = remember {
        List(4) {
            FogLayerTop(
                x = Random.nextFloat(),
                speed = 0.06f + Random.nextFloat() * 0.10f,
                width = 0.4f + Random.nextFloat() * 0.4f,
                alpha = 0.04f + Random.nextFloat() * 0.04f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "fog_top")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog_drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (l in layers) {
            val x = ((progress * l.speed + l.x) % 1.0f + 1.0f) % 1.0f
            drawCircle(
                Color.White.copy(alpha = l.alpha),
                l.width * size.width,
                Offset(x * size.width, size.height * 0.4f)
            )
        }
    }
}

@Composable
private fun StormTopBand() {
    RainTopBand(heavy = true)
    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L + Random.nextLong(5000))
            flash = true; delay(80L); flash = false
            delay(100L); flash = true; delay(60L); flash = false
        }
    }
    if (flash) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Flash only affects the top band area, not the whole screen
            drawRect(
                Color.White.copy(alpha = 0.25f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
        }
    }
}

@Composable
private fun NightTopBand(code: Int) {
    val stars = remember {
        List(45) {
            StarDataTop(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f,
                size = 0.5f + Random.nextFloat() * 1.5f,
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                twinkleSpeed = 2f + Random.nextFloat() * 3f,
                twinklePhase = Random.nextFloat() * 360f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "night_top")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (star in stars) {
            val twinkle = sin((progress * 360f + star.twinklePhase) * (PI.toFloat() / 180f))
            val alpha = star.alpha * (0.5f + 0.5f * twinkle)
            drawCircle(
                Color.White.copy(alpha = alpha.coerceIn(0f, 1f) * 0.7f),
                star.size,
                Offset(star.x * size.width, star.y * size.height)
            )
        }
        // Subtle moon glow in top-right for clear nights
        if (code == 0) {
            drawCircle(Color.White.copy(alpha = 0.08f), size.minDimension * 0.10f, Offset(size.width * 0.85f, size.height * 0.35f))
            drawCircle(Color.White.copy(alpha = 0.18f), size.minDimension * 0.05f, Offset(size.width * 0.85f, size.height * 0.35f))
        }
    }
}

// ============================================================
// BOTTOM-BAND ANIMATIONS (very subtle, never overlap text)
// ============================================================

@Composable
private fun RainyBottomBand() {
    val infinite = rememberInfiniteTransition(label = "puddle")
    val pulse by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ripple"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 3 expanding ripples at the very bottom of the screen
        for (i in 0..2) {
            val phase = ((pulse + i * 0.33f) % 1f)
            val cx = size.width * (0.2f + 0.3f * i)
            val cy = size.height * 0.95f
            val r = phase * 30f
            drawCircle(
                Color.White.copy(alpha = (1f - phase) * 0.12f),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun SnowBottomBand() {
    val infinite = rememberInfiniteTransition(label = "snow_drift")
    val drift by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle horizontal drift lines (snow piles) at the bottom
        for (i in 0..2) {
            val y = size.height * (0.95f - i * 0.02f)
            val dx = sin(drift * 2f * PI.toFloat() + i) * 8f
            drawLine(
                Color.White.copy(alpha = 0.08f - i * 0.02f),
                Offset(0f, y),
                Offset(size.width, y + dx),
                strokeWidth = 2f
            )
        }
    }
}

// ============================================================
// Data classes
// ============================================================

private data class CloudDriftTop(val x: Float, val speed: Float, val size: Float, val alpha: Float)
private data class DropTop(val x: Float, val speed: Float, val length: Float, val phase: Float)
private data class FlakeTop(val x: Float, val speed: Float, val size: Float, val sway: Float, val phase: Float)
private data class FogLayerTop(val x: Float, val speed: Float, val width: Float, val alpha: Float)
private data class StarDataTop(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float
)
