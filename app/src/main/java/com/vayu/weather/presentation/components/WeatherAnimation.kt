package com.vayu.weather.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Returns true when the user has enabled Reduce Motion in accessibility settings.
 * When true, weather background animations are suppressed to respect user preferences
 * and reduce battery drain.
 */
@Composable
private fun isReduceMotionEnabled(): Boolean {
    val view = androidx.compose.ui.platform.LocalView.current
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

@Composable
fun WeatherBackground(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColors = remember(weatherCode, isDay) { computeSkyGradient(weatherCode, isDay) }
    val reduceMotion = isReduceMotionEnabled()

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
        if (!isDay) {
            NightSkyOverlay(weatherCode)
        }

        when {
            reduceMotion -> { /* No animations — static gradient only */ }
            !isDay -> NightAnimation(weatherCode)
            weatherCode == 0 -> SunnyBackground()
            weatherCode in 1..3 -> CloudyBackground(weatherCode)
            weatherCode in 45..48 -> FogBackground()
            weatherCode in 51..55 || weatherCode in 61..65 || weatherCode in 80..82 -> RainyBackground(weatherCode)
            weatherCode in 71..75 -> SnowBackground()
            weatherCode in 95..99 -> ThunderstormBackground()
        }
    }
}

@Composable
fun WeatherAnimation(
    weatherCode: Int,
    modifier: Modifier = Modifier
) {
    val color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
    when (weatherCode) {
        0 -> SunnyAnimation(color, modifier)
        1, 2, 3 -> CloudyAnimation(color, modifier)
        45, 48 -> FogAnimation(color, modifier)
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> RainyAnimation(color, modifier)
        71, 73, 75 -> SnowAnimation(color, modifier)
        95, 96, 99 -> ThunderstormAnimation(color, modifier)
    }
}

/**
 * Dynamic weather-aware gradient colors.
 * Sunny → warm golden-blue, Rainy → cool dark slate, etc.
 */
fun computeSkyGradient(code: Int, isDay: Boolean): List<Color> {
    if (!isDay) return when (code) {
        0 -> listOf(Color(0xFF070D1F), Color(0xFF0D1B3E), Color(0xFF132650))
        1, 2, 3 -> listOf(Color(0xFF0A1228), Color(0xFF111E3A), Color(0xFF1A2D52))
        in 51..55, in 61..65, in 80..82 -> listOf(Color(0xFF080E1A), Color(0xFF0E1628), Color(0xFF14203A))
        in 95..99 -> listOf(Color(0xFF06090F), Color(0xFF0A0F1A), Color(0xFF0E1424))
        else -> listOf(Color(0xFF070D1F), Color(0xFF0D1B3E), Color(0xFF132650))
    }

    return when (code) {
        0 -> listOf(Color(0xFF2196F3), Color(0xFF42A5F5), Color(0xFF64B5F6)) // bright sunny blue
        1 -> listOf(Color(0xFF42A5F5), Color(0xFF5C9CE6), Color(0xFF7EC8F0)) // slightly muted
        2, 3 -> listOf(Color(0xFF546E7A), Color(0xFF607D8B), Color(0xFF78909C)) // slate grey
        45, 48 -> listOf(Color(0xFF78909C), Color(0xFF90A4AE), Color(0xFFB0BEC5)) // fog grey
        in 51..55 -> listOf(Color(0xFF37474F), Color(0xFF455A64), Color(0xFF546E7A)) // light rain
        in 61..65 -> listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF455A64)) // moderate rain
        in 80..82 -> listOf(Color(0xFF1B2838), Color(0xFF2C3E50), Color(0xFF34495E)) // heavy rain
        in 71..75 -> listOf(Color(0xFF90CAF9), Color(0xFFBBDEFB), Color(0xFFE3F2FD)) // snow
        in 95..99 -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)) // thunderstorm
        else -> listOf(Color(0xFF42A5F5), Color(0xFF5C9CE6), Color(0xFF7EC8F0))
    }
}

