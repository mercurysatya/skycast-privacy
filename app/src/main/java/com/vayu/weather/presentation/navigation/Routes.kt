package com.vayu.weather.presentation.navigation

sealed class Route(val route: String) {
    data object Weather : Route("Weather")
    data object Search : Route("Search")
    data object Map : Route("Map")
    data object Favorites : Route("Favorites")
    data object Settings : Route("Settings")
}
