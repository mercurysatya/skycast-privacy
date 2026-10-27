package com.vayu.weather

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.presentation.alerts.AlertsScreen
import com.vayu.weather.presentation.alerts.AlertsState
import com.vayu.weather.presentation.alerts.SeverityFilter
import com.vayu.weather.presentation.alerts.WeatherTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the alerts screen: the list must show the *current* advisories
 * only, the header count must match the number of visible cards, and the
 * history view must still show everything.
 */
class AlertsScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The screen compares the advisory's validity against the wall clock, so
    // the fixtures are anchored to the current time.
    private val now = System.currentTimeMillis()

    private fun alert(
        id: Long,
        title: String = "Rain Alert",
        severity: String = "high",
        expiresAt: Long = now + 60_000
    ) = WeatherAlert(
        id = id,
        title = title,
        message = "High chance of rain today (80%). Don't forget your umbrella!",
        severity = severity,
        timestamp = now,
        latitude = 18.4865,
        longitude = 84.1060,
        fingerprint = "18.5|84.1|rain|2026-10-20",
        expiresAt = expiresAt
    )

    private fun render(state: AlertsState, showHistory: Boolean = false) {
        composeTestRule.setContent {
            AlertsScreen(
                state = state,
                onDeleteAlert = {},
                onClearAll = {},
                onFilterChange = {},
                onWeatherTypeFilterChange = {},
                onSnoozeAlert = { _, _ -> },
                onSnoozeDurationChange = {},
                onToggleExpand = {},
                showHistory = showHistory
            )
        }
    }

    @Test
    fun onlyCurrentAdvisoriesAreListed() {
        render(
            AlertsState(
                alerts = listOf(
                    alert(1, expiresAt = now + 60_000),
                    alert(2, expiresAt = now - 1),
                    alert(3, expiresAt = 0)
                )
            )
        )
        composeTestRule.onAllNodesWithText("Rain Alert").assertCountEquals(1)
        composeTestRule.onNodeWithText("1 alert").assertIsDisplayed()
    }

    @Test
    fun theHeaderCountMatchesTheNumberOfVisibleCards() {
        render(
            AlertsState(
                alerts = listOf(
                    alert(1, title = "Rain Alert", expiresAt = now + 60_000),
                    alert(2, title = "Wind Alert", severity = "medium", expiresAt = now + 60_000)
                )
            )
        )
        composeTestRule.onAllNodesWithText("Rain Alert").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Wind Alert").assertCountEquals(1)
        composeTestRule.onNodeWithText("2 alerts").assertIsDisplayed()
    }

    @Test
    fun theHistoryViewShowsEveryStoredAdvisory() {
        render(
            AlertsState(
                alerts = listOf(
                    alert(1, expiresAt = now + 60_000),
                    alert(2, expiresAt = now - 1),
                    alert(3, expiresAt = 0)
                )
            ),
            showHistory = true
        )
        composeTestRule.onAllNodesWithText("Rain Alert").assertCountEquals(3)
        composeTestRule.onNodeWithText("3 alerts").assertIsDisplayed()
    }

    @Test
    fun aSnoozedAdvisoryIsHidden() {
        render(
            AlertsState(
                alerts = listOf(
                    alert(1, expiresAt = now + 60_000),
                    alert(2, title = "Wind Alert", expiresAt = now + 60_000)
                ),
                snoozedAlertIds = setOf(1L)
            )
        )
        composeTestRule.onAllNodesWithText("Rain Alert").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Wind Alert").assertCountEquals(1)
    }

    @Test
    fun theEmptyStateIsShownWhenThereIsNoAdvisory() {
        render(AlertsState(alerts = emptyList()))
        composeTestRule.onNodeWithText("No Alerts Yet").assertIsDisplayed()
    }

    @Test
    fun theSeverityFilterHidesTheOtherSeverities() {
        render(
            AlertsState(
                alerts = listOf(
                    alert(1, severity = "high", expiresAt = now + 60_000),
                    alert(2, title = "Wind Alert", severity = "medium", expiresAt = now + 60_000)
                ),
                severityFilter = SeverityFilter.HIGH
            )
        )
        composeTestRule.onAllNodesWithText("Wind Alert").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Rain Alert").assertCountEquals(1)
    }
}
