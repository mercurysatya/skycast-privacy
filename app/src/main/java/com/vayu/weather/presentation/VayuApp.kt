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
import com.vayu.weather.presentation.onboarding.OnboardingScreen
import com.vayu.weather.ui.theme.SkyBlue
import com.vayu.weather.ui.theme.StormGray
import com.vayu.weather.presentation.history.WeatherHistoryScreen
import com.vayu.weather.presentation.history.WeatherHistoryViewModel
import com.vayu.weather.presentation.weather.WeatherCardRenderer
import com.vayu.weather.presentation.weather.WeatherShareFormatter
import com.vayu.weather.presentation.weather.WeatherViewModel
import com.vayu.weather.ui.theme.SkyCastTheme
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VayuApp() {
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = SkyBlue,
        selectedTextColor = SkyBlue,
        indicatorColor = SkyBlue.copy(alpha = 0.14f),
        unselectedIconColor = StormGray,
        unselectedTextColor = StormGray
    )
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = SkyBlue,
        unselectedIconColor = StormGray,
        indicatorColor = SkyBlue.copy(alpha = 0.14f)
    )
    Log.d("VayuApp", "VayuApp Composing")
    val weatherViewModel: WeatherViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    val historyViewModel: WeatherHistoryViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? Activity

    val settingsState by settingsViewModel.state.collectAsState()
    var showOnboarding by remember { mutableStateOf(true) } // Will be set to false after check
    var onboardingChecked by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var privacyPolicyAnchor by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("Weather") }

    BackHandler(enabled = showPrivacyPolicy) {
        showPrivacyPolicy = false
        privacyPolicyAnchor = null
    }

    BackHandler(enabled = showDetail && !showAlerts && !showSettings && !showHistory && !showPrivacyPolicy) {
        showDetail = false
    }

    BackHandler(enabled = showHistory && !showAlerts && !showSettings && !showPrivacyPolicy) {
        showHistory = false
    }

    BackHandler(enabled = showAlerts && !showSettings && !showHistory && !showPrivacyPolicy) {
        showAlerts = false
    }

    BackHandler(enabled = showSettings && !showHistory && !showPrivacyPolicy) {
        showSettings = false
    }

    LaunchedEffect(Unit) {
        Log.d("VayuApp", "LaunchedEffect: Loading weather and ad")
        // First frame is ready — let the splash screen go. The dashboard is the
        // default tab, so there is no extra navigation to do on cold start.
        SplashGate.isReady = true
        weatherViewModel.loadWeatherInfo()
        // Gather UMP consent first; initialize/load ads only when allowed
        val act = activity
        if (act != null) {
            ConsentManager.gatherConsent(act) {
                if (ConsentManager.canRequestAds(act)) {
                    AdManager.initializeMobileAds(act)
                    AdManager.loadInterstitial(act)
                    AdManager.loadRewardedAd(act)
                }
            }
        }
        // Check if onboarding is complete
        onboardingChecked = true
        showOnboarding = !settingsViewModel.isOnboardingComplete()
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
            if (onboardingChecked && showOnboarding) {
                OnboardingScreen(
                    onComplete = {
                        showOnboarding = false
                        settingsViewModel.setOnboardingComplete()
                    }
                )
            } else if (showPrivacyPolicy) {
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
            } else if (showHistory) {
                WeatherHistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { showHistory = false }
                )
            } else if (showAlerts) {
                AlertsScreen(
                    state = alertsViewModel.state,
                    onDeleteAlert = alertsViewModel::deleteAlert,
                    onClearAll = alertsViewModel::clearAllAlerts,
                    onFilterChange = alertsViewModel::setSeverityFilter,
                    onWeatherTypeFilterChange = alertsViewModel::setWeatherTypeFilter,
                    onToggleExpand = alertsViewModel::toggleExpandAlert,
                    onSnoozeAlert = { alert, duration -> alertsViewModel.snoozeAlert(alert, duration) },
                    onSnoozeDurationChange = alertsViewModel::updateSnoozeDuration
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
                    onUse24hClockChange = settingsViewModel::setUse24hClock,
                    onPressureUnitChange = settingsViewModel::setPressureUnit,
                    onPrecipitationUnitChange = settingsViewModel::setPrecipitationUnit,
                    onSectionVisibilityChange = { section, visible -> settingsViewModel.setSectionVisibility(section, visible) },
                    // Quiet hours
                    onQuietHoursEnabledChange = settingsViewModel::setQuietHoursEnabled,
                    onQuietHoursStartHourChange = settingsViewModel::setQuietHoursStartHour,
                    onQuietHoursStartMinuteChange = settingsViewModel::setQuietHoursStartMinute,
                    onQuietHoursEndHourChange = settingsViewModel::setQuietHoursEndHour,
                    onQuietHoursEndMinuteChange = settingsViewModel::setQuietHoursEndMinute,
                    // Per-day notification times
                    onNotificationTime1EnabledChange = settingsViewModel::setNotificationTime1Enabled,
                    onNotificationTime1HourChange = settingsViewModel::setNotificationTime1Hour,
                    onNotificationTime1MinuteChange = settingsViewModel::setNotificationTime1Minute,
                    onNotificationTime2EnabledChange = settingsViewModel::setNotificationTime2Enabled,
                    onNotificationTime2HourChange = settingsViewModel::setNotificationTime2Hour,
                    onNotificationTime2MinuteChange = settingsViewModel::setNotificationTime2Minute,
                    onNotificationTime3EnabledChange = settingsViewModel::setNotificationTime3Enabled,
                    onNotificationTime3HourChange = settingsViewModel::setNotificationTime3Hour,
                    onNotificationTime3MinuteChange = settingsViewModel::setNotificationTime3Minute,
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 3.dp
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "Weather",
                                onClick = { currentRoute = "Weather" },
                                icon = {
                                    if (alertsViewModel.state.alerts.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(alertsViewModel.state.alerts.size.toString()) }
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Cloud, "Weather", Modifier.size(24.dp))
                                        }
                                    } else {
                                        Icon(Icons.Rounded.Cloud, "Weather", Modifier.size(24.dp))
                                    }
                                },
                                label = { Text("Weather") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentRoute == "Search",
                                onClick = { currentRoute = "Search" },
                                icon = { Icon(Icons.Rounded.Search, "Search", Modifier.size(24.dp)) },
                                label = { Text("Search") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentRoute == "Favorites",
                                onClick = { currentRoute = "Favorites" },
                                icon = { Icon(Icons.Rounded.Favorite, "Favorites", Modifier.size(24.dp)) },
                                label = { Text("Favorites") },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentRoute == "Map",
                                onClick = { currentRoute = "Map" },
                                icon = { Icon(Icons.Rounded.Map, "Map", Modifier.size(24.dp)) },
                                label = { Text("Map") },
                                colors = navColors
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when (currentRoute) {
                            "Search" -> {
                                SearchScreen(
                                    state = searchViewModel.state,
                                    onQueryChange = searchViewModel::onQueryChange,
                                    onCitySelected = { city ->
                                        searchViewModel.addToRecentSearches(city)
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name
                                        )
                                        currentRoute = "Weather"
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
                            "Favorites" -> {
                                FavoritesScreen(
                                    state = favoritesViewModel.state,
                                    onCitySelected = { city ->
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name
                                        )
                                        currentRoute = "Weather"
                                    },
                                    onRemoveFavorite = favoritesViewModel::removeFavorite,
                                    onReorder = favoritesViewModel::reorderFavorites
                                )
                            }
                            "Map" -> {
                                WeatherMapScreen(
                                    locationTracker = weatherViewModel.locationTracker
                                )
                            }
                            else -> {
                                WeatherDashboard(
                                    state = weatherViewModel.state,
                                    settings = settingsState,
                                    onRetry = { weatherViewModel.loadWeatherInfo() },
                                    onRefresh = { weatherViewModel.refreshWeatherInfo() },
                                    onToggleUnit = settingsViewModel::toggleTemperatureUnit,
                                    onOpenSettings = { showSettings = true },
                                    onOpenAlerts = { showAlerts = true },
                                    onOpenDetail = {
                                        val act = activity
                                        if (act != null) {
                                            com.vayu.weather.presentation.ads.AdManager.showInterstitial(act) {
                                                showDetail = true
                                            }
                                        } else {
                                            showDetail = true
                                        }
                                    },
                                    onOpenHistory = { showHistory = true },
                                    onShare = {
                                        val weatherInfo = weatherViewModel.state.weatherInfo
                                        if (weatherInfo != null) {
                                            val shareText = WeatherShareFormatter.formatForShare(
                                                context = context,
                                                cityName = weatherViewModel.currentCityName,
                                                weatherInfo = weatherInfo,
                                                isCelsius = settingsState.temperatureUnit == TemperatureUnit.CELSIUS
                                            )
                                            scope.launch {
                                                val imageUri: android.net.Uri? = withContext(Dispatchers.IO) {
                                                    var bitmap: android.graphics.Bitmap? = null
                                                    try {
                                                        bitmap = WeatherCardRenderer.generateWeatherCardBitmap(
                                                            context = context,
                                                            cityName = weatherViewModel.currentCityName ?: context.getString(R.string.default_city_name),
                                                            weatherInfo = weatherInfo,
                                                            isCelsius = settingsState.temperatureUnit == TemperatureUnit.CELSIUS
                                                        )
                                                        val file = java.io.File(context.cacheDir, "weather_share.png")
                                                        java.io.FileOutputStream(file).use { out ->
                                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                        }
                                                        androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            file
                                                        )
                                                    } catch (e: Exception) {
                                                        Log.e("VayuApp", "Share card generation failed", e)
                                                        null
                                                    } finally {
                                                        bitmap?.recycle()
                                                    }
                                                }
                                                try {
                                                    if (imageUri != null) {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "image/png"
                                                            putExtra(Intent.EXTRA_STREAM, imageUri)
                                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_weather_title)))
                                                    } else {
                                                        throw IllegalStateException("No share image")
                                                    }
                                                } catch (e: Exception) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_weather_title)))
                                                }
                                            }
                                        }
                                    },
                                    onDismissRefreshError = { weatherViewModel.clearRefreshError() },
                                    cityName = weatherViewModel.currentCityName,
                                    latitude = weatherViewModel.currentLat ?: 0.0,
                                    longitude = weatherViewModel.currentLon ?: 0.0
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
