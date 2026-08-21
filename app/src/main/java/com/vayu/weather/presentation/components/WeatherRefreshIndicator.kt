package com.vayu.weather.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WeatherRefreshIndicator(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val color = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            weatherCode == 0 -> SunRefreshIndicator(color, size)
            weatherCode in 1..3 -> CloudRefreshIndicator(color, size)
            weatherCode in 45..48 -> FogRefreshIndicator(color, size)
            weatherCode in 51..55 || weatherCode in 61..65 || weatherCode in 80..82 -> RainRefreshIndicator(color, size)
            weatherCode in 71..75 -> SnowRefreshIndicator(color, size)
            weatherCode in 95..99 -> StormRefreshIndicator(color, size)
            else -> CloudRefreshIndicator(color, size)
        }
    }
}

@Composable
private fun SunRefreshIndicator(color: Color, size: Dp) {
    val infinite = rememberInfiniteTransition(label = "sun_refresh")
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val pulse by infinite.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.toPx() / 2, size.toPx() / 2)
        val r = size.toPx() * 0.35f

        drawCircle(color.copy(alpha = 0.2f * pulse), r * 1.4f, c)
        drawCircle(color, r * pulse, c)

        for (i in 0 until 8) {
            val rad = (rotation + i * 45f) * (PI.toFloat() / 180f)
            val inner = r * 1.1f
            val outer = r * 1.6f
            drawLine(
                color = color.copy(alpha = 0.6f),
                start = Offset(c.x + inner * cos(rad), c.y + inner * sin(rad)),
                end = Offset(c.x + outer * cos(rad), c.y + outer * sin(rad)),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CloudRefreshIndicator(color: Color, size: Dp) {
    val infinite = rememberInfiniteTransition(label = "cloud_refresh")
    val progress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val pulse by infinite.animateFloat(
        0.7f, 1f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.toPx() / 2 + sin(progress * 2 * PI.toFloat()) * size.toPx() * 0.05f
        val cy = size.toPx() / 2
        val r = size.toPx() * 0.2f

        val alpha = 0.5f + 0.5f * pulse
        drawCircle(color.copy(alpha = alpha), r * 1.2f, Offset(cx, cy))
        drawCircle(color.copy(alpha = alpha), r * 0.85f, Offset(cx + r * 0.6f, cy - r * 0.2f))
        drawCircle(color.copy(alpha = alpha), r * 0.75f, Offset(cx - r * 0.55f, cy + r * 0.1f))
        drawCircle(color.copy(alpha = alpha), r * 0.65f, Offset(cx + r * 0.3f, cy - r * 0.4f))
    }
}

@Composable
private fun RainRefreshIndicator(color: Color, size: Dp) {
    val drops = remember {
        List(6) {
            RainDropData(
                x = 0.2f + Random.nextFloat() * 0.6f,
                delay = Random.nextFloat() * 0.5f,
                length = 0.15f + Random.nextFloat() * 0.1f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "rain_refresh")
    val progress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (d in drops) {
            val y = ((progress + d.delay) % 1.0f)
            val screenY = y * size.toPx()
            val screenX = d.x * size.toPx()
            val len = d.length * size.toPx()
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(screenX, screenY),
                end = Offset(screenX - len * 0.3f, screenY + len),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SnowRefreshIndicator(color: Color, size: Dp) {
    val flakes = remember {
        List(5) {
            SnowFlakeData(
                x = 0.2f + Random.nextFloat() * 0.6f,
                delay = Random.nextFloat() * 0.6f,
                swayAmp = 3f + Random.nextFloat() * 5f,
                size = 2f + Random.nextFloat() * 2f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "snow_refresh")
    val progress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "fall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (f in flakes) {
            val y = ((progress + f.delay) % 1.0f)
            val sway = sin((progress + f.delay) * 4 * PI.toFloat()) * f.swayAmp
            val screenX = f.x * size.toPx() + sway
            val screenY = y * size.toPx()
            drawCircle(color.copy(alpha = 0.6f), f.size, Offset(screenX, screenY))
        }
    }
}

@Composable
private fun FogRefreshIndicator(color: Color, size: Dp) {
    val layers = remember {
        List(4) {
            FogLayerData(
                x = 0.1f + Random.nextFloat() * 0.2f,
                delay = Random.nextFloat() * 0.4f,
                width = 0.3f + Random.nextFloat() * 0.3f,
                alpha = 0.08f + Random.nextFloat() * 0.08f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "fog_refresh")
    val progress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (l in layers) {
            val x = ((progress + l.delay) % 1.0f + 1.0f) % 1.0f
            drawCircle(
                color = color.copy(alpha = l.alpha),
                radius = l.width * size.toPx(),
                center = Offset(x * size.toPx(), size.toPx() / 2)
            )
        }
    }
}

@Composable
private fun StormRefreshIndicator(color: Color, size: Dp) {
    val infinite = rememberInfiniteTransition(label = "storm_refresh")
    val progress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "rain"
    )
    val flash by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flash"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until 4) {
            val x = (0.15f + i * 0.2f) * size.toPx()
            val y = ((progress + i * 0.15f) % 1.0f) * size.toPx()
            val len = size.toPx() * 0.12f
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(x, y),
                end = Offset(x - len * 0.3f, y + len),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
        }

        if (flash > 0.95f) {
            drawCircle(
                Color.White.copy(alpha = (flash - 0.95f) * 10f),
                size.toPx() * 0.4f,
                Offset(size.toPx() * 0.5f, size.toPx() * 0.3f)
            )
        }
    }
}

private data class RainDropData(val x: Float, val delay: Float, val length: Float)
private data class SnowFlakeData(val x: Float, val delay: Float, val swayAmp: Float, val size: Float)
private data class FogLayerData(val x: Float, val delay: Float, val width: Float, val alpha: Float)
