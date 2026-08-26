package com.vayu.weather.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.ui.theme.*
import kotlin.math.roundToInt

/**
 * Enhanced multi-city comparison — shows weather across saved cities with ranking.
 */
@Composable
fun EnhancedCityComparison(
    cities: List<CityWeatherData>,
    currentCityIndex: Int,
    onCitySelected: (Int) -> Unit,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    if (cities.size < 2) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .animateContentSize()
            .semantics(mergeDescendants = true) {
                contentDescription = "Compare weather across ${cities.size} cities"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CompareArrows,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "City Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Horizontal scrollable city cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(cities.size) { index ->
                    val city = cities[index]
                    val isSelected = index == currentCityIndex
                    val isWarmest = city.tempC == cities.maxOfOrNull { it.tempC }
                    val isColdest = city.tempC == cities.minOfOrNull { it.tempC }

                    CityMiniCard(
                        city = city,
                        isSelected = isSelected,
                        isWarmest = isWarmest,
                        isColdest = isColdest,
                        isCelsius = isCelsius,
                        onClick = { onCitySelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CityMiniCard(
    city: CityWeatherData,
    isSelected: Boolean,
    isWarmest: Boolean,
    isColdest: Boolean,
    isCelsius: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val bgBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val badge = when {
        isWarmest -> "🔥 Warmest"
        isColdest -> "❄️ Coldest"
        else -> null
    }

    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(12.dp)
            .semantics {
                contentDescription = "${city.name}: ${city.tempC} degrees, ${city.condition}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (badge != null) {
            Text(
                text = badge,
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Text(
            text = city.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = city.icon,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${city.tempC}°",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isWarmest -> WarmOrange
                isColdest -> SkyBlue
                else -> Color.White
            }
        )

        Text(
            text = city.condition,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 9.sp
        )
    }
}

data class CityWeatherData(
    val name: String,
    val tempC: Double,
    val condition: String,
    val icon: String,
    val weatherCode: Int = 0,
    val humidity: Double = 0.0,
    val windSpeed: Double = 0.0,
    val isDay: Boolean = true
)
