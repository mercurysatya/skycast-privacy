package com.vayu.weather.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.domain.model.WeatherInfo
import kotlin.math.roundToInt

data class WeatherActivitySuggestion(
    val icon: ImageVector,
    val category: String,
    val title: String,
    val description: String,
    val rating: String, // "excellent", "good", "fair", "poor"
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun GardeningPetWeatherCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier
) {
    val suggestions = generateActivitySuggestions(weatherInfo)
    if (suggestions.isEmpty()) return

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
                text = "Outdoor Activities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                ActivitySuggestionRow(suggestion)
                if (suggestion != suggestions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivitySuggestionRow(suggestion: WeatherActivitySuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(suggestion.color.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = suggestion.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = suggestion.color
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RatingBadge(rating = suggestion.rating, color = suggestion.color)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RatingBadge(rating: String, color: androidx.compose.ui.graphics.Color) {
    val emoji = when (rating) {
        "excellent" -> "⭐"
        "good" -> "👍"
        "fair" -> "⚠️"
        else -> "❌"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$emoji ${rating.replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun generateActivitySuggestions(info: WeatherInfo): List<WeatherActivitySuggestion> {
    val suggestions = mutableListOf<WeatherActivitySuggestion>()
    val current = info.current
    val today = info.daily.firstOrNull()
    val temp = current.temperature
    val code = current.weatherCode
    val windSpeed = current.windSpeed ?: 0.0
    val uvIndex = today?.uvIndex ?: 0.0
    val humidity = current.humidity ?: 50.0
    val visibility = current.visibility ?: 10000.0
    val isRainy = code in 51..82 || code in 95..99
    val isStormy = code in 95..99

    // ── Dog Walking ──
    val dogRating = when {
        isStormy -> "poor"
        isRainy && temp < 5 -> "poor"
        isRainy -> "fair"
        temp > 35 || temp < 0 -> "poor"
        temp in 15.0..28.0 && windSpeed < 25 && uvIndex < 7 -> "excellent"
        temp in 5.0..35.0 -> "good"
        else -> "fair"
    }
    suggestions.add(WeatherActivitySuggestion(
        icon = Icons.Rounded.Pets,
        category = "pets",
        title = "Dog Walking",
        description = when (dogRating) {
            "excellent" -> "Perfect conditions for a walk! Moderate temp, calm winds."
            "good" -> "Good walking weather. Bring water if it's warm."
            "fair" -> "Fair conditions. Short walk recommended."
            else -> "Not ideal for walking. Consider indoor play today."
        },
        rating = dogRating,
        color = MaterialTheme.colorScheme.tertiary
    ))

    // ── Gardening ──
    val gardenRating = when {
        isRainy && temp > 10 -> "excellent" // Natural watering!
        temp in 15.0..30.0 && !isRainy && humidity > 40 -> "good"
        temp > 35 -> "poor" // Heat stress on plants
        temp < 2 -> "poor" // Frost risk
        code in 45..48 -> "fair" // Fog is okay for some tasks
        else -> "fair"
    }
    suggestions.add(WeatherActivitySuggestion(
        icon = Icons.Rounded.Grass,
        category = "gardening",
        title = "Gardening",
        description = when (gardenRating) {
            "excellent" -> "Great gardening day! Rain will water your plants naturally."
            "good" -> "Good conditions for planting, pruning, or weeding."
            "fair" -> "Fair for gardening. Water plants in early morning."
            else -> "Protect sensitive plants. Avoid outdoor gardening today."
        },
        rating = gardenRating,
        color = MaterialTheme.colorScheme.secondary
    ))

    // ── Outdoor Exercise ──
    val exerciseRating = when {
        isStormy -> "poor"
        temp > 33 -> "poor"
        temp in 15.0..28.0 && !isRainy && windSpeed < 30 -> "excellent"
        temp in 10.0..33.0 && !isRainy -> "good"
        isRainy -> "fair"
        temp < 5 -> "poor"
        else -> "fair"
    }
    suggestions.add(WeatherActivitySuggestion(
        icon = Icons.Rounded.DirectionsRun,
        category = "exercise",
        title = "Outdoor Exercise",
        description = when (exerciseRating) {
            "excellent" -> "Ideal for running, cycling, or outdoor workouts!"
            "good" -> "Good conditions. Stay hydrated and wear appropriate gear."
            "fair" -> "Fair for exercise. Consider a shorter session."
            else -> "Indoor exercise recommended today."
        },
        rating = exerciseRating,
        color = MaterialTheme.colorScheme.primary
    ))

    // ── Car Wash ──
    val carWashRating = when {
        isRainy -> "poor" // Don't wash if rain is coming
        isStormy -> "poor"
        code in 51..65 -> "poor"
        temp > 10 && !isRainy -> "excellent" // Dry conditions
        else -> "good"
    }
    suggestions.add(WeatherActivitySuggestion(
        icon = Icons.Rounded.DirectionsCar,
        category = "car",
        title = "Car Wash",
        description = when (carWashRating) {
            "excellent" -> "Perfect day to wash your car — dry conditions ahead."
            "good" -> "Good conditions for a car wash."
            "fair" -> "Check forecast before washing."
            else -> "Rain expected — skip the car wash today."
        },
        rating = carWashRating,
        color = MaterialTheme.colorScheme.primary
    ))

    // ── Stargazing (night only) ──
    if (!current.isDay && !isRainy && code !in 45..48) {
        val starRating = when {
            code == 0 && humidity < 60 -> "excellent"
            code <= 2 -> "good"
            else -> "fair"
        }
        suggestions.add(WeatherActivitySuggestion(
            icon = Icons.Rounded.NightsStay,
            category = "stargazing",
            title = "Stargazing",
            description = when (starRating) {
                "excellent" -> "Clear skies! Excellent visibility for stargazing tonight."
                "good" -> "Decent conditions. Some clouds may partially obscure view."
                else -> "Cloudy. Limited stargazing visibility."
            },
            rating = starRating,
            color = MaterialTheme.colorScheme.tertiary
        ))
    }

    return suggestions
}
