package com.vayu.weather.presentation.navigation

/**
 * SkyCast navigation routes.
 *
 * The app uses a sealed class for type-safe navigation. Primary destinations
 * appear in the bottom navigation bar; secondary destinations are stacked on
 * top of the current primary via [Routes.stack].
 *
 * Adding a new screen:
 *  1. Add a new `data object` (or `data class` if it needs arguments) below
 *  2. Map it to a composable in [com.vayu.weather.presentation.NavGraph]
 *  3. If it's a primary destination, add it to [primaryDestinations]
 */
sealed class Routes(val route: String) {
    data object Weather : Routes("weather")
    data object Search : Routes("search")
    data object Favorites : Routes("favorites")
    data object Map : Routes("map")

    // Secondary destinations
    data object Settings : Routes("settings")
    data object Alerts : Routes("alerts")
    data object History : Routes("history")
    data object Detail : Routes("detail")
    data object PrivacyPolicy : Routes("privacy")
    data object Compare : Routes("compare")
    data object Travel : Routes("travel")

    companion object {
        /** Bottom-nav destinations in display order. */
        val primaryDestinations = listOf(Weather, Search, Favorites, Map)

        /** Map a saved/route string back to a [Routes] instance, or null. */
        fun fromRouteString(value: String?): Routes? = when (value) {
            Weather.route -> Weather
            Search.route -> Search
            Favorites.route -> Favorites
            Map.route -> Map
            Settings.route -> Settings
            Alerts.route -> Alerts
            History.route -> History
            Detail.route -> Detail
            PrivacyPolicy.route -> PrivacyPolicy
            Compare.route -> Compare
            Travel.route -> Travel
            else -> null
        }

        /** Default landing route. */
        val default: Routes = Weather
    }
}
