package com.vayu.weather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vayu.weather.domain.model.City
import com.vayu.weather.presentation.favorites.FavoritesScreen
import com.vayu.weather.presentation.favorites.FavoritesState
import com.vayu.weather.presentation.search.SearchScreen
import com.vayu.weather.presentation.search.SearchState
import org.junit.Rule
import org.junit.Test

class SearchFavoritesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCity = City(
        id = 5128581,
        name = "New York",
        latitude = 40.7128,
        longitude = -74.0060,
        country = "United States",
        admin1 = "New York",
        countryCode = "US"
    )

    @Test
    fun search_displays_empty_state() {
        composeTestRule.setContent {
            SearchScreen(
                state = SearchState(searchQuery = "", searchResults = emptyList()),
                onQueryChange = {},
                onCitySelected = {}
            )
        }
        composeTestRule.onNodeWithText("Discover Weather Worldwide").assertIsDisplayed()
    }

    @Test
    fun search_displays_city_results() {
        composeTestRule.setContent {
            SearchScreen(
                state = SearchState(
                    searchQuery = "New York",
                    searchResults = listOf(sampleCity),
                    isLoading = false
                ),
                onQueryChange = {},
                onCitySelected = {}
            )
        }
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
        composeTestRule.onNodeWithText("New York, United States").assertIsDisplayed()
    }

    @Test
    fun search_displays_no_results_message() {
        composeTestRule.setContent {
            SearchScreen(
                state = SearchState(
                    searchQuery = "Xyz",
                    searchResults = emptyList(),
                    isLoading = false
                ),
                onQueryChange = {},
                onCitySelected = {}
            )
        }
        composeTestRule.onNodeWithText("No cities found for \"Xyz\"").assertIsDisplayed()
    }

    @Test
    fun search_displays_loading_indicator() {
        composeTestRule.setContent {
            SearchScreen(
                state = SearchState(
                    searchQuery = "Test",
                    searchResults = emptyList(),
                    isLoading = true
                ),
                onQueryChange = {},
                onCitySelected = {}
            )
        }
        composeTestRule.onNodeWithText("Search Cities").assertIsDisplayed()
    }

    @Test
    fun favorites_displays_empty_state() {
        composeTestRule.setContent {
            FavoritesScreen(
                state = FavoritesState(favorites = emptyList()),
                onCitySelected = {},
                onRemoveFavorite = {}
            )
        }
        composeTestRule.onNodeWithText("No Favorites Yet").assertIsDisplayed()
    }

    @Test
    fun favorites_displays_saved_cities() {
        composeTestRule.setContent {
            FavoritesScreen(
                state = FavoritesState(favorites = listOf(sampleCity)),
                onCitySelected = {},
                onRemoveFavorite = {}
            )
        }
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
        composeTestRule.onNodeWithText("New York, United States").assertIsDisplayed()
    }
}
