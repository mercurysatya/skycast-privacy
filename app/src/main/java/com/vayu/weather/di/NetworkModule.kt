package com.vayu.weather.di

import com.vayu.weather.data.remote.GeocodingApi
import com.vayu.weather.data.remote.OpenMeteoAirQualityApi
import com.vayu.weather.data.remote.OpenMeteoApi
import com.vayu.weather.data.remote.RainViewerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.vayu.weather.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private fun createRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideOpenMeteoApi(okHttpClient: OkHttpClient): OpenMeteoApi =
        createRetrofit("https://api.open-meteo.com/", okHttpClient)
            .create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    fun provideAirQualityApi(okHttpClient: OkHttpClient): OpenMeteoAirQualityApi =
        createRetrofit("https://air-quality-api.open-meteo.com/", okHttpClient)
            .create(OpenMeteoAirQualityApi::class.java)

    @Provides
    @Singleton
    fun provideGeocodingApi(okHttpClient: OkHttpClient): GeocodingApi =
        createRetrofit("https://geocoding-api.open-meteo.com/", okHttpClient)
            .create(GeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideRainViewerApi(okHttpClient: OkHttpClient): RainViewerApi =
        createRetrofit("https://api.rainviewer.com/", okHttpClient)
            .create(RainViewerApi::class.java)
}
