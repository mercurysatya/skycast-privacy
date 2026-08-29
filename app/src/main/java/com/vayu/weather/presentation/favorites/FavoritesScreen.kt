package com.vayu.weather.presentation.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vayu.weather.R
import com.vayu.weather.domain.model.City
import com.vayu.weather.presentation.favorites.FavoritesEmptyStateV2
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FavoritesScreen(
    state: FavoritesState,
    onCitySelected: (City) -> Unit,
    onRemoveFavorite: (City) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onBrowseCities: () -> Unit = {},
    onOpenCompare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val contentDescription = stringResource(R.string.favorites)

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
            .semantics { this.contentDescription = contentDescription }
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
            if (state.favorites.size >= 2) {
                androidx.compose.material3.TextButton(onClick = onOpenCompare) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare")
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.favorites_long_press_reorder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.favorites.isEmpty()) {
            FavoritesEmptyStateV2(
                onBrowseClick = onBrowseCities
            )
        } else {
            DraggableFavoritesList(
                favorites = state.favorites,
                onCitySelected = onCitySelected,
                onRemoveFavorite = onRemoveFavorite,
                onReorder = onReorder,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DraggableFavoritesList(
    favorites: List<City>,
    onCitySelected: (City) -> Unit,
    onRemoveFavorite: (City) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(favorites, key = { _, city -> city.id }) { index, city ->
            val isDragged = draggedIndex == index
            val isTarget = targetIndex == index && draggedIndex != null

            val elevation by animateColorAsState(
                targetValue = if (isDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else if (isTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                label = "fav_elevation"
            )

            FavoriteCityCard(
                city = city,
                onClick = { onCitySelected(city) },
                onRemove = { onRemoveFavorite(city) },
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        scaleX = if (isDragged) 1.03f else 1f
                        scaleY = if (isDragged) 1.03f else 1f
                        translationY = if (isDragged) 4f else 0f
                    }
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                scope.launch {
                                    delay(50)
                                }
                            },
                            onDragCancel = {
                                draggedIndex = null
                                targetIndex = null
                            },
                            onDragEnd = {
                                draggedIndex?.let { from ->
                                    targetIndex?.let { to ->
                                        if (from != to) {
                                            onReorder(from, to)
                                        }
                                    }
                                }
                                draggedIndex = null
                                targetIndex = null
                            },
                            onDrag = { _, dragAmount ->
                                // Estimate target from vertical drag
                                draggedIndex?.let { from ->
                                    val estimatedDelta = (dragAmount.y / 72f).toInt()
                                    val newTarget = (from + estimatedDelta).coerceIn(0, favorites.lastIndex)
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

@Composable
private fun FavoriteCityCard(
    city: City,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = stringResource(R.string.drag_to_reorder),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = stringResource(R.string.current_location),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).widthIn(min = 1.dp)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    append(city.admin1 ?: "")
                    if (city.admin1 != null && city.country != null) append(", ")
                    append(city.country ?: "")
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

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onRemove,
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Remove ${city.name} from favorites"
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete_alert),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
