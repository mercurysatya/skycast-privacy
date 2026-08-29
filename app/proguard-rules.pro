# ProGuard rules for Vayu Weather application

# Hilt — required. Hilt generates the @AndroidEntryPoint subclasses; we
# keep the user-visible @HiltAndroidApp class and the @Inject constructors
# but do not blanket-keep the whole package.
-keep class * { @dagger.hilt.android.Hilt *; }
-keep class * { @dagger.hilt.android.AndroidEntryPoint *; }
-keep class com.vayu.weather.VayuApplication { *; }
-keep class * { @Inject *; }
-keep class * { implements android.os.Parcelable; }

# Moshi serialization — Moshi uses reflection on @Json fields.
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * { @com.squareup.moshi.Json *; }

# Kotlinx Serialization
-keep class org.jetbrains.kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# WorkManager workers (instantiated by name).
-keep class com.vayu.weather.data.worker.** { *; }
-keep class androidx.work.Worker { *; }

# Retrofit interfaces (uses reflection on @GET/@POST/etc).
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp platform shims referenced dynamically.
-keepclassmembers class com.squareup.okhttp3.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**

# Room entities and DAOs — Room generates the impl classes and looks them
# up by name.
-keep abstract class * extends androidx.room.RoomDatabase { *; }
-keep class * { implements android.database.CursorWrapper; }
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.DatabaseIndex *; }
-keep @androidx.room.Dao class * { *; }

# Apache common logging (used by Retrofit/OkHttp on certain platforms).
-keep class org.apache.commons.logging.** { *; }
-dontwarn org.apache.commons.logging.**

# Glance widgets — instantiated by the system by class name.
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }
-keep class com.vayu.weather.presentation.widget.WeatherWidget { *; }
-keep class com.vayu.weather.presentation.widget.WeatherWidgetReceiver { *; }

# AdMob — the SDK uses reflection on its own classes; without these the
# SDK throws NoSuchMethodError in release.
-keep class com.google.android.gms.ads.** { *; }
-keep class * { @com.google.android.gms.ads.* *; }

# Firebase — the SDK reads its own annotations at runtime.
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keep class com.google.android.gms.internal.ads.** { *; }

# Permission/manifest constants
-keep class android.Manifest { *; }

# Play Services Location
-keepclassmembers class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.internal.location.**

# Domain models annotated with @Serializable — we want them shrunk
# and renamed, but their field names matter for JSON; the kotlinx
# rules above are sufficient.
-keepclassmembers class com.vayu.weather.domain.model.** {
    public *;
}

# ViewModels are Hilt-managed; nothing to keep explicitly.

# MapLibre — loads native code + uses reflection in some encoders.
-keep class org.maplibre.android.** { *; }
-keep class org.maplibre.compose.** { *; }
-dontwarn org.maplibre.android.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.flow.**

# DataStore
-keep class androidx.datastore.** { *; }

# Joda-Time alternatives
-dontwarn java.lang.invoke.StringConcatFactory

# Notifications
-keep class androidx.core.app.NotificationCompat { *; }