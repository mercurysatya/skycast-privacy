package com.vayu.weather.presentation.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.R
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.WeatherOpacity
import com.vayu.weather.ui.theme.WeatherShapes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Live weather demo card shown on the first onboarding page.
 * Shows an animated "current weather" preview so users immediately
 * understand what the app provides.
 */
@Composable
fun OnboardingDemoCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val infinite = rememberInfiniteTransition(label = "demo_card")
    val sunPulse by infinite.animateFloat(0.85f, 1f,
        infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )
    val rayAngle by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(
            tween(60000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "ray_angle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live animated weather icon
            Canvas(modifier = Modifier.size(80.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.25f

                // Sun glow
                drawCircle(Color(0x60FFD54F), r * 4f * sunPulse, Offset(cx, cy))
                drawCircle(Color(0x45FFD54F), r * 7f * sunPulse, Offset(cx, cy))

                for (i in 0 until 12) {
                    val rad = (rayAngle + i * 30f) * (PI.toFloat() / 180f)
                    val inner = r * 1.0f
                    val outer = r * 2.0f * sunPulse
                    drawLine(
                        Color(0x35FFD54F),
                        Offset(cx + inner * cos(rad), cy + inner * sin(rad)),
                        Offset(cx + outer * cos(rad), cy + outer * sin(rad)),
                        strokeWidth = 2f
                    )
                }

                drawCircle(Color(0xFFFFD54F), r * 0.9f * sunPulse, Offset(cx, cy))
                drawCircle(Color(0xFFFFF59D), r * 0.5f * sunPulse, Offset(cx, cy))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "35°",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Thin,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.onboarding_demo_city),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.onboarding_demo_country),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sunny",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DemoMetric(icon = Icons.Rounded.WaterDrop, value = "65%", color = SkyBlue)
                        DemoMetric(icon = Icons.Rounded.Air, value = "12 km/h", color = Color(0xFF7DD3FC))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini hourly preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hours = listOf("Now", "+3h", "+6h", "+9h", "+12h")
                val temps = listOf(35, 33, 31, 29, 27)
                val icons = listOf(
                    Icons.Rounded.WbSunny,
                    Icons.Rounded.WbSunny,
                    Icons.Rounded.Cloud,
                    Icons.Rounded.Cloud,
                    Icons.Rounded.NightsStay
                )

                hours.forEachIndexed { i, h ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = h,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = icons[i],
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${temps[i]}°",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}