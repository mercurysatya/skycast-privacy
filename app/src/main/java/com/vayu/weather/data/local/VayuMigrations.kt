package com.vayu.weather.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The database migrations of the app.
 *
 * They live here (instead of inside the Hilt module) so that the migration
 * tests can exercise exactly the same code that runs on a user's device.
 */
internal object VayuMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
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
            """.trimIndent())
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
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
            """.trimIndent())
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
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
            """.trimIndent())
        }
    }

    /**
     * v4 -> v5: adds the favorites-column for drag-reorder. Non-destructive:
     * existing rows inherit their insertion order via rowid, so the list the
     * user already has keeps the same visual order after the upgrade.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE favorite_cities ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE favorite_cities SET sortIndex = rowid")
        }
    }

    /**
     * v5 -> v6: gives weather alerts a stable identity and a validity window.
     *
     * Non-destructive: no alert row is deleted. Legacy rows get a unique
     * placeholder fingerprint (so the new unique index can be created) and
     * `expiresAt = 0`, which marks them as expired — they stay visible in the
     * alert history but no longer count as active. The worker/evaluator then
     * writes one fresh advisory for the current forecast.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE weather_alerts ADD COLUMN fingerprint TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE weather_alerts ADD COLUMN expiresAt INTEGER NOT NULL DEFAULT 0")
            // Every legacy row needs its own fingerprint value for the unique
            // index to be creatable.
            db.execSQL("UPDATE weather_alerts SET fingerprint = 'legacy-' || id WHERE fingerprint = ''")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_weather_alerts_fingerprint " +
                    "ON weather_alerts(fingerprint)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}
