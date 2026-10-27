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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vayu.weather.R
import com.vayu.weather.presentation.weather.TemperatureUnit
import kotlinx.coroutines.launch

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
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
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
            DraggableFavoritesList(
                favorites = favorites,
                onReorder = onReorder,
                onCitySelected = onCitySelected,
                onRemoveFavorite = onRemoveFavorite,
                isCelsius = isCelsius
            )
        }
    }
}

/**
 * Long-press drag to reorder. On drag end the committed order is handed to
 * [onReorder] (from, to), which forwards to the ViewModel and persists the
 * new ordering — the "Long press to reorder" hint above the list is real.
 */
@Composable
private fun DraggableFavoritesList(
    favorites: List<FavoriteWithWeather>,
    onReorder: (Int, Int) -> Unit,
    onCitySelected: (com.vayu.weather.domain.model.City) -> Unit,
    onRemoveFavorite: (com.vayu.weather.domain.model.City) -> Unit,
    isCelsius: Boolean
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var draggedItemHeightPx by remember { mutableIntStateOf(0) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(favorites, key = { _, f -> f.city.id }) { index, f ->
            val isDragged = draggedIndex == index
            val isTarget = targetIndex == index && draggedIndex != null

            SkyCastFavoriteCard(
                cityName = f.city.name,
                region = f.city.admin1,
                country = f.city.country,
                weather = f.weather,
                isCelsius = isCelsius,
                isLoading = f.isLoading,
                hasAlert = false,
                onClick = { onCitySelected(f.city) },
                onRemove = { onRemoveFavorite(f.city) },
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .onSizeChanged { draggedItemHeightPx = it.height }
                    .graphicsLayer {
                        scaleX = if (isDragged) 1.03f else 1f
                        scaleY = if (isDragged) 1.03f else 1f
                        translationY = if (isDragged) 4f else 0f
                    }
                    .graphicsLayer { alpha = if (isTarget) 0.55f else 1f }
                    .pointerInput(favorites.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                accumulatedDragY = 0f
                                draggedIndex = index
                            },
                            onDragCancel = {
                                draggedIndex = null
                                targetIndex = null
                            },
                            onDragEnd = {
                                val from = draggedIndex
                                val to = targetIndex
                                if (from != null && to != null && from != to) {
                                    onReorder(from, to)
                                }
                                draggedIndex = null
                                targetIndex = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggedIndex?.let { from ->
                                    accumulatedDragY += dragAmount.y
                                    val itemHeight = draggedItemHeightPx
                                        .takeIf { it > 0 }
                                        ?: (72.dp.toPx()).toInt()
                                    val steps = (accumulatedDragY / itemHeight).toInt()
                                    val newTarget = (from + steps)
                                        .coerceIn(0, favorites.lastIndex)
                                    if (newTarget != targetIndex) {
                                        targetIndex = newTarget
                                    }
                                }
                            }
                        )
                    }
            )
        }
    }
}
