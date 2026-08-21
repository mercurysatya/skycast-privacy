package com.vayu.weather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vayu.weather.domain.model.AirQuality
import com.vayu.weather.presentation.components.AirQualityCard
import org.junit.Rule
import org.junit.Test

class AirQualityCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAirQuality = AirQuality(
        europeanAqi = 25,
        usAqi = 30,
        pm25 = 12.5,
        pm10 = 20.0,
        nitrogenDioxide = 15.0,
        ozone = 60.0,
        sulphurDioxide = 5.0,
        carbonMonoxide = 0.3
    )

    @Test
    fun airQualityCard_displays_aqi_label() {
        composeTestRule.setContent {
            AirQualityCard(airQuality = sampleAirQuality)
        }
        composeTestRule.onNodeWithText("Fair").assertIsDisplayed()
    }

    @Test
    fun airQualityCard_displays_title() {
        composeTestRule.setContent {
            AirQualityCard(airQuality = sampleAirQuality)
        }
        composeTestRule.onNodeWithText("Air Quality").assertIsDisplayed()
    }

    @Test
    fun airQualityCard_displays_pollutants() {
        composeTestRule.setContent {
            AirQualityCard(airQuality = sampleAirQuality)
        }
        composeTestRule.onNodeWithText("PM2.5").assertIsDisplayed()
        composeTestRule.onNodeWithText("PM10").assertIsDisplayed()
        composeTestRule.onNodeWithText("NO₂").assertIsDisplayed()
    }

    @Test
    fun airQualityCard_displays_european_aqi() {
        composeTestRule.setContent {
            AirQualityCard(airQuality = sampleAirQuality)
        }
        composeTestRule.onNodeWithText("European AQI: 25").assertIsDisplayed()
    }

    @Test
    fun airQualityCard_shows_nothing_for_null() {
        composeTestRule.setContent {
            AirQualityCard(airQuality = null)
        }
        composeTestRule.onNodeWithText("Air Quality").assertDoesNotExist()
    }
}
