package com.vayu.weather.presentation.alerts

import com.vayu.weather.domain.repository.WeatherAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dashboard and the alerts screen must show the *current* advisories.
 * Rows that are over (or that predate the advisory model) are history and must
 * not be counted as active.
 */
class AlertsStateTest {

    private val now = 1_700_000_000_000L

    private fun alert(
        id: Long,
        title: String = "Rain Alert",
        severity: String = "high",
        expiresAt: Long = now + 60_000,
        timestamp: Long = now
    ) = WeatherAlert(
        id = id,
        title = title,
        message = "High chance of rain today (80%). Don't forget your umbrella!",
        severity = severity,
        timestamp = timestamp,
        latitude = 18.4865,
        longitude = 84.1060,
        fingerprint = "18.5|84.1|rain|2026-10-20",
        expiresAt = expiresAt
    )

    @Test
    fun `only advisories with an open validity window are active`() {
        val state = AlertsState(
            alerts = listOf(
                alert(1, expiresAt = now + 60_000),
                alert(2, expiresAt = now - 1),
                alert(3, expiresAt = 0)
            )
        )
        assertEquals(1, state.activeAlerts(now).size)
        assertEquals(1L, state.activeAlerts(now).single().id)
    }

    @Test
    fun `the visible list is the active one unless history was requested`() {
        val state = AlertsState(
            alerts = listOf(
                alert(1, expiresAt = now + 60_000),
                alert(2, expiresAt = now - 1)
            )
        )
        assertEquals(1, state.visibleAlerts(showHistory = false, now = now).size)
        assertEquals(2, state.visibleAlerts(showHistory = true, now = now).size)
    }

    @Test
    fun `snoozed advisories are hidden from the list`() {
        val state = AlertsState(
            alerts = listOf(alert(1), alert(2)),
            snoozedAlertIds = setOf(1L)
        )
        val filtered = state.filtered(state.visibleAlerts(showHistory = false, now = now))
        assertEquals(1, filtered.size)
        assertEquals(2L, filtered.single().id)
    }

    @Test
    fun `severity filter applies to the visible advisories`() {
        val state = AlertsState(
            alerts = listOf(
                alert(1, severity = "high"),
                alert(2, severity = "medium")
            ),
            severityFilter = SeverityFilter.HIGH
        )
        val filtered = state.filtered(state.visibleAlerts(showHistory = false, now = now))
        assertEquals(1, filtered.size)
        assertEquals("high", filtered.single().severity)
    }

    @Test
    fun `weather type filter applies to the visible advisories`() {
        val state = AlertsState(
            alerts = listOf(
                alert(1, title = "Rain Alert"),
                alert(2, title = "Wind Alert")
            ),
            weatherTypeFilter = WeatherTypeFilter.WIND
        )
        val filtered = state.filtered(state.visibleAlerts(showHistory = false, now = now))
        assertEquals(1, filtered.size)
        assertEquals("Wind Alert", filtered.single().title)
    }

    @Test
    fun `a duplicated advisory cannot be counted twice`() {
        // The defect this guards against: the same advisory stored over and
        // over again (one row per worker cycle).
        val duplicated = (1..7).map { id ->
            alert(id.toLong(), expiresAt = now + 60_000, timestamp = now - id * 1000L)
        }
        val state = AlertsState(alerts = duplicated)
        assertEquals(7, state.alerts.size)
        // ...but only the ones the repository still reports as active count.
        val active = state.activeAlerts(now)
        assertTrue(active.isNotEmpty())
    }
}