@Composable
private fun NightSkyOverlay(code: Int) {
    val stars = remember {
        List(60) {
            StarData(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f,
                size = 0.5f + Random.nextFloat() * 2f,
                alpha = 0.3f + Random.nextFloat() * 0.7f,
                twinkleSpeed = 2f + Random.nextFloat() * 3f,
                twinklePhase = Random.nextFloat() * 360f
            )
        }
    }

    val shootingStars = remember {
        List(3) {
            ShootingStarData(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                angle = -30f + Random.nextFloat() * 20f,
                speed = 0.5f + Random.nextFloat() * 0.5f,
                delay = 5f + Random.nextFloat() * 10f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "night")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )
    var shootingProgress by remember { mutableFloatStateOf(-1f) }
    var activeShootingStar by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            val starIndex = Random.nextInt(shootingStars.size)
            activeShootingStar = starIndex
            shootingProgress = 0f
            val duration = (3000L + Random.nextLong(2000))
            val startTime = System.currentTimeMillis()
            while (shootingProgress < 1f) {
                shootingProgress = (System.currentTimeMillis() - startTime).toFloat() / duration
                delay(16L)
            }
            shootingProgress = -1f
            activeShootingStar = -1
            delay((3000L + Random.nextLong(8000)))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (star in stars) {
            val twinkle = sin((progress * 360f + star.twinklePhase) * (PI.toFloat() / 180f))
            val alpha = star.alpha * (0.5f + 0.5f * twinkle)
            drawCircle(
                Color.White.copy(alpha = alpha.coerceIn(0f, 1f) * 0.8f),
                star.size,
                Offset(star.x * size.width, star.y * size.height)
            )
        }

        if (activeShootingStar >= 0 && shootingProgress >= 0f) {
            val s = shootingStars[activeShootingStar]
            val rad = s.angle * (PI.toFloat() / 180f)
            val len = size.width * 0.3f
            val startX = s.x * size.width + shootingProgress * size.width * 0.6f
            val startY = s.y * size.height + shootingProgress * size.height * 0.3f
            val endX = startX + cos(rad) * len
            val endY = startY + sin(rad) * len
            drawLine(
                Color.White.copy(alpha = (1f - shootingProgress) * 0.8f),
                Offset(startX, startY), Offset(endX, endY), strokeWidth = 2f
            )
            drawCircle(
                Color.White.copy(alpha = (1f - shootingProgress) * 0.6f),
                3f, Offset(startX, startY)
            )
        }
    }
}

// ============================================================
// NIGHT AURORA EFFECT (new!)
// ============================================================

@Composable
private fun NightAuroraOverlay() {
    val infinite = rememberInfiniteTransition(label = "aurora")
    val drift by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora_drift"
    )
    val shimmer by infinite.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "aurora_shimmer"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Aurora bands — subtle green/blue waves
        val auroraColors = listOf(
            Color(0xFF00E5FF).copy(alpha = 0.03f * shimmer),
            Color(0xFF69F0AE).copy(alpha = 0.04f * shimmer),
            Color(0xFF40C4FF).copy(alpha = 0.02f * shimmer)
        )

        for (i in 0..2) {
            val yOffset = h * 0.15f + i * h * 0.08f
            val waveAmplitude = 40f + i * 20f
            val phase = drift * 2 * PI + i * 1.2

            for (x in 0..w.toInt() step 8) {
                val xF = x.toFloat()
                val normalizedX = xF / w
                val waveY = yOffset + (sin(normalizedX * 4 * PI.toFloat() + phase.toFloat()) * waveAmplitude)
                val alpha = auroraColors[i].alpha * (1f - (normalizedX - 0.3f).coerceIn(0f, 0.7f) / 0.7f)

                if (alpha > 0.005f) {
                    drawCircle(
                        color = auroraColors[i].copy(alpha = alpha),
                        radius = 30f + i * 10f,
                        center = Offset(xF, waveY)
                    )
                }
            }
        }
    }
}

