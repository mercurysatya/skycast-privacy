package com.vayu.weather.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vayu.weather.R
import com.vayu.weather.domain.model.City
import com.vayu.weather.presentation.ads.AdBanner

@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onToggleFavorite: ((City) -> Unit)? = null,
    isFavorite: ((Long) -> Boolean)? = null,
    onClearRecentSearches: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.search_cities),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.discover_weather),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_for_city)) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        // Search results
        else if (state.query.isNotBlank() && state.results.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.results) { city ->
                    CityItem(
                        city = city,
                        onClick = { onCitySelected(city) },
                        isFavorite = isFavorite?.invoke(city.id) == true,
                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(city) } }
                    )
                }
            }
        }
        // Empty state
        else if (state.query.isBlank()) {
            EmptySearchContent(
                recentSearches = state.recentSearches,
                onCitySelected = onCitySelected,
                onQueryChange = onQueryChange,
                onClearRecentSearches = onClearRecentSearches,
                modifier = Modifier.weight(1f)
            )
        }
        // No results
        else if (state.query.isNotBlank() && state.results.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_cities_found, state.query),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Error message
        state.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        AdBanner()
    }
}

@Composable
private fun EmptySearchContent(
    recentSearches: List<City>,
    onCitySelected: (City) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearRecentSearches: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Description
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.discover_weather),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recent searches
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_searches),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClearRecentSearches, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = stringResource(R.string.clear_all),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            recentSearches.forEach { city ->
                CityItem(
                    city = city,
                    onClick = { onCitySelected(city) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Popular cities
        Text(
            text = stringResource(R.string.popular_cities),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        val popularCities = listOf(
            City(id = 2643743, name = stringResource(R.string.city_london), latitude = 51.5085, longitude = -0.1257, country = stringResource(R.string.country_uk), countryCode = "GB"),
            City(id = 5128581, name = stringResource(R.string.city_new_york), latitude = 40.7143, longitude = -74.006, country = stringResource(R.string.country_us), countryCode = "US"),
            City(id = 1850147, name = stringResource(R.string.city_tokyo), latitude = 35.6895, longitude = 139.6917, country = stringResource(R.string.country_japan), countryCode = "JP"),
            City(id = 2988507, name = stringResource(R.string.city_paris), latitude = 48.8534, longitude = 2.3488, country = stringResource(R.string.country_france), countryCode = "FR"),
            City(id = 292223, name = stringResource(R.string.city_dubai), latitude = 25.2582, longitude = 55.2719, country = stringResource(R.string.country_uae), countryCode = "AE"),
            City(id = 2147714, name = stringResource(R.string.city_sydney), latitude = -33.8679, longitude = 151.2073, country = stringResource(R.string.country_australia), countryCode = "AU"),
            City(id = 1880252, name = stringResource(R.string.city_singapore), latitude = 1.3521, longitude = 103.8198, country = stringResource(R.string.country_singapore), countryCode = "SG"),
            City(id = 1275339, name = stringResource(R.string.city_mumbai), latitude = 19.0728, longitude = 72.8797, country = stringResource(R.string.country_india), countryCode = "IN")
        )

        popularCities.forEach { city ->
            CityItem(
                city = city,
                onClick = { onCitySelected(city) }
            )
        }
    }
}

@Composable
private fun CityItem(
    city: City,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    if (!city.admin1.isNullOrBlank()) append(city.admin1)
                    if (!city.country.isNullOrBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(city.country)
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites, city.name) else stringResource(R.string.add_to_favorites, city.name),
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
