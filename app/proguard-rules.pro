# Kotlin
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-dontwarn kotlin.**

# Kotlin Serialization
-keepclassmembers class kotlinx.serialization.** { *; }
-keep,includedescriptorclasses class com.vayu.weather.**$$serializer { *; }
-keepclassmembers class com.vayu.weather.domain.model.** { *; }
-keepclassmembers class com.vayu.weather.data.remote.dto.** { *; }
-keepclassmembers class com.vayu.weather.data.local.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keepclassmembers class com.vayu.weather.data.remote.dto.** { *; }
-keepclassmembers class com.vayu.weather.domain.model.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class com.vayu.weather.data.remote.** { *; }

# OkHttp / Logging
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# MapLibre
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Google Play Services (Ads, Location)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Glance (App Widget)
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Compose
-dontwarn androidx.compose.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Strip Log calls in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
