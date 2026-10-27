package com.vayu.weather.di

import android.app.Application
import androidx.room.Room
import com.vayu.weather.data.local.VayuDatabase
import com.vayu.weather.data.local.VayuMigrations
import com.vayu.weather.data.local.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVayuDatabase(app: Application): VayuDatabase {
        return Room.databaseBuilder(
            app,
            VayuDatabase::class.java,
            "vayu_db"
        )
            .addMigrations(*VayuMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(db: VayuDatabase): WeatherDao {
        return db.weatherDao
    }
}
