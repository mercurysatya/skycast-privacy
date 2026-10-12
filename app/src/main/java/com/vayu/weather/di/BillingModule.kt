package com.vayu.weather.di

import com.vayu.weather.data.billing.BillingGateway
import com.vayu.weather.data.billing.BillingProductConfig
import com.vayu.weather.data.billing.BillingProducts
import com.vayu.weather.data.billing.PlayBillingGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindBillingGateway(
        playBillingGateway: PlayBillingGateway
    ): BillingGateway

    @Binds
    @Singleton
    abstract fun bindBillingProducts(
        billingProducts: BillingProducts
    ): BillingProductConfig
}
