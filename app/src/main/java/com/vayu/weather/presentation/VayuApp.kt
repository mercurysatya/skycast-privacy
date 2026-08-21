package com.vayu.weather.presentation

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vayu.weather.presentation.ads.AdManager
import com.vayu.weather.R
import com.vayu.weather.presentation.alerts.AlertsScreen
import com.vayu.weather.presentation.alerts.AlertsViewModel
import com.vayu.weather.presentation.favorites.FavoritesScreen
import com.vayu.weather.presentation.favorites.FavoritesViewModel
import com.vayu.weather.presentation.map.WeatherMapScreen
import com.vayu.weather.presentation.search.SearchScreen
import com.vayu.weather.presentation.search.SearchViewModel
import com.vayu.weather.presentation.settings.PrivacyPolicyScreen
import com.vayu.weather.presentation.settings.SettingsScreen
import com.vayu.weather.presentation.settings.SettingsViewModel
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WeatherDashboard
import com.vayu.weather.presentation.weather.WeatherDetailScreen
import com.vayu.weather.presentation.weather.WeatherShareFormatter
import com.vayu.weather.presentation.weather.WeatherViewModel
import com.vayu.weather.ui.theme.SkyCastTheme
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun VayuApp() {
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFF38BDF8),
        selectedTextColor = Color(0xFF38BDF8),
        indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.14f),
        unselectedIconColor = Color(0xFF64748B),
        unselectedTextColor = Color(0xFF64748B)
    )
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = Color(0xFF38BDF8),
        unselectedIconColor = Color(0xFF64748B),
        indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.14f)
    )
    Log.d("VayuApp", "VayuApp Composing")
    val weatherViewModel: WeatherViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? Activity

    val settingsState by settingsViewModel.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var privacyPolicyAnchor by remember { mutableStateOf<String?>(null) }

    val navigator = rememberListDetailPaneScaffoldNavigator()
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("Weather") }
    var mapNavCount by remember { mutableStateOf(0) }

    BackHandler(enabled = showPrivacyPolicy) {
        showPrivacyPolicy = false
        privacyPolicyAnchor = null
    }

    BackHandler(enabled = showDetail && !showAlerts && !showSettings && !showPrivacyPolicy) {
        showDetail = false
    }

    BackHandler(enabled = showAlerts && !showSettings && !showPrivacyPolicy) {
        showAlerts = false
    }

    BackHandler(enabled = showSettings && !showPrivacyPolicy) {
        showSettings = false
    }

    BackHandler(enabled = !showSettings && navigator.canNavigateBack()) {
        scope.launch {
            Log.d("VayuApp", "Back pressed, navigating back")
            navigator.navigateBack()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("VayuApp", "LaunchedEffect: Loading weather and ad")
        weatherViewModel.loadWeatherInfo()
        AdManager.loadInterstitial(context)
    }

    val isDarkTheme = when (settingsState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SkyCastTheme(darkTheme = isDarkTheme, dynamicColor = settingsState.useDynamicColor) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showPrivacyPolicy) {
                PrivacyPolicyScreen(
                    anchor = privacyPolicyAnchor,
                    onBack = {
                        showPrivacyPolicy = false
                        privacyPolicyAnchor = null
                    }
                )
            } else if (showDetail) {
                WeatherDetailScreen(
                    state = weatherViewModel.state,
                    settings = settingsState,
                    cityName = weatherViewModel.currentCityName,
                    onBack = { showDetail = false }
                )
            } else if (showAlerts) {
                AlertsScreen(
                    state = alertsViewModel.state,
                    onDeleteAlert = alertsViewModel::deleteAlert,
                    onClearAll = alertsViewModel::clearAllAlerts,
                    onFilterChange = alertsViewModel::setSeverityFilter,
                    onToggleExpand = alertsViewModel::toggleExpandAlert
                )
            } else if (showSettings) {
                SettingsScreen(
                    state = settingsState,
                    onToggleUnit = settingsViewModel::toggleTemperatureUnit,
                    onWindUnitChange = settingsViewModel::setWindUnit,
                    onThemeModeChange = settingsViewModel::setThemeMode,
                    onToggleDynamicColor = settingsViewModel::setUseDynamicColor,
                    onToggleNotifications = settingsViewModel::setNotificationsEnabled,
                    onRainAlertThresholdChange = settingsViewModel::setRainAlertThreshold,
                    onCheckIntervalChange = settingsViewModel::setCheckIntervalHours,
                    onSeverityFilterChange = settingsViewModel::setSeverityFilter,
                    onWidgetSizeChange = settingsViewModel::setWidgetSize,
                    onWindAlertThresholdChange = settingsViewModel::setWindAlertThreshold,
                    onEnableWindAlertsChange = settingsViewModel::setEnableWindAlerts,
                    onUvAlertThresholdChange = settingsViewModel::setUvAlertThreshold,
                    onEnableUvAlertsChange = settingsViewModel::setEnableUvAlerts,
                    onHeatAlertThresholdChange = settingsViewModel::setHeatAlertThreshold,
                    onEnableHeatAlertsChange = settingsViewModel::setEnableHeatAlerts,
                    onColdAlertThresholdChange = settingsViewModel::setColdAlertThreshold,
                    onEnableColdAlertsChange = settingsViewModel::setEnableColdAlerts,
                    onBack = { showSettings = false },
                    onOpenPrivacyPolicy = { anchor ->
                        privacyPolicyAnchor = anchor
                        showPrivacyPolicy = true
                    },
                    onDeleteAllData = { settingsViewModel.deleteAllData() },
                    onClearCache = { settingsViewModel.clearWeatherCache() },
                    onOpenAlerts = { showAlerts = true }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (navigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    tonalElevation = 3.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "Weather",
                                        onClick = {
                                            currentRoute = "Weather"
                                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Weather") }
                                        },
                                        icon = { Icon(Icons.Rounded.Cloud, "Weather", Modifier.size(24.dp)) },
                                        label = { Text("Weather") },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "Search",
                                        onClick = {
                                            currentRoute = "Search"
                                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.List, "Search") }
                                        },
                                        icon = { Icon(Icons.Rounded.Search, "Search", Modifier.size(24.dp)) },
                                        label = { Text("Search") },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "Favorites",
                                        onClick = {
                                            currentRoute = "Favorites"
                                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.List, "Favorites") }
                                        },
                                        icon = { Icon(Icons.Rounded.Favorite, "Favorites", Modifier.size(24.dp)) },
                                        label = { Text("Favorites") },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "Map",
                                        onClick = {
                                            currentRoute = "Map"
                                            mapNavCount++
                                            scope.launch {
                                                try {
                                                    if (activity != null && mapNavCount % 3 == 0) {
                                                        AdManager.showInterstitial(activity) {
                                                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map") }
                                                        }
                                                    } else {
                                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("VayuApp", "Nav err", e)
                                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map") }
                                                }
                                            }
                                        },
                                        icon = { Icon(Icons.Rounded.Map, "Map", Modifier.size(24.dp)) },
                                        label = { Text("Map") },
                                        colors = navColors
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Row(modifier = Modifier.padding(padding)) {
                        if (navigator.scaffoldDirective.maxHorizontalPartitions > 1) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                NavigationRailItem(
                                    selected = currentRoute == "Weather",
                                    onClick = {
                                        currentRoute = "Weather"
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Weather") }
                                    },
                                    icon = { Icon(Icons.Rounded.Cloud, "Weather") },
                                    label = { Text("Weather") },
                                    colors = railColors
                                )
                                NavigationRailItem(
                                    selected = currentRoute == "Search",
                                    onClick = {
                                        currentRoute = "Search"
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.List, "Search") }
                                    },
                                    icon = { Icon(Icons.Rounded.Search, "Search") },
                                    label = { Text("Search") },
                                    colors = railColors
                                )
                                NavigationRailItem(
                                    selected = currentRoute == "Favorites",
                                    onClick = {
                                        currentRoute = "Favorites"
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.List, "Favorites") }
                                    },
                                    icon = { Icon(Icons.Rounded.Favorite, "Favorites") },
                                    label = { Text("Favorites") },
                                    colors = railColors
                                )
                                NavigationRailItem(
                                    selected = currentRoute == "Map",
                                    onClick = {
                                        currentRoute = "Map"
                                        mapNavCount++
                                        scope.launch {
                                            try {
                                                if (activity != null && mapNavCount % 3 == 0) {
                                                    AdManager.showInterstitial(activity) {
                                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map") }
                                                    }
                                                } else {
                                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map")
                                                }
                                            } catch (e: Exception) {
                                                scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "Map") }
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Rounded.Map, "Map") },
                                    label = { Text("Map") },
                                    colors = railColors
                                )
                            }
                        }

                        ListDetailPaneScaffold(
                            directive = navigator.scaffoldDirective,
                            value = navigator.scaffoldValue,
                            listPane = {
                                val contentKey = navigator.currentDestination?.contentKey
                                when (contentKey) {
                                    "Favorites" -> {
                                        FavoritesScreen(
                                            state = favoritesViewModel.state,
                                            onCitySelected = { city ->
                                                weatherViewModel.loadWeatherForCity(
                                                    city.latitude, city.longitude, city.name
                                                )
                                                scope.launch {
                                                    try {
                                                        navigator.navigateTo(
                                                            ListDetailPaneScaffoldRole.Detail, "Weather"
                                                        )
                                                    } catch (e: Exception) { Log.e("VayuApp", "Nav err", e) }
                                                }
                                            },
                                            onRemoveFavorite = favoritesViewModel::removeFavorite
                                        )
                                    }
                                    else -> {
                                        SearchScreen(
                                            state = searchViewModel.state,
                                            onQueryChange = searchViewModel::onQueryChange,
                                            onCitySelected = { city ->
                                                searchViewModel.addToRecentSearches(city)
                                                weatherViewModel.loadWeatherForCity(
                                                    city.latitude, city.longitude, city.name
                                                )
                                                scope.launch {
                                                    try {
                                                        navigator.navigateTo(
                                                            ListDetailPaneScaffoldRole.Detail, "Weather"
                                                        )
                                                    } catch (e: Exception) { Log.e("VayuApp", "Nav err", e) }
                                                }
                                            },
                                            onToggleFavorite = { city ->
                                                favoritesViewModel.toggleFavorite(city)
                                            },
                                            isFavorite = { cityId ->
                                                favoritesViewModel.state.favorites.any { it.id == cityId }
                                            },
                                            onClearRecentSearches = searchViewModel::clearRecentSearches
                                        )
                                    }
                                }
                            },
                            detailPane = {
                                val contentKey = navigator.currentDestination?.contentKey
                                when (contentKey) {
                                    "Map" -> {
                                        WeatherMapScreen(
                                            locationTracker = weatherViewModel.locationTracker
                                        )
                                    }
                                    "Weather", null -> {
                                        WeatherDashboard(
                                            state = weatherViewModel.state,
                                            settings = settingsState,
                                            onRetry = { weatherViewModel.loadWeatherInfo() },
                                            onRefresh = { weatherViewModel.refreshWeatherInfo() },
                                            onToggleUnit = settingsViewModel::toggleTemperatureUnit,
                                            onOpenSettings = { showSettings = true },
                                            onOpenAlerts = { showAlerts = true },
                                            onOpenDetail = { showDetail = true },
                                            onShare = {
                                                val weatherInfo = weatherViewModel.state.weatherInfo
                                                if (weatherInfo != null) {
                                                    val shareText = WeatherShareFormatter.formatForShare(
                                                        context = context,
                                                        cityName = weatherViewModel.currentCityName,
                                                        weatherInfo = weatherInfo,
                                                        isCelsius = settingsState.temperatureUnit == TemperatureUnit.CELSIUS
                                                    )
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_weather_title)))
                                                }
                                            },
                                            onDismissRefreshError = { weatherViewModel.clearRefreshError() },
                                            cityName = weatherViewModel.currentCityName
                                        )
                                    }
                                    else -> {
                                        Box(modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
