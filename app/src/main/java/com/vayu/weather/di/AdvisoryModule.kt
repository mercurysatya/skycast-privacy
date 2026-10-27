package com.vayu.weather.di

import com.vayu.weather.data.local.ResWeatherAdvisoryTextProvider
import com.vayu.weather.domain.model.WeatherAdvisoryTextProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdvisoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherAdvisoryTextProvider(
        resWeatherAdvisoryTextProvider: ResWeatherAdvisoryTextProvider
    ): WeatherAdvisoryTextProvider
}
