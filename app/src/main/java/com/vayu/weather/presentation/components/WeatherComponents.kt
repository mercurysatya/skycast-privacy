package com.vayu.weather.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.ui.theme.WeatherColors
import com.vayu.weather.ui.theme.WeatherOpacity
import com.vayu.weather.ui.theme.WeatherShapes

// ============================================================
// GLASS CARD — the core premium container
// ============================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = WeatherOpacity.GLASS_LIGHT,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = modifier
        .shadow(0.dp, WeatherShapes.cardLarge, clip = true)
        .clip(WeatherShapes.cardLarge)
        .background(Color.White.copy(alpha = alpha))
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )

    Box(modifier = cardModifier) {
        content()
    }
}

// ============================================================
// SECTION HEADER — reusable section title
// ============================================================

@Composable
fun WeatherSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY)
        )
        trailing?.invoke()
    }
}

// ============================================================
// METRIC CARD — for weather detail items
// ============================================================

@Composable
fun WeatherMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = Color.White.copy(alpha = WeatherOpacity.ICON),
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier,
        alpha = WeatherOpacity.GLASS_LIGHT,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_TERTIARY)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
                )
            }
        }
    }
}

// ============================================================
// PROGRESS RING — for UV, AQI gauges
// ============================================================

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = WeatherColors.sunny,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    strokeWidth: Float = 6f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress_ring"
    )

    Canvas(modifier = modifier) {
        val sweepAngle = animatedProgress * 270f
        val startAngle = 135f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val radius = (size.minDimension - strokeWidth) / 2f

        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
            style = stroke
        )

        if (animatedProgress > 0f) {
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                style = stroke
            )
        }
    }
}

// ============================================================
// WEATHER CHIP — for filtering tags
// ============================================================

@Composable
fun WeatherChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val bgColor = if (selected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
    val textColor = if (selected) color else Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

// ============================================================
// EMPTY STATE — for empty lists
// ============================================================

@Composable
fun WeatherEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.White.copy(alpha = 0.25f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = WeatherOpacity.TEXT_PRIMARY)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = WeatherOpacity.TEXT_SECONDARY)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = WeatherShapes.button
            ) {
                Text(
                    text = actionLabel,
                    color = Color.White
                )
            }
        }
    }
}

// ============================================================
// SEVERITY BADGE — for alerts
// ============================================================

@Composable
fun SeverityBadge(
    severity: String,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (severity) {
        "high" -> WeatherColors.uvVeryHigh to "High"
        "medium" -> WeatherColors.uvModerate to "Medium"
        "low" -> WeatherColors.aqiGood to "Low"
        else -> Color.White to severity
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ============================================================
// UPDATED AGO — for last updated time
// ============================================================

@Composable
fun LastUpdatedText(
    lastUpdated: String?,
    modifier: Modifier = Modifier
) {
    if (lastUpdated != null) {
        Text(
            text = lastUpdated,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = WeatherOpacity.TEXT_DISABLED),
            modifier = modifier,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
