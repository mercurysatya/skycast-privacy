package com.vayu.weather.presentation

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vayu.weather.presentation.ads.AdManager
import com.vayu.weather.R
import com.vayu.weather.presentation.alerts.AlertsScreen
import com.vayu.weather.presentation.alerts.AlertsViewModel
import com.vayu.weather.presentation.favorites.FavoritesViewModel
import com.vayu.weather.presentation.map.WeatherMapScreen
import com.vayu.weather.presentation.search.SearchScreen
import com.vayu.weather.presentation.search.SearchViewModel
import com.vayu.weather.presentation.settings.PrivacyPolicyScreen
import com.vayu.weather.presentation.settings.SettingsScreen
import com.vayu.weather.presentation.settings.SettingsViewModel
import com.vayu.weather.presentation.weather.ThemeMode
import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.SkyCastHomeScreen
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
import androidx.compose.ui.text.font.FontWeight

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
    val context = LocalContext.current
    val activity = context as? Activity

    val settingsState by settingsViewModel.state.collectAsState()
    val nav = com.vayu.weather.presentation.navigation.rememberSkyCastNavController()
    var showOnboarding by remember { mutableStateOf(true) }
    var onboardingChecked by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var privacyPolicyAnchor by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val currentRoute = nav.currentRoute

    // Share-text strings captured at composition time so the share coroutine
    // never reaches back into LocalContext (which would race with config changes).
    val shareDefaultCity = stringResource(R.string.default_city_name)
    val shareSubject = stringResource(R.string.share_subject)
    val shareChooserTitle = stringResource(R.string.share_weather_title)

    // Single, well-ordered back-handler. Modal screens above the primary
    // route are popped in reverse order.
    BackHandler(enabled = nav.stack.isNotEmpty() || showPrivacyPolicy) {
        if (showPrivacyPolicy) {
            showPrivacyPolicy = false
            privacyPolicyAnchor = null
        } else if (!nav.popBackStack()) {
            // Stack empty — nothing to do; system will exit the app.
        }
    }

    LaunchedEffect(Unit) {
        Log.d("VayuApp", "LaunchedEffect: Loading weather and ad")
        SplashGate.isReady = true
        onboardingChecked = true
        showOnboarding = !settingsViewModel.isOnboardingComplete()
    }

    // Reload ads when app returns to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val act = activity
                if (act != null && ConsentManager.canRequestAds(act)) {
                    AdManager.preloadNext(act)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                val historyViewModel: WeatherHistoryViewModel = hiltViewModel()
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
                    onQuietHoursEnabledChange = settingsViewModel::setQuietHoursEnabled,
                    onQuietHoursStartHourChange = settingsViewModel::setQuietHoursStartHour,
                    onQuietHoursStartMinuteChange = settingsViewModel::setQuietHoursStartMinute,
                    onQuietHoursEndHourChange = settingsViewModel::setQuietHoursEndHour,
                    onQuietHoursEndMinuteChange = settingsViewModel::setQuietHoursEndMinute,
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
                        // Only show the bottom bar on primary destinations
                        if (nav.stack.isEmpty()) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                tonalElevation = 3.dp
                            ) {
                                val primary = nav.primaryRoute
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Weather,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Weather) },
                                    icon = {
                                        val filteredAlerts = remember(alertsViewModel.state.alerts) {
                                            val now = System.currentTimeMillis()
                                            alertsViewModel.state.alerts.filter { now - it.timestamp <= 24 * 60 * 60 * 1000L }
                                        }
                                        if (filteredAlerts.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text(filteredAlerts.size.toString()) }
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Cloud, "Weather", Modifier.size(24.dp))
                                            }
                                        } else {
                                            Icon(Icons.Rounded.Cloud, "Weather", Modifier.size(24.dp))
                                        }
                                    },
                                    label = {
                                        Text(
                                            "Weather",
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Weather) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Search,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Search) },
                                    icon = { Icon(Icons.Rounded.Search, "Search", Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            "Search",
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Search) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Favorites,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Favorites) },
                                    icon = { Icon(Icons.Rounded.Favorite, "Favorites", Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            "Favorites",
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Favorites) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Map,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Map) },
                                    icon = { Icon(Icons.Rounded.Map, "Map", Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            "Map",
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Map) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when {
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Compare -> {
                                val compareViewModel: com.vayu.weather.presentation.compare.CompareViewModel = hiltViewModel()
                                com.vayu.weather.presentation.compare.SkyCastCompareScreen(
                                    selected = compareViewModel.selected,
                                    maxCities = com.vayu.weather.presentation.compare.CompareViewModel.MAX_CITIES,
                                    onAddCity = { nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Search) },
                                    onRemoveCity = compareViewModel::remove,
                                    onCityTapped = { city ->
                                        weatherViewModel.loadWeatherForCity(city.latitude, city.longitude, city.name)
                                    },
                                    onBack = { nav.popBackStack() },
                                    isCelsius = settingsState.temperatureUnit == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Travel -> {
                                val travelViewModel: com.vayu.weather.presentation.travel.TravelViewModel = hiltViewModel()
                                com.vayu.weather.presentation.travel.SkyCastTravelScreen(
                                    state = travelViewModel.state,
                                    onSetDate = travelViewModel::setDate,
                                    onPickDestination = { nav.popBackStack() },
                                    onRefresh = travelViewModel::refresh,
                                    isCelsius = settingsState.temperatureUnit == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS,
                                    onBack = { nav.popBackStack() }
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Search -> {
                                SearchScreen(
                                    state = searchViewModel.state,
                                    onQueryChange = searchViewModel::onQueryChange,
                                    onCitySelected = { city ->
                                        searchViewModel.addToRecentSearches(city)
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name
                                        )
                                        nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Weather)
                                    },
                                    onToggleFavorite = { city ->
                                        favoritesViewModel.toggleFavorite(city)
                                    },
                                    isFavorite = { cityId ->
                                        favoritesViewModel.state.favorites.any { it.id == cityId }
                                    },
                                    onClearRecentSearches = searchViewModel::clearRecentSearches,
                                    onOpenTravel = {
                                        val act = activity
                                        if (act != null) {
                                            com.vayu.weather.presentation.ads.AdManager.showInterstitial(act) {
                                                nav.navigate(com.vayu.weather.presentation.navigation.Routes.Travel)
                                            }
                                        } else {
                                            nav.navigate(com.vayu.weather.presentation.navigation.Routes.Travel)
                                        }
                                    }
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Favorites -> {
                                val favoritesWithWeatherViewModel: com.vayu.weather.presentation.favorites.FavoritesWithWeatherViewModel = hiltViewModel()
                                com.vayu.weather.presentation.favorites.SkyCastFavoritesScreen(
                                    favorites = favoritesWithWeatherViewModel.favorites,
                                    onCitySelected = { city ->
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name
                                        )
                                        nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Weather)
                                    },
                                    onRemoveFavorite = favoritesWithWeatherViewModel::removeFavorite,
                                    onBrowseCities = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Search) },
                                    onCompare = {
                                        val act = activity
                                        if (act != null) {
                                            com.vayu.weather.presentation.ads.AdManager.showInterstitial(act) {
                                                nav.navigate(com.vayu.weather.presentation.navigation.Routes.Compare)
                                            }
                                        } else {
                                            nav.navigate(com.vayu.weather.presentation.navigation.Routes.Compare)
                                        }
                                    },
                                    isCelsius = settingsState.temperatureUnit == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Map -> {
                                WeatherMapScreen(
                                    locationTracker = weatherViewModel.locationTracker
                                )
                            }
                            else -> {
                                var openMetric by remember { mutableStateOf<String?>(null) }
                                var isDetailedForecastUnlocked by remember { mutableStateOf(false) }
                                com.vayu.weather.presentation.weather.SkyCastHomeScreen(
                                    state = weatherViewModel.state,
                                    settings = settingsState,
                                    cityName = weatherViewModel.currentCityName,
                                    regionName = weatherViewModel.state.regionName,
                                    onOpenSettings = { showSettings = true },
                                    onOpenAlerts = { showAlerts = true },
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
                                                            cityName = weatherViewModel.currentCityName ?: shareDefaultCity,
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
                                                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                                                    } else {
                                                        throw IllegalStateException("No share image")
                                                    }
                                                } catch (e: Exception) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                                                }
                                            }
                                        }
                                    },
                                    onRefresh = { weatherViewModel.refreshWeatherInfo() },
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
                                    onToggleUnit = settingsViewModel::toggleTemperatureUnit,
                                    onToggleTheme = {
                                        val currentMode = settingsState.themeMode
                                        val nextMode = when (currentMode) {
                                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                            ThemeMode.LIGHT -> ThemeMode.DARK
                                            else -> ThemeMode.SYSTEM
                                        }
                                        settingsViewModel.setThemeMode(nextMode)
                                    },
                                    themeMode = settingsState.themeMode,
                                    onOpenMetricDetail = { key -> openMetric = key },
                                    isDetailedForecastUnlocked = isDetailedForecastUnlocked,
                                    onWatchAdForDetails = {
                                        val act = activity
                                        if (act != null) {
                                            com.vayu.weather.presentation.ads.AdManager.showRewardedAd(act, onRewardGranted = {
                                                isDetailedForecastUnlocked = true
                                            }, onAdDismissed = {})
                                        }
                                    }
                                )

                                if (openMetric != null && weatherViewModel.state.weatherInfo != null) {
                                    val info = weatherViewModel.state.weatherInfo!!
                                    com.vayu.weather.presentation.components.skycast.SkyCastMetricDetailSheet(
                                        metric = openMetric,
                                        info = info,
                                        isCelsius = settingsState.temperatureUnit == TemperatureUnit.CELSIUS,
                                        onDismiss = { openMetric = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}