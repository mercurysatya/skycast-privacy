package com.vayu.weather

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vayu.weather.data.local.VayuDatabase
import com.vayu.weather.data.local.VayuMigrations
import com.vayu.weather.data.local.WeatherAlertEntity
import com.vayu.weather.data.remote.GeocodingApi
import com.vayu.weather.data.remote.OpenMeteoAirQualityApi
import com.vayu.weather.data.remote.OpenMeteoApi
import com.vayu.weather.data.repository.WeatherRepositoryImpl
import com.vayu.weather.domain.repository.WeatherAlert
import com.vayu.weather.domain.repository.WeatherAlertUpsertResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for the alert store: the repository's upsert must be
 * idempotent (the defect was that every worker cycle appended another row for
 * the same ongoing condition) and the v5 -> v6 migration must not destroy
 * anything.
 */
@RunWith(AndroidJUnit4::class)
class WeatherAlertDatabaseTest {

    private lateinit var db: VayuDatabase
    private lateinit var repository: WeatherRepositoryImpl

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, VayuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WeatherRepositoryImpl(
            api = noOpApi(OpenMeteoApi::class.java),
            airQualityApi = noOpApi(OpenMeteoAirQualityApi::class.java),
            geocodingApi = noOpApi(GeocodingApi::class.java),
            dao = db.weatherDao,
            db = db,
            application = context as Application
        )
    }

    /** The alert store never touches the network — these stubs are never called. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> noOpApi(type: Class<T>): T = java.lang.reflect.Proxy.newProxyInstance(
        type.classLoader,
        arrayOf(type)
    ) { _, _, _ -> null } as T

    @After
    fun tearDown() {
        db.close()
    }

    private fun alert(
        fingerprint: String = "18.5|84.1|rain|2026-10-20",
        message: String = "High chance of rain today (80%). Don't forget your umbrella!",
        severity: String = "high",
        expiresAt: Long = 4_000_000_000_000L
    ) = WeatherAlert(
        title = "Rain Alert",
        message = message,
        severity = severity,
        timestamp = 1_700_000_000_000L,
        latitude = 18.4865,
        longitude = 84.1060,
        cityName = "Srikakulam",
        fingerprint = fingerprint,
        expiresAt = expiresAt
    )

    @Test
    fun ingestingTheSameAdvisoryTenTimesStoresOneRow() = runBlocking {
        repeat(10) {
            val result = repository.upsertWeatherAlert(alert())
            assertTrue(result is WeatherAlertUpsertResult.Created || result is WeatherAlertUpsertResult.Unchanged)
        }
        val stored = repository.getWeatherAlerts(50).first()
        assertEquals(1, stored.size)
    }

    @Test
    fun aChangedForecastUpdatesTheExistingRow() = runBlocking {
        repository.upsertWeatherAlert(alert())
        val result = repository.upsertWeatherAlert(alert(message = "High chance of rain today (85%). Don't forget your umbrella!"))
        assertTrue("expected an update, got $result", result is WeatherAlertUpsertResult.Updated)

        val stored = repository.getWeatherAlerts(50).first()
        assertEquals(1, stored.size)
        assertTrue(stored.single().message.contains("85"))
    }

    @Test
    fun anEscalationIsReportedAsEscalation() = runBlocking {
        repository.upsertWeatherAlert(alert(severity = "medium", message = "m"))
        val result = repository.upsertWeatherAlert(alert(severity = "high", message = "h"))
        assertTrue("expected an escalation, got $result", result is WeatherAlertUpsertResult.Updated)
        assertTrue((result as WeatherAlertUpsertResult.Updated).escalated)
    }

    @Test
    fun differentAdvisoriesGetTheirOwnRows() = runBlocking {
        repository.upsertWeatherAlert(alert(fingerprint = "18.5|84.1|rain|2026-10-20"))
        repository.upsertWeatherAlert(alert(fingerprint = "18.5|84.1|wind|2026-10-20"))
        repository.upsertWeatherAlert(alert(fingerprint = "12.9|77.6|rain|2026-10-20"))
        repository.upsertWeatherAlert(alert(fingerprint = "18.5|84.1|rain|2026-10-21"))
        assertEquals(4, repository.getWeatherAlerts(50).first().size)
    }

    @Test
    fun theUniqueIndexRejectsASecondRowWithTheSameFingerprint() = runBlocking {
        val first = db.weatherDao.insertWeatherAlert(
            WeatherAlertEntity(
                title = "Rain Alert",
                message = "m",
                severity = "high",
                timestamp = 1L,
                latitude = 18.4865,
                longitude = 84.1060,
                cityName = null,
                fingerprint = "18.5|84.1|rain|2026-10-20",
                expiresAt = 4_000_000_000_000L
            )
        )
        assertTrue(first > 0)
        var failed = false
        try {
            db.weatherDao.insertWeatherAlert(
                WeatherAlertEntity(
                    title = "Rain Alert",
                    message = "m",
                    severity = "high",
                    timestamp = 2L,
                    latitude = 18.4865,
                    longitude = 84.1060,
                    cityName = null,
                    fingerprint = "18.5|84.1|rain|2026-10-20",
                    expiresAt = 4_000_000_000_000L
                )
            )
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("the unique index must reject a duplicate fingerprint", failed)
    }

    @Test
    fun onlyValidAdvisoriesAreReportedAsActive() = runBlocking {
        repository.upsertWeatherAlert(alert(fingerprint = "a", expiresAt = 4_000_000_000_000L))
        repository.upsertWeatherAlert(alert(fingerprint = "b", expiresAt = 1L))
        val active = repository.getActiveWeatherAlerts(50, now = 1_700_000_000_000L).first()
        assertEquals(1, active.size)
        assertEquals("a", active.single().fingerprint)
    }

    @Test
    fun resolvingAnAdvisoryKeepsItAsHistory() = runBlocking {
        repository.upsertWeatherAlert(alert())
        repository.resolveWeatherAlert("18.5|84.1|rain|2026-10-20", resolvedAt = 1_700_000_000_500L)

        val all = repository.getWeatherAlerts(50).first()
        assertEquals("the row must not be deleted", 1, all.size)
        assertTrue("the advisory must no longer be active", !all.single().isActive(1_700_000_000_500L))
    }

    /**
     * The v5 -> v6 migration must keep every stored alert and only mark the
     * legacy rows as expired.
     */
    @Test
    fun migrationFromV5KeepsEveryAlertRow() = runBlocking {
        val dbFile = File(context.cacheDir, "migration_test.db")
        if (dbFile.exists()) dbFile.delete()
        val legacy = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        // The complete v5 schema — Room validates every table when it opens
        // the migrated database.
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_cities (
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                country TEXT,
                admin1 TEXT,
                countryCode TEXT,
                sortIndex INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weather_cache (
                locationId TEXT NOT NULL,
                weatherDataJson TEXT NOT NULL,
                lastUpdated INTEGER NOT NULL,
                PRIMARY KEY(locationId)
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                country TEXT,
                admin1 TEXT,
                countryCode TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weather_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                cityName TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                temperature REAL NOT NULL,
                weatherCode INTEGER NOT NULL,
                humidity REAL,
                windSpeed REAL,
                apparentTemperature REAL,
                isDay INTEGER NOT NULL DEFAULT 1,
                surfacePressure REAL,
                uvIndex REAL,
                precipitationProbability INTEGER
            )
            """.trimIndent()
        )
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weather_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                severity TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                latitude REAL,
                longitude REAL,
                cityName TEXT
            )
            """.trimIndent()
        )
        legacy.execSQL(
            "INSERT INTO weather_alerts (title, message, severity, timestamp, latitude, longitude, cityName) " +
                "VALUES ('Rain Alert', 'legacy 1', 'high', 1, 18.4, 84.1, 'Srikakulam')"
        )
        legacy.execSQL(
            "INSERT INTO weather_alerts (title, message, severity, timestamp, latitude, longitude, cityName) " +
                "VALUES ('Rain Alert', 'legacy 2', 'high', 2, 18.4, 84.1, 'Srikakulam')"
        )
        legacy.version = 5
        legacy.close()

        val migrated = Room.databaseBuilder(context, VayuDatabase::class.java, dbFile.absolutePath)
            .addMigrations(*VayuMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val rows = migrated.weatherDao.getWeatherAlerts(50).first()
        assertEquals("no alert row may be lost", 2, rows.size)
        assertTrue("legacy rows must be expired", rows.none { it.isActive(1_700_000_000_000L) })
        assertEquals("legacy rows keep a unique fingerprint", 2, rows.map { it.fingerprint }.distinct().size)

        // The new advisory model must work on the migrated database.
        val repositoryMigrated = WeatherRepositoryImpl(
            api = noOpApi(OpenMeteoApi::class.java),
            airQualityApi = noOpApi(OpenMeteoAirQualityApi::class.java),
            geocodingApi = noOpApi(GeocodingApi::class.java),
            dao = migrated.weatherDao,
            db = migrated,
            application = context as Application
        )
        repositoryMigrated.upsertWeatherAlert(
            WeatherAlert(
                title = "Rain Alert",
                message = "fresh",
                severity = "medium",
                timestamp = 1_700_000_000_000L,
                latitude = 18.4865,
                longitude = 84.1060,
                fingerprint = "18.5|84.1|rain|2026-10-20",
                expiresAt = 4_000_000_000_000L
            )
        )
        assertEquals(3, migrated.weatherDao.getWeatherAlerts(50).first().size)
        migrated.close()
        dbFile.delete()
        Unit
    }
}
