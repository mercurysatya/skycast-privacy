package com.vayu.weather.di

import com.vayu.weather.data.remote.GeocodingApi
import com.vayu.weather.data.remote.OpenMeteoAirQualityApi
import com.vayu.weather.data.remote.OpenMeteoApi
import com.vayu.weather.data.remote.RainViewerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: android.content.Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.vayu.weather.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 10L * 1024 * 1024) // 10 MB
        val cacheControl = CacheControl.Builder()
            .maxAge(10, TimeUnit.MINUTES)  // serve from cache for 10 min
            .build()
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build()
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
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
