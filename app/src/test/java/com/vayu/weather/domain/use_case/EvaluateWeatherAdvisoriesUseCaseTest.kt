package com.vayu.weather.domain.use_case

import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.domain.model.CurrentWeather
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.WeatherAdvisoryFingerprint
import com.vayu.weather.domain.model.WeatherAdvisoryTextProvider
import com.vayu.weather.domain.model.WeatherAdvisoryType
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherAlertUpsertResult
import com.vayu.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Regression tests for the duplicate-alert defect: the app generated one
 * "Rain Alert" row every time the 3-hourly worker ran, so the dashboard ended
 * up with several active cards for one single ongoing condition.
 *
 * The advisories are now identified by a fingerprint (type + bucketed location
 * + forecast day) and written with an idempotent upsert, so repeated
 * evaluation can never create a second row for the same advisory.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EvaluateWeatherAdvisoriesUseCaseTest {

    private lateinit var repository: WeatherRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var useCase: EvaluateWeatherAdvisoriesUseCase

    /** Mirrors the repository's upsert semantics, keyed on the fingerprint. */
    /** Fixed clock: 2026-10-20T12:00:00Z � independent of the wall clock. */
    private val FIXED_NOW: Long = 1791892800000L

    private val stored = mutableMapOf<String, WeatherAlert>()
    private var nextId = 1L

    @Before
    fun setup() {
        repository = mock()
        settingsManager = mock()
        // The settings accessors are suspend functions, so the stubs have to be
        // installed from a coroutine.
        runBlocking {
            whenever(settingsManager.getSeverityFilter()).thenReturn("ALL")
            whenever(settingsManager.getRainAlertThreshold()).thenReturn(50)
            whenever(settingsManager.getEnableWindAlerts()).thenReturn(false)
            whenever(settingsManager.getWindAlertThreshold()).thenReturn(60)
            whenever(settingsManager.getEnableUvAlerts()).thenReturn(false)
            whenever(settingsManager.getUvAlertThreshold()).thenReturn(8)
            whenever(settingsManager.getEnableHeatAlerts()).thenReturn(false)
            whenever(settingsManager.getHeatAlertThreshold()).thenReturn(40)
            whenever(settingsManager.getEnableColdAlerts()).thenReturn(false)
            whenever(settingsManager.getColdAlertThreshold()).thenReturn(0)
        }

        runBlocking {
            whenever(repository.upsertWeatherAlert(any())).thenAnswer { invocation ->
                val alert = invocation.getArgument<WeatherAlert>(0)
                val fingerprint = alert.fingerprint.orEmpty()
                val existing = stored[fingerprint]
                when {
                    existing == null -> {
                        val id = nextId++
                        stored[fingerprint] = alert.copy(id = id)
                        WeatherAlertUpsertResult.Created(id)
                    }
                    existing.message != alert.message || existing.severity != alert.severity -> {
                        stored[fingerprint] = alert.copy(id = existing.id)
                        WeatherAlertUpsertResult.Updated(
                            existing.id,
                            rank(alert.severity) > rank(existing.severity)
                        )
                    }
                    else -> WeatherAlertUpsertResult.Unchanged(existing.id)
                }
            }
            whenever(repository.resolveWeatherAlert(any(), any())).thenAnswer { invocation ->
                val fingerprint = invocation.getArgument<String>(0)
                val resolvedAt = invocation.getArgument<Long>(1)
                val existing = stored[fingerprint]
                if (existing != null && existing.expiresAt > resolvedAt) {
                    stored[fingerprint] = existing.copy(expiresAt = resolvedAt)
                    true
                } else {
                    false
                }
            }
        }

        useCase = EvaluateWeatherAdvisoriesUseCase(repository, settingsManager, FakeTextProvider)
    }

    // ------------------------------------------------------------------
    // A. Identity
    // ------------------------------------------------------------------

    @Test
    fun `fingerprint is deterministic for the same advisory`() {
        val a = WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4865, 84.1060, "2026-10-20")
        val b = WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4865, 84.1060, "2026-10-20")
        assertEquals(a, b)
    }

    @Test
    fun `fingerprint survives gps jitter of the same place`() {
        // Two fixes of the same place, ~350 m apart (the phone in a pocket).
        val a = WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4865237, 84.1060711, "2026-10-20")
        val b = WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4830000, 84.1030000, "2026-10-20")
        assertEquals(a, b)
    }

    @Test
    fun `fingerprint separates types locations and forecast days`() {
        val base = WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4865, 84.1060, "2026-10-20")
        assertNotEquals(
            base,
            WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.WIND, 18.4865, 84.1060, "2026-10-20")
        )
        assertNotEquals(
            base,
            WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 12.9716, 77.5946, "2026-10-20")
        )
        assertNotEquals(
            base,
            WeatherAdvisoryFingerprint.of(WeatherAdvisoryType.RAIN, 18.4865, 84.1060, "2026-10-21")
        )
    }

    // ------------------------------------------------------------------
    // B. Duplicate prevention
    // ------------------------------------------------------------------

    @Test
    fun `repeated evaluation of the same forecast keeps one advisory`() = runTest {
        repeat(10) {
            useCase(weatherInfo(rainProbability = 80), 18.4865237, 84.1060711, "Srikakulam", now = FIXED_NOW)
        }
        assertEquals(1, stored.size)
    }

    @Test
    fun `restarting the app and reloading the same forecast keeps one advisory`() = runTest {
        useCase(weatherInfo(rainProbability = 80), 18.4865237, 84.1060711, "Srikakulam", now = FIXED_NOW)
        // New process: a fresh ViewModel/worker, the same forecast.
        useCase(weatherInfo(rainProbability = 80), 18.4865237, 84.1060711, "Srikakulam", now = FIXED_NOW)
        useCase(weatherInfo(rainProbability = 80), 18.4628251, 84.1044212, "Srikakulam", now = FIXED_NOW)
        assertEquals(1, stored.size)
    }

    @Test
    fun `concurrent evaluations produce one advisory`() = runTest {
        val jobs = (1..10).map {
            async {
                useCase(weatherInfo(rainProbability = 80), 18.4865237, 84.1060711, "Srikakulam", now = FIXED_NOW)
            }
        }
        jobs.awaitAll()
        assertEquals(1, stored.size)
    }

    @Test
    fun `an unchanged advisory is reported as unchanged`() = runTest {
        useCase(weatherInfo(rainProbability = 80), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val changes = useCase(weatherInfo(rainProbability = 80), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val rain = changes.filterIsInstance<AdvisoryChange.Unchanged>()
            .firstOrNull { it.type == WeatherAdvisoryType.RAIN }
        assertTrue("expected an unchanged rain advisory, got $changes", rain != null)
    }

    // ------------------------------------------------------------------
    // C. Legitimate updates and distinct advisories
    // ------------------------------------------------------------------

    @Test
    fun `a changed probability updates the same advisory`() = runTest {
        useCase(weatherInfo(rainProbability = 80), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val changes = useCase(weatherInfo(rainProbability = 85), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        assertEquals(1, stored.size)
        assertTrue(stored.values.single().message.contains("85"))
        val updated = changes.filterIsInstance<AdvisoryChange.Updated>()
            .firstOrNull { it.type == WeatherAdvisoryType.RAIN }
        assertTrue("expected an updated rain advisory, got $changes", updated != null)
    }

    @Test
    fun `an escalation is reported as escalation`() = runTest {
        useCase(weatherInfo(rainProbability = 60), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val changes = useCase(weatherInfo(rainProbability = 90), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val updated = changes.filterIsInstance<AdvisoryChange.Updated>()
            .firstOrNull { it.type == WeatherAdvisoryType.RAIN }
        assertTrue("expected an escalation, got $changes", updated != null && updated.escalated)
    }

    @Test
    fun `a de-escalation is not reported as escalation`() = runTest {
        useCase(weatherInfo(rainProbability = 90), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val changes = useCase(weatherInfo(rainProbability = 60), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val updated = changes.filterIsInstance<AdvisoryChange.Updated>()
            .firstOrNull { it.type == WeatherAdvisoryType.RAIN }
        assertTrue("expected a de-escalation, got $changes", updated != null && !updated.escalated)
    }

    @Test
    fun `two advisory types for one location are kept apart`() = runTest {
        whenever(settingsManager.getEnableWindAlerts()).thenReturn(true)
        whenever(settingsManager.getWindAlertThreshold()).thenReturn(20)
        useCase(weatherInfo(rainProbability = 80, windSpeed = 30.0), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        assertEquals(2, stored.size)
    }

    @Test
    fun `two locations with similar messages are kept apart`() = runTest {
        useCase(weatherInfo(rainProbability = 80), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        useCase(weatherInfo(rainProbability = 80), 12.9716, 77.5946, "Bengaluru", now = FIXED_NOW)
        assertEquals(2, stored.size)
    }

    @Test
    fun `two forecast days for one location are kept apart`() = runTest {
        useCase(weatherInfo(rainProbability = 80, date = "2026-10-20"), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        useCase(weatherInfo(rainProbability = 80, date = "2026-10-21"), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        assertEquals(2, stored.size)
    }

    // ------------------------------------------------------------------
    // D. Lifecycle: validity, expiry, resolving
    // ------------------------------------------------------------------

    @Test
    fun `an advisory is valid until the end of its forecast day`() = runTest {
        useCase(weatherInfo(rainProbability = 80, date = "2026-10-20"), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val alert = stored.values.single()
        // End of 2026-10-20 in Asia/Kolkata == 18:29:59 UTC.
        val expected = java.time.LocalDate.parse("2026-10-20")
            .atTime(23, 59, 59)
            .atZone(java.time.ZoneId.of("Asia/Kolkata"))
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, alert.expiresAt)
    }

    @Test
    fun `an advisory is active while its validity window is open`() = runTest {
        useCase(weatherInfo(rainProbability = 80, date = "2026-10-20"), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        val alert = stored.values.single()
        assertTrue(alert.isActive(alert.timestamp + 1000))
        assertTrue(!alert.isActive(alert.expiresAt + 1))
    }

    @Test
    fun `a condition that no longer holds closes the advisory`() = runTest {
        useCase(weatherInfo(rainProbability = 80), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        assertTrue(stored.values.single().isActive(stored.values.single().timestamp + 10))

        // The forecast now says 10 % rain — below the 50 % threshold.
        useCase(weatherInfo(rainProbability = 10), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)

        val alert = stored.values.single()
        assertTrue(
            "the advisory should have been closed: keys=${stored.keys} " +
                "expiresAt=${alert.expiresAt} ts=${alert.timestamp} now=${System.currentTimeMillis()}",
            !alert.isActive(alert.timestamp + 10)
        )
    }

    @Test
    fun `missing forecast data produces no advisory at all`() = runTest {
        val changes = useCase(weatherInfo(rainProbability = null), 18.4865, 84.1060, "Srikakulam", now = FIXED_NOW)
        assertTrue(changes.isEmpty())
        assertTrue(stored.isEmpty())
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun rank(severity: String) = when (severity) {
        "high" -> 3
        "medium" -> 2
        "low" -> 1
        else -> 0
    }

    private fun weatherInfo(
        rainProbability: Int?,
        windSpeed: Double? = 5.0,
        date: String = "2026-10-20"
    ) = WeatherInfo(
        current = CurrentWeather(
            time = "${date}T08:00",
            temperature = 26.0,
            humidity = 70.0,
            weatherCode = 61,
            windSpeed = windSpeed,
            windDirection = 120.0,
            apparentTemperature = 27.0,
            isDay = true
        ),
        hourly = emptyList(),
        daily = listOf(
            DailyWeather(
                date = date,
                weatherCode = 61,
                maxTemp = 29.0,
                minTemp = 24.0,
                uvIndex = 3.0,
                precipitationProbability = rainProbability
            )
        ),
        timezone = "Asia/Kolkata"
    )

    private object FakeTextProvider : WeatherAdvisoryTextProvider {
        override fun titleFor(type: WeatherAdvisoryType): String = when (type) {
            WeatherAdvisoryType.RAIN -> "Rain Alert"
            WeatherAdvisoryType.WIND -> "Wind Alert"
            WeatherAdvisoryType.UV -> "UV Alert"
            WeatherAdvisoryType.HEAT -> "Heat Alert"
            WeatherAdvisoryType.COLD -> "Cold Alert"
        }

        override fun messageFor(type: WeatherAdvisoryType, value: Int): String = when (type) {
            WeatherAdvisoryType.RAIN -> "High chance of rain today ($value%). Don't forget your umbrella!"
            WeatherAdvisoryType.WIND -> "Strong winds expected today ($value km/h). Stay safe!"
            WeatherAdvisoryType.UV -> "Very high UV index today (UV $value). Wear sunscreen!"
            WeatherAdvisoryType.HEAT -> "Extreme heat expected today ($value°C). Stay hydrated!"
            WeatherAdvisoryType.COLD -> "Freezing temperatures expected today ($value°C). Stay warm!"
        }
    }
}
