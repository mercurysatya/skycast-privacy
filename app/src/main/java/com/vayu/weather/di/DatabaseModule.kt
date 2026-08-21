package com.vayu.weather.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vayu.weather.data.local.VayuDatabase
import com.vayu.weather.data.local.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
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

    @Provides
    @Singleton
    fun provideVayuDatabase(app: Application): VayuDatabase {
        return Room.databaseBuilder(
            app,
            VayuDatabase::class.java,
            "vayu_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(db: VayuDatabase): WeatherDao {
        return db.weatherDao
    }
}
