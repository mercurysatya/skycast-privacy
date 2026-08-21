package com.vayu.weather.di

import android.app.Application
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.vayu.weather.data.location.DefaultLocationTracker
import com.vayu.weather.domain.location.LocationTracker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationTracker(defaultLocationTracker: DefaultLocationTracker): LocationTracker

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(app: Application): FusedLocationProviderClient {
            return LocationServices.getFusedLocationProviderClient(app)
        }
    }
}
