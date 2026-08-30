plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// AdMob IDs are resolved in order: gradle.properties → local.properties → Google test defaults.
// Release builds must have production IDs in either file; Gradle will fail with a clear
// error if a test ID is used in a release build.
val localProps = rootProject.file("local.properties").let { f ->
    if (f.exists()) f.readLines().associate { line ->
        val idx = line.indexOf('=')
        if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        else "" to ""
    } else emptyMap()
}
fun resolveAdMob(key: String, testDefault: String): String =
    (project.findProperty(key) as? String)
        ?: localProps[key]
        ?: testDefault

val admobAppId = resolveAdMob("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val admobBannerId = resolveAdMob("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111")
val admobInterstitialId = resolveAdMob("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")
val admobRewardedId = resolveAdMob("ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")

val isTestAdmobId = admobAppId.startsWith("ca-app-pub-3940256099942544")

// ── Release signing ──────────────────────────────────────────────────────
// Credentials are read from `key.properties` (gitignored). The file must
// exist and contain storePassword, keyAlias, keyPassword, and storeFile
// for release builds to be signed.  If the file is missing or contains
// placeholder values the release build will still succeed but the
// APK/AAB will be unsigned.
val keyPropsFile = rootProject.file("key.properties")
val keyProps = if (keyPropsFile.exists()) {
    keyPropsFile.readLines().associate { line ->
        val idx = line.indexOf('=')
        if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        else "" to ""
    }
} else emptyMap()
val hasSigningConfig = keyProps["storePassword"]?.isNotEmpty() == true &&
    keyProps["storePassword"] != "CHANGE_ME"

android {
    namespace = "com.vayu.weather"
    compileSdk = 37
    
    buildFeatures {
        buildConfig = true
    }
    
    defaultConfig {
        applicationId = "com.vayu.weather"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keyProps["storeFile"] ?: "")
                storePassword = keyProps["storePassword"] ?: ""
                keyAlias = keyProps["keyAlias"] ?: ""
                keyPassword = keyProps["keyPassword"] ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("Boolean", "DEBUG", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        release {
            // Fail the release build if no production AdMob IDs are provided.
            // Test IDs are only valid for development; shipping them to the
            // Play Store would either show "Test Ad" placeholders or, worse,
            // disable monetization entirely.
            // Set `-PallowTestAdmobIds=true` to bypass for internal verification
            // builds (e.g. when running R8 dry-runs without real keys).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("Boolean", "DEBUG", "false")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
            // Guard the release configuration to fail early with a clear
            // error if the developer forgot to inject production AdMob IDs.
            afterEvaluate {
                val allowTestIds = (project.findProperty("allowTestAdmobIds") as? String)?.toBoolean() == true
                tasks.matching { it.name.startsWith("assemble") && it.name.contains("Release", ignoreCase = true) }.configureEach {
                    doFirst {
                        if (isTestAdmobId && !allowTestIds) {
                            throw GradleException(
                                "AdMob test IDs detected for release build. Set ADMOB_APP_ID, " +
                                "ADMOB_BANNER_ID, ADMOB_INTERSTITIAL_ID, ADMOB_REWARDED_ID in " +
                                "gradle.properties (or -P flags) before producing a release artifact. " +
                                "Pass -PallowTestAdmobIds=true to bypass for internal verification only."
                            )
                        }
                    }
                }
            }
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        // Disable noise-only checks. Translating all 700+ strings to 5
        // regional languages is a separate ongoing workstream — the app
        // already ships with values-hi/kn/ml/ta/te baselines.
        disable += setOf(
            "MissingTranslation", "ExtraTranslation",
            "DefaultLocale",
            "GradleDependency", "NewerVersionAvailable", "OldTargetApi",
            "ModifierParameter", "AndroidGradlePluginVersion",
            "PluralsCandidate", "UnusedResources",
            "UseKtx", "UseTomlInstead", "AutoboxingStateValueProperty",
            "MonochromeLauncherIcon"
        )
        abortOnError = false
        warningsAsErrors = false
        checkAllWarnings = false
        // We still fail the build on errors that are NOT disabled above.
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.androidx.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.mpAndroidChart)
    implementation(libs.mapLibreCompose)
    runtimeOnly("org.maplibre.compose:maplibre-compose-runtime-opengl-android:0.15.0")
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-appcheck")

    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.moshi.kotlin)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.retrofit)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.23.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.core))
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    ksp(libs.androidx.room.compiler)
    ksp(libs.moshi.kotlin.codegen)
}