package com.vayu.weather.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.getWeatherIcon
import com.vayu.weather.presentation.weather.localizedWeatherDescription
import kotlin.math.roundToInt

data class CityWeather(
    val name: String,
    val weatherInfo: WeatherInfo?,
    val isLoading: Boolean = false
)

@Composable
fun MultiCityComparison(
    cities: List<CityWeather>,
    isCelsius: Boolean,
    onCityClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cities.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Compare Cities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                cities.forEach { city ->
                    CityWeatherMiniCard(
                        city = city,
                        isCelsius = isCelsius,
                        onClick = { onCityClick(city.name) },
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CityWeatherMiniCard(
    city: CityWeather,
    isCelsius: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "city_card_scale"
    )

    val info = city.weatherInfo

    Card(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // City name
            Text(
                text = city.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (info != null) {
                // Weather icon
                Icon(
                    imageVector = getWeatherIcon(info.current.weatherCode, info.current.isDay),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Temperature
                val temp = if (isCelsius) info.current.temperature.roundToInt()
                    else (info.current.temperature * 9.0 / 5.0 + 32.0).roundToInt()
                Text(
                    text = "${temp}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // High/Low
                info.daily.firstOrNull()?.let { today ->
                    val high = if (isCelsius) today.maxTemp.roundToInt()
                        else (today.maxTemp * 9.0 / 5.0 + 32.0).roundToInt()
                    val low = if (isCelsius) today.minTemp.roundToInt()
                        else (today.minTemp * 9.0 / 5.0 + 32.0).roundToInt()
                    Text(
                        text = "H:$high° L:$low°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Condition
                Text(
                    text = localizedWeatherDescription(info.current.weatherCode, info.current.isDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else if (city.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            } else {
                Text(
                    text = "No data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
