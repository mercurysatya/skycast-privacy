package com.vayu.weather.di

import com.vayu.weather.data.local.SettingsManager
import com.vayu.weather.data.repository.WeatherRepositoryImpl
import com.vayu.weather.domain.repository.UserDataStore
import com.vayu.weather.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindUserDataStore(
        settingsManager: SettingsManager
    ): UserDataStore
}
