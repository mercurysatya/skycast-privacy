package com.vayu.weather.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vayu.weather.R
import com.vayu.weather.presentation.weather.TemperatureUnit

/**
 * SkyCast premium favorites screen.
 *
 * Renders rich [SkyCastFavoriteCard]s for each saved city, with current
 * weather (loaded by [FavoritesWithWeatherViewModel]), H/L, rain
 * probability, wind, UV, and an alert badge for cities with active
 * severe weather.
 */
@Composable
fun SkyCastFavoritesScreen(
    favorites: List<FavoriteWithWeather>,
    onCitySelected: (com.vayu.weather.domain.model.City) -> Unit,
    onRemoveFavorite: (com.vayu.weather.domain.model.City) -> Unit,
    onBrowseCities: () -> Unit,
    onCompare: () -> Unit = {},
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.favorites),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (favorites.size >= 2) {
                FilledTonalButton(
                    onClick = onCompare,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CompareArrows, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }
            }
        }
        if (favorites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.favorites_long_press_reorder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (favorites.isEmpty()) {
            FavoritesEmptyStateV2(onBrowseClick = onBrowseCities)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(favorites, key = { it.city.id }, contentType = { "favoriteCard" }) { f ->
                    SkyCastFavoriteCard(
                        cityName = f.city.name,
                        region = f.city.admin1,
                        country = f.city.country,
                        weather = f.weather,
                        isCelsius = isCelsius,
                        isLoading = f.isLoading,
                        hasAlert = false,
                        onClick = { onCitySelected(f.city) },
                        onRemove = { onRemoveFavorite(f.city) }
                    )
                }
            }
        }
    }
}
