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
import androidx.compose.runtime.saveable.rememberSaveable
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

/** What the next Search-screen city selection should feed. */
private enum class PendingCitySelection { NONE, COMPARE, TRAVEL }

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
    // Activity-scoped ViewModels hoisted out of conditional branches so the
    // Search screen can hand a picked city to Compare/Travel (previously these
    // screens could never receive a city — "Add city"/"Pick destination" were dead ends).
    val compareViewModel: com.vayu.weather.presentation.compare.CompareViewModel = hiltViewModel()
    val travelViewModel: com.vayu.weather.presentation.travel.TravelViewModel = hiltViewModel()
    val favoritesWithWeatherViewModel: com.vayu.weather.presentation.favorites.FavoritesWithWeatherViewModel = hiltViewModel()
    val historyViewModel: com.vayu.weather.presentation.history.WeatherHistoryViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? Activity

    val settingsState by settingsViewModel.state.collectAsState()
    val nav = com.vayu.weather.presentation.navigation.rememberSkyCastNavController()
    // Saveable so an open screen survives rotation and process death.
    var showOnboarding by rememberSaveable { mutableStateOf(true) }
    var onboardingChecked by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAlerts by rememberSaveable { mutableStateOf(false) }
    /** Opens the alerts screen in history mode (from Settings → Alert History). */
    var showAlertsHistory by rememberSaveable { mutableStateOf(false) }
    var showDetail by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicy by rememberSaveable { mutableStateOf(false) }
    var showPremium by rememberSaveable { mutableStateOf(false) }
    var privacyPolicyAnchor by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * Set when the user taps "Add city" (Compare) or "Pick destination"
     * (Travel); consumed by the next city selection on the Search screen.
     */
    var pendingCitySelection by remember { mutableStateOf(PendingCitySelection.NONE) }

    val scope = rememberCoroutineScope()
    val currentRoute = nav.currentRoute

    // Share-text strings captured at composition time so the share coroutine
    // never reaches back into LocalContext (which would race with config changes).
    val shareDefaultCity = stringResource(R.string.default_city_name)
    val shareSubject = stringResource(R.string.share_subject)
    val shareChooserTitle = stringResource(R.string.share_weather_title)

    // Single, well-ordered back-handler. Modal screens above the primary
    // route are popped in reverse order; without these flags the system
    // back press would finish the activity from Settings/Alerts/History/Detail.
    val anyModalOpen = showPrivacyPolicy || showPremium || showSettings ||
        showAlerts || showHistory || showDetail
    BackHandler(enabled = nav.stack.isNotEmpty() || anyModalOpen) {
        when {
            showPrivacyPolicy -> {
                showPrivacyPolicy = false
                privacyPolicyAnchor = null
            }
            showPremium -> showPremium = false
            showSettings -> showSettings = false
            showAlerts -> showAlerts = false
            showHistory -> showHistory = false
            showDetail -> showDetail = false
            else -> nav.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("VayuApp", "LaunchedEffect: Loading weather and ad")
        SplashGate.isReady = true
        // Read the flag before flipping onboardingChecked, otherwise returning
        // users see the onboarding screen flash for a frame on every cold start.
        val onboardingComplete = settingsViewModel.isOnboardingComplete()
        showOnboarding = !onboardingComplete
        onboardingChecked = true
        if (onboardingComplete) {
            weatherViewModel.loadWeatherInfo()
        }
    }

    // A completed wipe resets onboarding. Navigating here (rather than from
    // the settings screen) keeps the flow reliable even if the user leaves the
    // screen before the success feedback is dismissed.
    LaunchedEffect(settingsState.dataDeleted) {
        if (settingsState.dataDeleted) {
            android.widget.Toast.makeText(
                context,
                R.string.data_deleted,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            settingsViewModel.clearDeletionResult()
            showSettings = false
            showOnboarding = true
        }
    }

    // GDPR/EEA consent must be gathered before any ad request is made.
    // The UMP form is shown only when required; then the ad SDK is
    // initialized and interstitial/rewarded ads are preloaded.
    LaunchedEffect(activity) {
        val act = activity ?: return@LaunchedEffect
        ConsentManager.gatherConsent(act) {
            // Banner composables read canRequestAds during composition; bump
            // the version so they appear once consent allows ads.
            AdManager.onConsentChanged()
            if (ConsentManager.canRequestAds(act)) {
                AdManager.initializeMobileAds(act)
                AdManager.preloadNext(act)
            }
        }
    }

    // Reload ads and re-verify the Play entitlement when returning to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Re-query Play: expiry, cancellation and revocation take
                // effect here, and a purchase completed elsewhere is picked up.
                settingsViewModel.refreshBilling()
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
                        // Deletion resets this flag off so no background work
                        // runs during first-run setup. Completing onboarding
                        // restores the first-run default.
                        settingsViewModel.setNotificationsEnabled(true)
                        // Without this, a first-run user lands on the empty
                        // dashboard error state because nothing triggers a load.
                        weatherViewModel.loadWeatherInfo()
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
                    isCelsius = settingsState.temperatureUnit == TemperatureUnit.CELSIUS,
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
                    onSnoozeDurationChange = alertsViewModel::updateSnoozeDuration,
                    onBack = { showAlerts = false },
                    // The settings entry opens the full history; every other
                    // entry shows the current advisories.
                    showHistory = showAlertsHistory
                )
            } else if (showPremium) {
                val billingState = settingsViewModel.billingState.collectAsState().value
                com.vayu.weather.presentation.premium.PremiumScreen(
                    isPremium = settingsState.isPremium,
                    billing = billingState,
                    onBack = {
                        settingsViewModel.clearBillingMessage()
                        showPremium = false
                    },
                    onPurchase = { offer ->
                        val act = activity
                        if (act != null) {
                            settingsViewModel.launchPremiumPurchase(act, offer)
                        }
                    },
                    onRestore = { settingsViewModel.restorePremiumPurchases() }
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
                    onDeletionResultShown = settingsViewModel::clearDeletionResult,
                    onOpenAlerts = {
                        // "Alert History" opens the full history, including the
                        // advisories that are over.
                        showAlertsHistory = true
                        showAlerts = true
                    },
                    onOpenPremium = {
                        // Keep settings true underneath: the premium branch
                        // renders before the settings branch, so the paywall
                        // overlays Settings and BACK reveals Settings again.
                        showPremium = true
                    }
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
                                                Icon(Icons.Rounded.Cloud, stringResource(R.string.weather), Modifier.size(24.dp))
                                            }
                                        } else {
                                            Icon(Icons.Rounded.Cloud, stringResource(R.string.weather), Modifier.size(24.dp))
                                        }
                                    },
                                    label = {
                                        Text(
                                            stringResource(R.string.weather),
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Weather) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Search,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Search) },
                                    icon = { Icon(Icons.Rounded.Search, stringResource(R.string.search), Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            stringResource(R.string.search),
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Search) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Favorites,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Favorites) },
                                    icon = { Icon(Icons.Rounded.Favorite, stringResource(R.string.favorites), Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            stringResource(R.string.favorites),
                                            fontWeight = if (primary == com.vayu.weather.presentation.navigation.Routes.Favorites) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = primary == com.vayu.weather.presentation.navigation.Routes.Map,
                                    onClick = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Map) },
                                    icon = { Icon(Icons.Rounded.Map, stringResource(R.string.map), Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            stringResource(R.string.map),
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
                                com.vayu.weather.presentation.compare.SkyCastCompareScreen(
                                    selected = compareViewModel.selected,
                                    maxCities = com.vayu.weather.presentation.compare.CompareViewModel.MAX_CITIES,
                                    onAddCity = {
                                        pendingCitySelection = PendingCitySelection.COMPARE
                                        nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Search)
                                    },
                                    onRemoveCity = compareViewModel::remove,
                                    onCityTapped = { city ->
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name, city.admin1
                                        )
                                    },
                                    onBack = { nav.popBackStack() },
                                    isCelsius = settingsState.temperatureUnit == com.vayu.weather.presentation.weather.TemperatureUnit.CELSIUS
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Travel -> {
                                com.vayu.weather.presentation.travel.SkyCastTravelScreen(
                                    state = travelViewModel.state,
                                    travelDateOutOfRange = travelViewModel.travelDateOutOfRange,
                                    onSetDate = travelViewModel::setDate,
                                    onPickDestination = {
                                        pendingCitySelection = PendingCitySelection.TRAVEL
                                        nav.navigate(com.vayu.weather.presentation.navigation.Routes.Search)
                                    },
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
                                        when (pendingCitySelection) {
                                            PendingCitySelection.COMPARE -> {
                                                pendingCitySelection = PendingCitySelection.NONE
                                                compareViewModel.add(city)
                                                nav.navigate(com.vayu.weather.presentation.navigation.Routes.Compare)
                                            }
                                            PendingCitySelection.TRAVEL -> {
                                                pendingCitySelection = PendingCitySelection.NONE
                                                travelViewModel.setDestination(city)
                                                nav.navigate(com.vayu.weather.presentation.navigation.Routes.Travel)
                                            }
                                            PendingCitySelection.NONE -> {
                                                weatherViewModel.loadWeatherForCity(
                                                    city.latitude, city.longitude, city.name, city.admin1
                                                )
                                                nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Weather)
                                            }
                                        }
                                    },
                                    onToggleFavorite = { city ->
                                        favoritesViewModel.toggleFavorite(city)
                                    },
                                    isFavorite = { cityId ->
                                        favoritesViewModel.state.favorites.any { it.id == cityId }
                                    },
                                    onClearRecentSearches = searchViewModel::clearRecentSearches,
                                    onOpenTravel = {
                                        // No interstitial on entry: interrupting navigation into
                                        // a feature screen violates the conservative ad policy for
                                        // a weather app (AdMob disruptive-ads guidance). The
                                        // interstitial API remains for future policy-compliant
                                        // placements only.
                                        nav.navigate(com.vayu.weather.presentation.navigation.Routes.Travel)
                                    }
                                )
                            }
                            nav.currentRoute == com.vayu.weather.presentation.navigation.Routes.Favorites -> {
                                com.vayu.weather.presentation.favorites.SkyCastFavoritesScreen(
                                    favorites = favoritesWithWeatherViewModel.favorites,
                                    onCitySelected = { city ->
                                        weatherViewModel.loadWeatherForCity(
                                            city.latitude, city.longitude, city.name, city.admin1
                                        )
                                        nav.replacePrimary(com.vayu.weather.presentation.navigation.Routes.Weather)
                                    },
                                    onRemoveFavorite = favoritesWithWeatherViewModel::removeFavorite,
                    onReorder = favoritesWithWeatherViewModel::reorderFavorites,
                                    onBrowseCities = { nav.navigate(com.vayu.weather.presentation.navigation.Routes.Search) },
                                    onCompare = {
                                        // No interstitial on entry — see onOpenTravel above.
                                        nav.navigate(com.vayu.weather.presentation.navigation.Routes.Compare)
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
                                var isDetailedForecastUnlocked by rememberSaveable { mutableStateOf(false) }
                                // Premium unlocks the extended outlook permanently; the
                                // rewarded-ad path remains for free users.
                                val detailedForecastUnlocked =
                                    isDetailedForecastUnlocked || settingsState.isPremium
                                com.vayu.weather.presentation.weather.SkyCastHomeScreen(
                                    state = weatherViewModel.state,
                                    settings = settingsState,
                                    cityName = weatherViewModel.currentCityName,
                                    regionName = weatherViewModel.state.regionName,
                                    onOpenSettings = { showSettings = true },
                                    onOpenAlerts = {
                                        // The dashboard opens the current
                                        // advisories, not the history.
                                        showAlertsHistory = false
                                        showAlerts = true
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
                                                    // Text fallback needs its own guard: no activity may
                                                    // accept text/plain (or the host may be finishing).
                                                    try {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                                                    } catch (e2: Exception) {
                                                        Log.e("VayuApp", "Share failed", e2)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onRefresh = { weatherViewModel.refreshWeatherInfo() },
                                    onOpenDetail = {
                                        // No interstitial here: opening a
                                        // detailed forecast is core weather
                                        // functionality (AdMob disruptive-ad
                                        // guidance). Travel/Compare entries
                                        // keep their capped interstitials.
                                        showDetail = true
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
                                    isDetailedForecastUnlocked = detailedForecastUnlocked,
                                    isPremium = settingsState.isPremium,
                                    onOpenPremium = { showPremium = true },
                                    onWatchAdForDetails = {
                                        val act = activity
                                        if (act != null) {
                                            if (!com.vayu.weather.presentation.ads.AdManager.isRewardedAdReady()) {
                                                android.widget.Toast.makeText(
                                                    act,
                                                    act.getString(R.string.ad_not_ready),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                com.vayu.weather.presentation.ads.AdManager.loadRewardedAd(act)
                                            } else {
                                                com.vayu.weather.presentation.ads.AdManager.showRewardedAd(act, onRewardGranted = {
                                                    isDetailedForecastUnlocked = true
                                                }, onAdDismissed = {})
                                            }
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