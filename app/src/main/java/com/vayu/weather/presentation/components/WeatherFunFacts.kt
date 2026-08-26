package com.vayu.weather.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.ui.theme.*
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Weather Fun Facts — unique educational content that makes the app shareable.
 * Users love sharing interesting weather facts on social media.
 */
@Composable
fun WeatherFunFacts(
    info: WeatherInfo,
    modifier: Modifier = Modifier
) {
    val facts = remember(info) { generateWeatherFacts(info) }
    var currentFactIndex by remember { mutableStateOf(0) }
    val view = LocalView.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Weather Fun Facts"
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
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AmberGlow
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weather Fun Fact",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.weight(1f))
                // Refresh button
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Next fact",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            currentFactIndex = (currentFactIndex + 1) % facts.size
                        },
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            AnimatedContent(
                targetState = currentFactIndex,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 2 } togetherWith
                    fadeOut() + slideOutVertically { -it / 2 }
                },
                label = "fact_transition"
            ) { index ->
                val fact = facts[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fact.color.copy(alpha = 0.12f))
                        .padding(14.dp)
                        .semantics {
                            contentDescription = "${fact.category}: ${fact.fact}"
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(fact.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = fact.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = fact.color
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = fact.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = fact.color
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = fact.fact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    if (fact.source.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Source: ${fact.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Dots indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                facts.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == currentFactIndex) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentFactIndex) AmberGlow
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

private data class WeatherFact(
    val icon: ImageVector,
    val category: String,
    val fact: String,
    val source: String,
    val color: Color
)

private fun generateWeatherFacts(info: WeatherInfo): List<WeatherFact> {
    val allFacts = mutableListOf<WeatherFact>()
    val code = info.current.weatherCode
    val temp = info.current.temperature
    val humidity = info.current.humidity ?: 50.0
    val windSpeed = info.current.windSpeed ?: 0.0
    val uv = info.daily.firstOrNull()?.uvIndex ?: 0.0

    // Temperature facts
    if (temp > 35) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.Thermostat,
            category = "Heat Record",
            fact = "The highest temperature ever recorded on Earth was 56.7°C (134°F) in Death Valley, California on July 10, 1913. Stay cool today!",
            source = "World Meteorological Organization",
            color = SunsetRed
        )
    } else if (temp < 0) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.AcUnit,
            category = "Cold Record",
            fact = "The lowest temperature ever recorded was -89.2°C (-128.6°F) at Vostok Station, Antarctica on July 21, 1983.",
            source = "World Meteorological Organization",
            color = SkyBlue
        )
    }

    // Rain facts
    if (code in 51..82 || code in 95..99) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.Umbrella,
            category = "Rain Science",
            fact = "A single raindrop falls at about 14 mph (22 km/h). In a heavy thunderstorm, a cloud can release 2 billion gallons of water!",
            source = "NOAA",
            color = SkyBlue
        )
    }

    // Wind facts
    if (windSpeed > 30) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.Air,
            category = "Wind Power",
            fact = "The fastest wind speed ever recorded was 407 km/h (253 mph) during Tropical Cyclone Olivia on Barrow Island, Australia in 1996.",
            source = "World Meteorological Organization",
            color = FreshGreen
        )
    }

    // UV facts
    if (uv > 6) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.WbSunny,
            category = "UV Science",
            fact = "UV rays can penetrate clouds — up to 80% of UV radiation passes through light cloud cover. Always wear sunscreen even on cloudy days!",
            source = "WHO",
            color = AmberGlow
        )
    }

    // Humidity facts
    if (humidity > 80) {
        allFacts += WeatherFact(
            icon = Icons.Rounded.WaterDrop,
            category = "Humidity Facts",
            fact = "When humidity reaches 100%, the air is saturated and cannot hold more moisture. This is when fog forms or dew appears on grass.",
            source = "NOAA",
            color = SkyBlue
        )
    }

    // Universal facts (always shown)
    allFacts += WeatherFact(
        icon = Icons.Rounded.Bolt,
        category = "Lightning Science",
        fact = "Lightning strikes the Earth about 8 million times per day. That's about 100 strikes every second! The air around a bolt can reach 30,000°C.",
        source = "National Weather Service",
        color = AmberGlow
    )

    allFacts += WeatherFact(
        icon = Icons.Rounded.Cloud,
        category = "Cloud Science",
        fact = "A typical cumulus cloud weighs about 500,000 kg (1.1 million pounds) — roughly the weight of 100 elephants! They float because the water droplets are spread out.",
        source = "NASA",
        color = Color(0xFF90CAF9)
    )

    allFacts += WeatherFact(
        icon = Icons.Rounded.WaterDrop,
        category = "Water Cycle",
        fact = "The water you drink today may have been drunk by a dinosaur 200 million years ago! Water on Earth is continuously recycled through the water cycle.",
        source = "USGS",
        color = SkyBlue
    )

    allFacts += WeatherFact(
        icon = Icons.Rounded.NightsStay,
        category = "Moon Science",
        fact = "The Moon affects Earth's tides through gravitational pull. During a full moon or new moon, tides are highest (spring tides) because the Sun and Moon align.",
        source = "NASA",
        color = Color(0xFF7986CB)
    )

    allFacts += WeatherFact(
        icon = Icons.Rounded.WbSunny,
        category = "Solar Facts",
        fact = "The Sun is actually white, not yellow! It appears yellow from Earth because our atmosphere scatters blue light. In space, it looks pure white.",
        source = "NASA",
        color = AmberGlow
    )

    allFacts += WeatherFact(
        icon = Icons.Rounded.Terrain,
        category = "Atmospheric Science",
        fact = "The atmosphere extends about 100 km above Earth, but 99% of its mass is within just 32 km of the surface. The troposphere where weather happens is only 12 km thick.",
        source = "NOAA",
        color = FreshGreen
    )

    // Shuffle but put weather-specific facts first
    val weatherSpecific = allFacts.filter { it != allFacts.last() }
    val universal = allFacts.filter { it == allFacts.last() || it.source == "NASA" || it.source == "NOAA" }

    return (weatherSpecific + universal.shuffled()).distinctBy { it.fact }.take(8)
}
