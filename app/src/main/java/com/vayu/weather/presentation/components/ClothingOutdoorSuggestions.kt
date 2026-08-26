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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vayu.weather.domain.model.WeatherInfo
import kotlin.math.roundToInt

// ─── Data Models ──────────────────────────────────────────────

private data class ClothingItem(
    val icon: ImageVector,
    val label: String,
    val detail: String,
    val color: Color,
    val priority: Int // 0 = essential
)

private data class ActivityRating(
    val icon: ImageVector,
    val title: String,
    val emoji: String,
    val rating: String, // "excellent", "good", "fair", "poor"
    val tip: String
)

private data class HealthAdvice(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val color: Color
)

// ─── Main Composable ──────────────────────────────────────────

@Composable
fun ClothingOutdoorSuggestionsCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier
) {
    val clothing = remember(weatherInfo) { generateClothingItems(weatherInfo) }
    val activities = remember(weatherInfo) { generateActivityRatings(weatherInfo) }
    val health = remember(weatherInfo) { generateHealthAdvice(weatherInfo) }
    val summaryText = remember(weatherInfo) { generateWeatherSummary(weatherInfo) }

    var expandedSection by remember { mutableStateOf<String?>("clothing") }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Checkroom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "What to Wear & Do",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Summary pill ──
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Clothing Section ──
            SectionToggle(
                title = "Clothing Essentials",
                icon = Icons.Rounded.Checkroom,
                isExpanded = expandedSection == "clothing",
                onClick = { expandedSection = if (expandedSection == "clothing") null else "clothing" }
            )
            AnimatedVisibility(visible = expandedSection == "clothing") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    clothing.forEach { item ->
                        ClothingChip(item)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Health Advice ──
            SectionToggle(
                title = "Health & Comfort",
                icon = Icons.Rounded.HealthAndSafety,
                isExpanded = expandedSection == "health",
                onClick = { expandedSection = if (expandedSection == "health") null else "health" }
            )
            AnimatedVisibility(visible = expandedSection == "health") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    health.forEach { advice ->
                        HealthAdviceRow(advice)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Activities ──
            SectionToggle(
                title = "Outdoor Activities",
                icon = Icons.Rounded.DirectionsWalk,
                isExpanded = expandedSection == "activities",
                onClick = { expandedSection = if (expandedSection == "activities") null else "activities" }
            )
            AnimatedVisibility(visible = expandedSection == "activities") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    activities.forEach { activity ->
                        ActivityRatingRow(activity)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ─── Section Toggle ───────────────────────────────────────────

@Composable
private fun SectionToggle(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Clothing Chip ────────────────────────────────────────────

@Composable
private fun ClothingChip(item: ClothingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(item.color.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with circle background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (item.priority == 0) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = item.color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Essential",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = item.color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ─── Health Advice Row ────────────────────────────────────────

@Composable
private fun HealthAdviceRow(advice: HealthAdvice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(advice.color.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            advice.icon,
            contentDescription = null,
            tint = advice.color,
            modifier = Modifier.size(20.dp).padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = advice.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = advice.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Activity Rating Row ──────────────────────────────────────

@Composable
private fun ActivityRatingRow(activity: ActivityRating) {
    val ratingColor = when (activity.rating) {
        "excellent" -> Color(0xFF22C55E)
        "good" -> Color(0xFF3B82F6)
        "fair" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            activity.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ratingColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${activity.emoji} ${activity.rating.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = activity.tip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

// ─── Logic: Weather Summary ───────────────────────────────────

private fun generateWeatherSummary(info: WeatherInfo): String {
    val current = info.current
    val temp = current.temperature
    val code = current.weatherCode
    val humidity = current.humidity ?: 50.0
    val wind = current.windSpeed ?: 0.0
    val feelsLike = current.apparentTemperature ?: temp

    val isRainy = code in 51..82
    val isStormy = code in 95..99
    val isSnowy = code in 71..77
    val isFoggy = code in 45..48
    val isClear = code in 0..2

    return when {
        isStormy -> "⚡ Thunderstorm active — stay indoors and avoid travel if possible."
        isSnowy -> "❄️ Snowing. Bundle up warmly and watch for slippery surfaces."
        isRainy -> "🌧️ Rain expected — waterproof layers and umbrella essential."
        isFoggy -> "🌫️ Foggy conditions — dress warmly and be cautious if driving."
        temp >= 40 -> "🔥 Extreme heat! Minimize outdoor exposure and hydrate constantly."
        temp >= 35 -> "🌡️ Very hot today. Wear minimal, breathable clothing and carry water."
        temp >= 30 -> "☀️ Hot and ${if (humidity > 70) "humid" else "sunny"}. Light, loose clothing recommended."
        temp >= 25 -> "🌤️ Warm ${if (isClear) "and clear" else ""}. Perfect for light clothing."
        temp >= 18 -> "🍃 Mild and pleasant. A light layer is enough."
        temp >= 10 -> "🧣 Cool — a jacket or sweater recommended."
        temp >= 0 -> "🧥 Cold — warm layers, coat, and accessories needed."
        else -> "🥶 Freezing! Heavy winter clothing essential — coat, gloves, scarf, hat."
    }
}

// ─── Logic: Clothing Items ────────────────────────────────────

private fun generateClothingItems(info: WeatherInfo): List<ClothingItem> {
    val items = mutableListOf<ClothingItem>()
    val temp = info.current.temperature
    val code = info.current.weatherCode
    val humidity = info.current.humidity ?: 50.0
    val wind = info.current.windSpeed ?: 0.0
    val uvIndex = info.daily.firstOrNull()?.uvIndex ?: 0.0
    val isRainy = code in 51..82
    val isStormy = code in 95..99
    val isSnowy = code in 71..77
    val isDay = info.current.isDay

    // ── Top ──
    when {
        temp >= 35 -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "Tank Top / Sleeveless",
            "Breathable cotton or moisture-wicking fabric",
            Color(0xFFF97316), 1
        ))
        temp >= 28 -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "T-Shirt or Light Top",
            if (humidity > 70) "Moisture-wicking fabric to handle humidity"
            else "Light cotton or linen",
            Color(0xFFF97316), 2
        ))
        temp >= 20 -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "T-Shirt or Light Shirt",
            "Comfortable for mild weather",
            Color(0xFF3B82F6), 2
        ))
        temp >= 10 -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "Long Sleeve / Light Sweater",
            "Layer a jacket on top for warmth",
            Color(0xFF3B82F6), 1
        ))
        temp >= 0 -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "Thermal Layer + Sweater",
            "Dress in layers for warmth",
            Color(0xFF6366F1), 1
        ))
        else -> items.add(ClothingItem(
            Icons.Rounded.Checkroom, "Heavy Sweater / Fleece",
            "Thermal base layer strongly recommended",
            Color(0xFF6366F1), 0
        ))
    }

    // ── Outer Layer ──
    when {
        isSnowy -> items.add(ClothingItem(
            Icons.Rounded.AcUnit, "Waterproof Winter Coat",
            "Insulated, windproof, and waterproof",
            Color(0xFF6366F1), 0
        ))
        isRainy && isStormy -> items.add(ClothingItem(
            Icons.Rounded.Thunderstorm, "Waterproof Rain Jacket",
            "Sealed seams recommended for heavy rain",
            Color(0xFF3B82F6), 0
        ))
        isRainy -> items.add(ClothingItem(
            Icons.Rounded.Umbrella, "Rain Jacket / Windbreaker",
            "Packable for easy carry",
            Color(0xFF3B82F6), 0
        ))
        temp < 5 -> items.add(ClothingItem(
            Icons.Rounded.AcUnit, "Heavy Winter Coat",
            "Insulated down or synthetic fill",
            Color(0xFF6366F1), 0
        ))
        temp < 12 -> items.add(ClothingItem(
            Icons.Rounded.AcUnit, "Medium Jacket or Coat",
            "Fleece-lined or wool blend",
            Color(0xFF6366F1), 1
        ))
        temp < 18 -> items.add(ClothingItem(
            Icons.Rounded.AcUnit, "Light Jacket or Cardigan",
            "Easy to remove if it warms up",
            Color(0xFF8B5CF6), 2
        ))
        wind > 30 -> items.add(ClothingItem(
            Icons.Rounded.Air, "Windbreaker",
            "Wind gusts up to ${wind.roundToInt()} km/h",
            Color(0xFF0EA5E9), 1
        ))
    }

    // ── Bottom ──
    when {
        temp >= 30 -> items.add(ClothingItem(
            Icons.Rounded.Straighten, "Shorts or Skirt",
            "Light, breathable fabric",
            Color(0xFFF97316), 2
        ))
        temp >= 15 -> items.add(ClothingItem(
            Icons.Rounded.Straighten, "Jeans or Light Trousers",
            "Comfortable all-day wear",
            Color(0xFF3B82F6), 2
        ))
        temp >= 0 -> items.add(ClothingItem(
            Icons.Rounded.Straighten, "Warm Trousers or Jeans",
            "Thermal leggings underneath in freezing temps",
            Color(0xFF6366F1), 1
        ))
        else -> items.add(ClothingItem(
            Icons.Rounded.Straighten, "Insulated Winter Pants",
            "Fleece-lined or snow pants for snow",
            Color(0xFF6366F1), 0
        ))
    }

    // ── Footwear ──
    when {
        isSnowy -> items.add(ClothingItem(
            Icons.Rounded.Hiking, "Insulated Waterproof Boots",
            "Non-slip sole for icy surfaces",
            Color(0xFF78716C), 0
        ))
        isRainy -> items.add(ClothingItem(
            Icons.Rounded.WaterDrop, "Waterproof Shoes / Boots",
            "Sealed seams to keep feet dry",
            Color(0xFF3B82F6), 0
        ))
        temp >= 25 -> items.add(ClothingItem(
            Icons.Rounded.Hiking, "Sandals or Breathable Shoes",
            "Open or mesh for ventilation",
            Color(0xFFF97316), 2
        ))
        temp < 5 -> items.add(ClothingItem(
            Icons.Rounded.Hiking, "Insulated Boots",
            "Warm lining for cold feet",
            Color(0xFF78716C), 1
        ))
        else -> items.add(ClothingItem(
            Icons.Rounded.Hiking, "Comfortable Walking Shoes",
            "Supportive and weather-appropriate",
            Color(0xFF78716C), 2
        ))
    }

    // ── Accessories ──
    when {
        uvIndex >= 6 && isDay -> items.add(ClothingItem(
            Icons.Rounded.WbSunny, "Sunglasses + Wide-Brim Hat",
            "UV Index ${uvIndex.roundToInt()} — protect eyes and scalp",
            Color(0xFFF59E0B), 0
        ))
        uvIndex >= 3 && isDay -> items.add(ClothingItem(
            Icons.Rounded.WbSunny, "Sunglasses Recommended",
            "Moderate UV — protect your eyes",
            Color(0xFFF59E0B), 2
        ))
    }
    when {
        temp < 0 -> {
            items.add(ClothingItem(
                Icons.Rounded.Park, "Scarf, Gloves & Beanie",
                "Protect extremities from frostbite",
                Color(0xFF6366F1), 0
            ))
            items.add(ClothingItem(
                Icons.Rounded.Shield, "Face Mask / Balaclava",
                "Wind chill protection in freezing temps",
                Color(0xFF6366F1), 0
            ))
        }
        temp < 10 -> items.add(ClothingItem(
            Icons.Rounded.Park, "Light Gloves & Beanie",
            "Keep extremities warm in cool weather",
            Color(0xFF8B5CF6), 1
        ))
    }
    when {
        isRainy -> items.add(ClothingItem(
            Icons.Rounded.Umbrella, "Compact Umbrella",
            "Keep one in your bag today",
            Color(0xFF3B82F6), 0
        ))
    }
    if (humidity > 80 && temp > 28) {
        items.add(ClothingItem(
            Icons.Rounded.WaterDrop, "Sweat-Resistant Deodorant",
            "High humidity — ${humidity.roundToInt()}% moisture in air",
            Color(0xFF0EA5E9), 1
        ))
    }

    return items.sortedBy { it.priority }
}

// ─── Logic: Activity Ratings ──────────────────────────────────

private fun generateActivityRatings(info: WeatherInfo): List<ActivityRating> {
    val suggestions = mutableListOf<ActivityRating>()
    val temp = info.current.temperature
    val code = info.current.weatherCode
    val wind = info.current.windSpeed ?: 0.0
    val uv = info.daily.firstOrNull()?.uvIndex ?: 0.0
    val humidity = info.current.humidity ?: 50.0
    val visibility = info.current.visibility ?: 10000.0
    val isRainy = code in 51..82
    val isStormy = code in 95..99
    val isSnowy = code in 71..77

    // Running
    val runningScore = when {
        isStormy || temp > 35 || temp < -5 -> "poor"
        isRainy || temp > 30 -> "fair"
        temp in 10.0..25.0 && !isRainy && wind < 25 -> "excellent"
        else -> "good"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.DirectionsRun, "Running",
        when (runningScore) { "excellent" -> "🏃"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        runningScore,
        when (runningScore) {
            "excellent" -> "Ideal running conditions — go for it!"
            "good" -> "Good for a run. Stay hydrated and pace yourself."
            "fair" -> "Possible but not ideal. Consider a shorter route."
            else -> "Skip outdoor running — try the gym instead."
        }
    ))

    // Cycling
    val cyclingScore = when {
        isStormy || wind > 40 -> "poor"
        isRainy -> "fair"
        temp in 10.0..30.0 && wind < 30 && visibility > 1000 -> "excellent"
        else -> "good"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.DirectionsBike, "Cycling",
        when (cyclingScore) { "excellent" -> "🚴"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        cyclingScore,
        when (cyclingScore) {
            "excellent" -> "Perfect cycling weather — clear roads and calm winds!"
            "good" -> "Good conditions. Bring a light layer just in case."
            "fair" -> "Wet roads possible. Ride carefully with reduced speed."
            else -> "Dangerous conditions for cycling. Postpone your ride."
        }
    ))

    // Hiking
    val hikingScore = when {
        isStormy || temp < -5 -> "poor"
        isRainy && temp < 10 -> "poor"
        isRainy -> "fair"
        isSnowy -> "fair"
        temp in 10.0..28.0 && !isRainy -> "excellent"
        else -> "good"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.Terrain, "Hiking",
        when (hikingScore) { "excellent" -> "⛰️"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        hikingScore,
        when (hikingScore) {
            "excellent" -> "Great hiking weather! Beautiful conditions on the trail."
            "good" -> "Good for hiking. Carry water and check trail conditions."
            "fair" -> "Trails may be slippery. Wear proper footwear."
            else -> "Not safe for hiking today. Try again when weather improves."
        }
    ))

    // Dog Walking
    val dogScore = when {
        isStormy || temp > 38 || temp < -10 -> "poor"
        isRainy -> "fair"
        temp in 5.0..30.0 && wind < 35 -> "excellent"
        else -> "good"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.Pets, "Dog Walking",
        when (dogScore) { "excellent" -> "🐕"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        dogScore,
        when (dogScore) {
            "excellent" -> "Perfect for a walk! Moderate temp, calm weather."
            "good" -> "Good walking weather. Bring water for your pup."
            "fair" -> "Keep it short. Watch for hot/cold ground on paw pads."
            else -> "Too extreme for pets today. Indoor play recommended."
        }
    ))

    // Photography
    val photoScore = when {
        isStormy -> "poor"
        code == 0 && temp > 5 && temp < 35 -> "excellent"
        code <= 3 -> "good"
        isRainy -> "fair"
        else -> "good"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.CameraAlt, "Photography",
        when (photoScore) { "excellent" -> "📸"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        photoScore,
        when (photoScore) {
            "excellent" -> "Beautiful light today — ideal for outdoor photography!"
            "good" -> "Good conditions. Clouds can add drama to photos."
            "fair" -> "Protect gear from moisture. Moody shots possible."
            else -> "Stormy conditions — gear risk. Stay indoors."
        }
    ))

    // Gardening
    val gardenScore = when {
        temp > 35 || temp < 2 -> "poor"
        isRainy && temp > 10 -> "excellent" // natural watering
        temp in 15.0..30.0 && !isRainy && humidity > 40 -> "good"
        else -> "fair"
    }
    suggestions.add(ActivityRating(
        Icons.Rounded.Grass, "Gardening",
        when (gardenScore) { "excellent" -> "🌱"; "good" -> "👍"; "fair" -> "⚠️"; else -> "❌" },
        gardenScore,
        when (gardenScore) {
            "excellent" -> "Rain will water your plants — great for transplanting!"
            "good" -> "Good for planting, pruning, or weeding."
            "fair" -> "Water in early morning. Avoid midday heat."
            else -> "Protect sensitive plants from extreme conditions."
        }
    ))

    return suggestions
}

// ─── Logic: Health Advice ─────────────────────────────────────

private fun generateHealthAdvice(info: WeatherInfo): List<HealthAdvice> {
    val advice = mutableListOf<HealthAdvice>()
    val temp = info.current.temperature
    val code = info.current.weatherCode
    val humidity = info.current.humidity ?: 50.0
    val wind = info.current.windSpeed ?: 0.0
    val uv = info.daily.firstOrNull()?.uvIndex ?: 0.0
    val feelsLike = info.current.apparentTemperature ?: temp
    val isDay = info.current.isDay

    // ── Heat Index / Wind Chill ──
    val heatIndex = calculateHeatIndex(temp, humidity)
    val windChill = calculateWindChill(temp, wind)

    if (heatIndex > 35) {
        advice.add(HealthAdvice(
            Icons.Rounded.LocalFireDepartment,
            "Heat Index ${heatIndex.roundToInt()}°",
            when {
                heatIndex >= 54 -> "⚠️ DANGEROUS! Risk of heat stroke. Avoid all outdoor activity."
                heatIndex >= 45 -> "Extreme heat danger! Stay hydrated — drink water every 15 min."
                else -> "Heat index makes it feel ${heatIndex.roundToInt()}°. Take breaks in shade."
            },
            Color(0xFFEF4444)
        ))
    }

    if (windChill < 0 && wind > 15) {
        advice.add(HealthAdvice(
            Icons.Rounded.AcUnit,
            "Wind Chill ${windChill.roundToInt()}°",
            "Wind makes it feel ${windChill.roundToInt()}°. Exposed skin can get frostbite in ${windChillToMinutes(windChill)} min.",
            Color(0xFF6366F1)
        ))
    }

    // ── UV Protection ──
    if (uv >= 3 && isDay) {
        val burnTime = uvToBurnMinutes(uv)
        advice.add(HealthAdvice(
            Icons.Rounded.WbSunny,
            "UV Index ${uv.roundToInt()} — ${uvCategory(uv)}",
            "Bare skin burns in ~$burnTime min. ${
                when {
                    uv >= 11 -> "Avoid sun 10AM-4PM. SPF 50+ essential."
                    uv >= 8 -> "SPF 30+ sunscreen. Reapply every 90 min."
                    uv >= 6 -> "SPF 30+ recommended. Wear a hat."
                    else -> "Sunscreen advised if outdoors > 30 min."
                }
            }",
            when {
                uv >= 11 -> Color(0xFF7C3AED)
                uv >= 8 -> Color(0xFFDC2626)
                uv >= 6 -> Color(0xFFF97316)
                else -> Color(0xFFF59E0B)
            }
        ))
    }

    // ── Hydration ──
    val dehydrationRisk = when {
        temp >= 38 && humidity > 60 -> "very high"
        temp >= 35 || (temp >= 30 && humidity > 70) -> "high"
        temp >= 28 && humidity > 60 -> "moderate"
        else -> null
    }
    if (dehydrationRisk != null) {
        advice.add(HealthAdvice(
            Icons.Rounded.WaterDrop,
            "Dehydration Risk: ${dehydrationRisk.replaceFirstChar { it.uppercase() }}",
            when (dehydrationRisk) {
                "very high" -> "Drink 250ml every 15 min. Avoid caffeine and alcohol."
                "high" -> "Drink water regularly — at least 2L throughout the day."
                else -> "Keep a water bottle handy. Drink before you feel thirsty."
            },
            when (dehydrationRisk) {
                "very high", "high" -> Color(0xFFEF4444)
                else -> Color(0xFF3B82F6)
            }
        ))
    }

    // ── Air Quality Notice (if very humid + hot) ──
    if (humidity > 85 && temp > 30) {
        advice.add(HealthAdvice(
            Icons.Rounded.Air,
            "High Humidity Warning",
            "Humidity at ${humidity.roundToInt()}% makes the air feel heavy. " +
                "Reduce strenuous activity and take frequent breaks.",
            Color(0xFF0EA5E9)
        ))
    }

    // ── Cold-related ──
    if (temp < 5 && humidity > 80) {
        advice.add(HealthAdvice(
            Icons.Rounded.AcUnit,
            "Damp Cold Alert",
            "Cold + damp air (${humidity.roundToInt()}%) penetrates clothing faster. " +
                "Wear waterproof outer layer to stay dry and warm.",
            Color(0xFF6366F1)
        ))
    }

    // ── General hydration reminder in heat ──
    if (temp >= 25 && isDay) {
        advice.add(HealthAdvice(
            Icons.Rounded.MonitorHeart,
            "Stay Active, Stay Healthy",
            when {
                temp >= 35 -> "Exercise before 8AM or after 6PM. Seek shade during midday."
                temp >= 28 -> "Great for outdoor exercise — just stay hydrated."
                else -> "Good conditions for outdoor activity. Enjoy the weather!"
            },
            when {
                temp >= 35 -> Color(0xFFF97316)
                else -> Color(0xFF22C55E)
            }
        ))
    }

    return advice.take(4) // Keep it concise
}

// ─── Utility Functions ────────────────────────────────────────

private fun calculateHeatIndex(temp: Double, humidity: Double): Double {
    // Simplified Rothfusz regression
    if (temp < 27) return temp
    val t = temp
    val h = humidity
    val hi = -8.7847 + 1.6114 * t + 2.3385 * h - 0.1461 * t * h -
            0.0068 * t * t - 0.0548 * h * h + 0.0012 * t * t * h +
            0.0009 * t * h * h
    return hi
}

private fun calculateWindChill(temp: Double, wind: Double): Double {
    // Wind chill formula (valid for temp < 10°C, wind > 4.8 km/h)
    if (temp > 10 || wind < 4.8) return temp
    return 13.12 + 0.6215 * temp - 11.37 * Math.pow(wind, 0.16) +
            0.3965 * temp * Math.pow(wind, 0.16)
}

private fun windChillToMinutes(windChill: Double): Int {
    return when {
        windChill < -40 -> 5
        windChill < -28 -> 10
        windChill < -20 -> 15
        windChill < -10 -> 30
        else -> 60
    }
}

private fun uvToBurnMinutes(uv: Double): Int {
    return when {
        uv >= 11 -> 10
        uv >= 8 -> 15
        uv >= 6 -> 25
        uv >= 3 -> 45
        else -> 80
    }
}

private fun uvCategory(uv: Double): String = when {
    uv >= 11 -> "Extreme"
    uv >= 8 -> "Very High"
    uv >= 6 -> "High"
    uv >= 3 -> "Moderate"
    uv >= 1 -> "Low"
    else -> "Minimal"
}
