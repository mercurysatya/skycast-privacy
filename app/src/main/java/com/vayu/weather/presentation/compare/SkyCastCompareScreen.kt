package com.vayu.weather.presentation.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.SkyCastTokens
import kotlin.math.roundToInt

/**
 * SkyCast Compare Locations screen.
 *
 * Side-by-side comparison of 2-4 cities. The UI is a metric table where the
 * leftmost column shows the metric label and each subsequent column shows
 * the value for a city. Tapping a city card selects that city as the new
 * default location.
 */
@Composable
fun SkyCastCompareScreen(
    selected: List<CityWeather>,
    maxCities: Int = 4,
    onAddCity: () -> Unit,
    onRemoveCity: (City) -> Unit,
    onCityTapped: (City) -> Unit = {},
    onBack: () -> Unit,
    isCelsius: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Rounded.Compare,
                contentDescription = null,
                tint = SkyBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Compare locations",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        com.vayu.weather.presentation.ads.AdBanner()

        if (selected.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Compare,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Add up to $maxCities cities to compare side by side",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // City header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Box(modifier = Modifier.weight(1.2f)) {}
                selected.take(maxCities).forEach { c ->
                    CompareCityHeader(
                        city = c,
                        isCelsius = isCelsius,
                        onRemove = { onRemoveCity(c.city) },
                        onTap = { onCityTapped(c.city) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (selected.size < maxCities) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(SkyCastTokens.RadiusLg))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable(onClick = onAddCity),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Metric rows
            val rows = listOf<Pair<String, (CityWeather) -> String>>(
                "Condition" to { c -> c.weather?.let { localizedCode(it.current.weatherCode, it.current.isDay) } ?: "—" },
                "Temperature" to { c -> c.weather?.let { "${convertTemp(it.current.temperature, isCelsius)}°" } ?: "—" },
                "Feels like" to { c -> c.weather?.current?.apparentTemperature?.let { "${convertTemp(it, isCelsius)}°" } ?: "—" },
                "High / Low" to { c -> c.weather?.daily?.firstOrNull()?.let { "${convertTemp(it.maxTemp, isCelsius)}° / ${convertTemp(it.minTemp, isCelsius)}°" } ?: "—" },
                "Rain" to { c -> c.weather?.hourly?.firstOrNull()?.precipitationProbability?.let { "$it%" } ?: "—" },
                "Wind" to { c -> c.weather?.current?.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "—" },
                "Humidity" to { c -> c.weather?.current?.humidity?.let { "${it.roundToInt()}%" } ?: "—" },
                "UV" to { c -> c.weather?.daily?.firstOrNull()?.uvIndex?.let { "${it.roundToInt()}" } ?: "—" }
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { (label, fn) ->
                    item(key = "row_$label") {
                        CompareRow(label = label, selected = selected, fn = fn)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareCityHeader(
    city: CityWeather,
    isCelsius: Boolean,
    onRemove: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(SkyCastTokens.RadiusLg))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onTap)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = SkyBlue,
                    modifier = Modifier.size(12.dp)
                )
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onRemove)
                )
            }
            Text(
                text = city.city.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            val temp = city.weather?.current?.temperature
            if (temp != null) {
                Text(
                    text = "${convertTemp(temp, isCelsius)}°",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    selected: List<CityWeather>,
    fn: (CityWeather) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkyCastTokens.RadiusMd))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(1.2f)
        )
        selected.forEach { c ->
            Text(
                text = fn(c),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun localizedCode(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "Clear" else "Clear night"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Cloudy"
}

private fun convertTemp(c: Double, isCelsius: Boolean): Int =
    if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()
