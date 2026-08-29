package com.vayu.weather.presentation.favorites

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import com.vayu.weather.presentation.components.GlassCard
import com.vayu.weather.presentation.components.WeatherSectionHeader
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.WeatherOpacity
import com.vayu.weather.ui.theme.WeatherShapes
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FavoritesEmptyStateV2(
    onBrowseClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val infinite = rememberInfiniteTransition(label = "empty_state")
    val sunAngle by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(
            tween(20000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "sun_angle"
    )
    val cloudDrift by infinite.animateFloat(0f, 1f,
        infiniteRepeatable(
            tween(15000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "cloud_drift"
    )

    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated illustration
            Canvas(modifier = Modifier.size(200.dp)) {
                val w = size.width
                val h = size.height

                // Sun (top-right)
                val sunCx = (w * 0.75f + cos(sunAngle * PI / 180.0) * 12f).toFloat()
                val sunCy = (h * 0.25f + sin(sunAngle * PI / 180.0) * 12f).toFloat()
                drawCircle(Color(0xFFFFB74D), 28f, Offset(sunCx, sunCy))
                for (i in 0 until 8) {
                    val rad = ((sunAngle + i * 45f) * PI / 180.0).toFloat()
                    drawLine(
                        Color(0xFFFFB74D).copy(alpha = 0.4f),
                        Offset(sunCx + 30f * cos(rad), sunCy + 30f * sin(rad)),
                        Offset(sunCx + 45f * cos(rad), sunCy + 45f * sin(rad)),
                        strokeWidth = 2f
                    )
                }

                // Clouds drifting
                val cloud1x = (cloudDrift * 0.6f + 0.1f) * w
                drawCloud(cloud1x, h * 0.35f, w * 0.2f, 0.15f)
                val cloud2x = ((cloudDrift + 0.5f) % 1f) * w
                drawCloud(cloud2x, h * 0.25f, w * 0.15f, 0.12f)

                // Ground with city silhouette
                drawGround(w, h)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.empty_favorites_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_favorites_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBrowseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBlue
                ),
                shape = WeatherShapes.button
            ) {
                Text(
                    text = stringResource(R.string.browse_popular),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0C4A6E)
                )
            }
        }
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, baseR: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    drawCircle(color, baseR, Offset(cx, cy))
    drawCircle(color, baseR * 0.85f, Offset(cx + baseR * 0.5f, cy - baseR * 0.1f))
    drawCircle(color, baseR * 0.75f, Offset(cx - baseR * 0.4f, cy + baseR * 0.05f))
    drawCircle(color, baseR * 0.65f, Offset(cx + baseR * 0.25f, cy - baseR * 0.25f))
}

private fun DrawScope.drawGround(w: Float, h: Float) {
    val groundY = h * 0.75f
    // Ground
    drawRect(
        Color(0xFF334155),
        topLeft = Offset(0f, groundY),
        size = androidx.compose.ui.geometry.Size(w, h - groundY)
    )
    // City silhouette
    val buildings = listOf(
        Pair(0.1f, 0.45f), Pair(0.25f, 0.35f), Pair(0.4f, 0.5f),
        Pair(0.6f, 0.3f), Pair(0.75f, 0.4f), Pair(0.9f, 0.25f)
    )
    for ((xFrac, heightFrac) in buildings) {
        val bx = xFrac * w
        val bh = heightFrac * h
        drawRect(
            Color(0xFF1E293B),
            topLeft = Offset(bx - w * 0.04f, groundY - bh),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, bh)
        )
        // Windows
        for (i in 0 until 3) {
            drawRect(
                Color(0xFFFFD54F).copy(alpha = 0.3f),
                topLeft = Offset(bx - w * 0.03f, groundY - bh + 10f + i * 14f),
                size = androidx.compose.ui.geometry.Size(w * 0.06f, 6f)
            )
        }
    }
}