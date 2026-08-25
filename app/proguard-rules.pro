# ProGuard rules for Vayu Weather application

# Keep Hilt related classes
-keep class com.vayu.weather.** { *; }
-keep class * { @dagger.hilt.android.Hilt *; }
-keep class * { @Inject *; }
-keep class * { implements android.os.Parcelable; }

# Keep Moshi serialization
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * { @com.squareup.moshi.Json *; }

# Keep Kotlinx Serialization
-keep class org.jetbrains.kotlinx.serialization.** { *; }

# Keep WorkManager workers
-keep class com.vayu.weather.data.worker.** { *; }
-keep class androidx.work.Worker { *; }

# Keep Retrofit interfaces
-keepclassmembers class * {
    @retrofit2.http.* *;
}

# Keep OkHttp interceptors
-keepclassmembers class com.squareup.okhttp3.** { *; }

# Keep Room entities and DAOs
-keep abstract class * extends androidx.room.RoomDatabase { *; }
-keep class * { implements android.database.CursorWrapper; }
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.DatabaseIndex *; }

# Keep Apache common logging
-keep class org.apache.commons.** { *; }

# Keep Glance widgets
-keep class androidx.glance.** { *; }

# Keep AdMob classes
-keep class com.google.android.gms.ads.** { *; }
-keep class * { @com.google.android.gms.ads.* *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*

# Keep permission and permissions related classes
-keep class android.Manifest { *; }

# Keep location related classes
-keepclassmembers class com.google.android.gms.location.** { *; }

# Keep WeatherCondition enum
-keepclassmembers enum WeatherCondition {
    *;
    public *;
}

# Suppress R8 warning about Play Services Location companion object
-dontwarn com.google.android.gms.internal.location.zze
-dontwarn com.google.android.gms.internal.location.**

# Suppress Compose Glance widget stack trace mapping warning
-keep class com.vayu.weather.presentation.widget.WeatherWidget { *; }
-keep class com.vayu.weather.presentation.widget.WeatherWidgetReceiver { *; }

# Keep Glance classes
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }

# Keep Weather models
-keep class com.vayu.weather.domain.model.WeatherInfo { *; }
-keep class com.vayu.weather.domain.model.WeatherCondition { *; }
-keep class com.vayu.weather.domain.model.AirQuality { *; }
-keep class com.vayu.weather.domain.model.WeatherAlert { *; }
-keep class com.vayu.weather.domain.model.WeatherHistorySnapshot { *; }
-keep class com.vayu.weather.domain.model.WeatherHistoryDay { *; }
-keep class com.vayu.weather.domain.model.City { *; }

# Keep DataStore preferences
-keep class com.vayu.weather.data.local.SettingsManager { *; }
-keep class * { @org.jetbrains.annotations.NonNull *; }
-keep class * { @org.jetbrains.annotations.NotNull *; }

# Keep ViewModels
-keep class com.vayu.weather.presentation.** { *; }

# Keep navigation graph
-keep class androidx.navigation.** { *; }



# Keep serialization of domain models
-keep @com.fasterxml.jackson.annotation.JsonPropertyOrder class *
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

# Keep custom application class
-keep class com.vayu.weather.VayuApplication { *; }