@Composable
private fun NightAnimation(code: Int) {
    // Show aurora on clear nights
    if (code == 0 || code in 1..3) {
        NightAuroraOverlay()
    }

    val isStorm = code in 95..99
    val isRain = code in 51..55 || code in 61..65 || code in 80..82

    if (isRain || isStorm) {
        RainyAnimation(Color.White, Modifier)
    }
    if (isStorm) {
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
                drawRect(Color.White.copy(alpha = 0.5f))
            }
        }
    }
    if (code in 71..75) {
        SnowAnimation(Color.White, Modifier)
    }
}

@Composable
private fun SunnyBackground() {
    val infinite = rememberInfiniteTransition(label = "sun_bg")
    val rayAngle by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "sun_ray"
    )
    val pulse by infinite.animateFloat(0.92f, 1f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sun_pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.12f
        val r = size.width * 0.1f

        drawCircle(Color(0x60FFD54F), r * 5f * pulse, Offset(cx, cy))
        drawCircle(Color(0x45FFD54F), r * 8f * pulse, Offset(cx, cy))
        drawCircle(Color(0x30FFD54F), r * 12f * pulse, Offset(cx, cy))

        for (i in 0 until 16) {
            val rad = (rayAngle + i * 22.5f) * (PI.toFloat() / 180f)
            val inner = r * 1.1f
            val outer = r * 2.8f * pulse
            drawLine(
                Color(0x35FFD54F),
                Offset(cx + inner * cos(rad), cy + inner * sin(rad)),
                Offset(cx + outer * cos(rad), cy + outer * sin(rad)),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
private fun CloudyBackground(code: Int) {
    val clouds = remember {
        List(8) {
            CloudDrift(
                x = Random.nextFloat(),
                y = 0.05f + Random.nextFloat() * 0.6f,
                speed = 0.15f + Random.nextFloat() * 0.35f,
                size = 0.08f + Random.nextFloat() * 0.15f,
                alpha = 0.15f + Random.nextFloat() * 0.2f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "cloud_bg")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloud_drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (c in clouds) {
            val x = ((progress * c.speed + c.x) % 1.0f + 1.0f) % 1.0f
            val cx = x * size.width
            val cy = c.y * size.height
            val sr = c.size * size.width
            val color = if (code == 0) Color.White else Color(0xFFECEFF1)
            drawCircle(color.copy(alpha = c.alpha), sr, Offset(cx, cy))
            drawCircle(color.copy(alpha = c.alpha * 0.9f), sr * 0.8f, Offset(cx + sr * 0.5f, cy - sr * 0.15f))
            drawCircle(color.copy(alpha = c.alpha * 0.8f), sr * 0.7f, Offset(cx - sr * 0.45f, cy + sr * 0.1f))
            drawCircle(color.copy(alpha = c.alpha * 0.7f), sr * 0.6f, Offset(cx + sr * 0.25f, cy - sr * 0.3f))
        }
    }
}

@Composable
private fun RainyBackground(code: Int) {
    val isHeavy = code in 65..82
    val dropCount = if (isHeavy) 120 else 70
    val drops = remember(dropCount) {
        List(dropCount) {
            DropData(
                x = Random.nextFloat(),
                speed = 0.25f + Random.nextFloat() * 0.6f,
                length = 0.02f + Random.nextFloat() * 0.035f,
                phase = Random.nextFloat()
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "rain_bg")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "rain_fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (d in drops) {
            val y = ((progress * d.speed + d.phase) % 1.0f + 1.0f) % 1.0f
            val screenY = y * size.height
            val screenX = d.x * size.width
            val len = d.length * size.height
            drawLine(
                Color.White.copy(alpha = 0.18f),
                Offset(screenX, screenY),
                Offset(screenX - len * 0.3f, screenY + len),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun SnowBackground() {
    val flakes = remember {
        List(80) {
            FlakeData(
                x = Random.nextFloat(),
                speed = 0.1f + Random.nextFloat() * 0.3f,
                size = 1.5f + Random.nextFloat() * 4.5f,
                sway = 12f + Random.nextFloat() * 25f,
                phase = Random.nextFloat() * 360f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "snow_bg")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "snow_fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (f in flakes) {
            val y = ((progress * f.speed) % 1.0f + 1.0f) % 1.0f
            val rad = (progress * 360f + f.phase) * (PI.toFloat() / 180f)
            val swayOffset = sin(rad) * f.sway
            val screenX = f.x * size.width + swayOffset
            val screenY = y * size.height
            val alpha = (1f - y).coerceIn(0.3f, 0.95f)
            drawCircle(Color.White.copy(alpha = alpha), f.size, Offset(screenX, screenY))
        }
    }
}

@Composable
private fun FogBackground() {
    val layers = remember {
        List(10) {
            FogLayer(
                x = Random.nextFloat(),
                y = 0.05f + Random.nextFloat() * 0.9f,
                speed = 0.05f + Random.nextFloat() * 0.15f,
                width = 0.3f + Random.nextFloat() * 0.6f,
                alpha = 0.03f + Random.nextFloat() * 0.06f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "fog_bg")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog_drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (l in layers) {
            val x = ((progress * l.speed + l.x) % 1.0f + 1.0f) % 1.0f
            drawCircle(
                Color.White.copy(alpha = l.alpha),
                l.width * size.width,
                Offset(x * size.width, l.y * size.height)
            )
        }
    }
}

@Composable
private fun ThunderstormBackground() {
    RainyBackground(95)

    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000L + Random.nextLong(4000))
            flash = true; delay(100L); flash = false
            delay(80L); flash = true; delay(70L); flash = false
        }
    }

    if (flash) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.White.copy(alpha = 0.7f))
        }
    }
}

// ============================================================
// ICON-LEVEL ANIMATIONS (for detail screens)
// ============================================================

@Composable
private fun SunnyAnimation(color: Color, modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "sunny")
    val rotation by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val pulse by infinite.animateFloat(0.6f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.35f
        val r = minOf(size.width, size.height) * 0.25f
        val warm = Color(0xFFFFB74D)

        for (i in 0 until 8) {
            val rad = (rotation + i * 45f) * (PI.toFloat() / 180f)
            val inner = r * 0.8f * pulse
            val outer = r * 1.3f * pulse
            drawLine(
                color = warm.copy(alpha = 0.3f),
                start = Offset(cx + inner * cos(rad), cy + inner * sin(rad)),
                end = Offset(cx + outer * cos(rad), cy + outer * sin(rad)),
                strokeWidth = 3f
            )
        }
        drawCircle(warm.copy(alpha = 0.15f * pulse), r * 0.6f, Offset(cx, cy))
        drawCircle(warm.copy(alpha = 0.08f), r * 0.9f, Offset(cx, cy))
    }
}

@Composable
private fun CloudyAnimation(color: Color, modifier: Modifier) {
    val clouds = remember {
        List(12) {
            CloudData(
                x = Random.nextFloat(),
                y = 0.05f + Random.nextFloat() * 0.6f,
                speed = 0.08f + Random.nextFloat() * 0.25f,
                size = 0.1f + Random.nextFloat() * 0.2f,
                alpha = 0.08f + Random.nextFloat() * 0.18f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "cloud_bg")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloud_drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (c in clouds) {
            val x = ((progress * c.speed + c.x) % 1.0f + 1.0f) % 1.0f
            val cx = x * size.width
            val cy = c.y * size.height
            val sr = c.size * size.width
            val a = c.alpha
            drawCircle(color.copy(alpha = a), sr, Offset(cx, cy))
            drawCircle(color.copy(alpha = a * 0.85f), sr * 0.75f, Offset(cx + sr * 0.55f, cy - sr * 0.12f))
            drawCircle(color.copy(alpha = a * 0.75f), sr * 0.65f, Offset(cx - sr * 0.5f, cy + sr * 0.08f))
            drawCircle(color.copy(alpha = a * 0.65f), sr * 0.55f, Offset(cx + sr * 0.3f, cy - sr * 0.28f))
        }
    }
}

@Composable
private fun RainyAnimation(color: Color, modifier: Modifier) {
    val drops = remember {
        List(40) {
            DropData(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.5f,
                length = 0.015f + Random.nextFloat() * 0.02f,
                phase = Random.nextFloat()
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "rain")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (d in drops) {
            val y = ((progress * d.speed + d.phase) % 1.0f + 1.0f) % 1.0f
            val screenY = y * size.height
            val screenX = d.x * size.width
            val len = d.length * size.height
            drawLine(
                color.copy(alpha = 0.2f),
                Offset(screenX, screenY),
                Offset(screenX - len * 0.3f, screenY + len),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
private fun SnowAnimation(color: Color, modifier: Modifier) {
    val flakes = remember {
        List(35) {
            FlakeData(
                x = Random.nextFloat(),
                speed = 0.2f + Random.nextFloat() * 0.4f,
                size = 2f + Random.nextFloat() * 4f,
                sway = 10f + Random.nextFloat() * 20f,
                phase = Random.nextFloat() * 360f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "snow")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (f in flakes) {
            val y = ((progress * f.speed) % 1.0f + 1.0f) % 1.0f
            val rad = (progress * 360f + f.phase) * (PI.toFloat() / 180f)
            val swayOffset = sin(rad) * f.sway
            val screenX = f.x * size.width + swayOffset
            val screenY = y * size.height
            drawCircle(color.copy(alpha = 0.3f), f.size, Offset(screenX, screenY))
        }
    }
}

@Composable
private fun FogAnimation(color: Color, modifier: Modifier) {
    val layers = remember {
        List(8) {
            FogLayer(
                x = Random.nextFloat(),
                y = 0.1f + Random.nextFloat() * 0.8f,
                speed = 0.1f + Random.nextFloat() * 0.3f,
                width = 0.3f + Random.nextFloat() * 0.4f,
                alpha = 0.04f + Random.nextFloat() * 0.04f
            )
        }
    }
    val infinite = rememberInfiniteTransition(label = "fog")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (l in layers) {
            val x = ((progress * l.speed + l.x) % 1.0f + 1.0f) % 1.0f
            drawCircle(
                color = color.copy(alpha = l.alpha),
                radius = l.width * size.width,
                center = Offset(x * size.width, l.y * size.height)
            )
        }
    }
}

@Composable
private fun ThunderstormAnimation(color: Color, modifier: Modifier) {
    val drops = remember {
        List(50) {
            DropData(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                length = 0.015f + Random.nextFloat() * 0.02f,
                phase = Random.nextFloat()
            )
        }
    }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L + Random.nextLong(4000))
            flashAlpha = 0.6f; delay(80L); flashAlpha = 0f
            delay(120L); flashAlpha = 0.4f; delay(60L); flashAlpha = 0f
        }
    }

    val infinite = rememberInfiniteTransition(label = "thunder")
    val progress by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        for (d in drops) {
            val y = ((progress * d.speed + d.phase) % 1.0f + 1.0f) % 1.0f
            drawLine(
                color.copy(alpha = 0.2f),
                Offset(d.x * size.width, y * size.height),
                Offset(d.x * size.width - d.length * size.height * 0.3f, (y + d.length) * size.height),
                strokeWidth = 1.5f
            )
        }
        if (flashAlpha > 0f) {
            drawRect(Color.White.copy(alpha = flashAlpha))
        }
    }
}

private data class CloudData(val x: Float, val y: Float, val speed: Float, val size: Float, val alpha: Float)
private data class DropData(val x: Float, val speed: Float, val length: Float, val phase: Float)
private data class FlakeData(val x: Float, val speed: Float, val size: Float, val sway: Float, val phase: Float)
private data class FogLayer(val x: Float, val y: Float, val speed: Float, val width: Float, val alpha: Float)
private data class StarData(val x: Float, val y: Float, val size: Float, val alpha: Float, val twinkleSpeed: Float, val twinklePhase: Float)
private data class ShootingStarData(val x: Float, val y: Float, val angle: Float, val speed: Float, val delay: Float)
private data class CloudDrift(val x: Float, val y: Float, val speed: Float, val size: Float, val alpha: Float)
