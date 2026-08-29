package com.vayu.weather.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SkyCast Design System — central token registry.
 *
 * Use these tokens throughout the app instead of hard-coded values to keep the
 * UI visually consistent. Token groups cover colors, spacing, shape, elevation,
 * typography, animation and semantic weather helpers.
 */
object SkyCastTokens {
    // ── Spacing scale (4-pt grid) ──
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space6 = 6.dp
    val Space8 = 8.dp
    val Space10 = 10.dp
    val Space12 = 12.dp
    val Space14 = 14.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space28 = 28.dp
    val Space32 = 32.dp
    val Space40 = 40.dp
    val Space48 = 48.dp
    val Space64 = 64.dp

    // ── Corner radii ──
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val Radius2xl = 24.dp
    val Radius3xl = 28.dp
    val RadiusPill = 50.dp

    // ── Elevation ──
    val ElevationNone = 0.dp
    val ElevationSubtle = 1.dp
    val ElevationLow = 2.dp
    val ElevationMedium = 4.dp
    val ElevationHigh = 8.dp
    val ElevationHero = 12.dp

    // ── Touch targets ──
    val TouchTargetMin = 44.dp
    val TouchTargetComfortable = 48.dp
    val TouchTargetLarge = 56.dp

    // ── Icon sizes ──
    val IconXs = 12.dp
    val IconSm = 16.dp
    val IconMd = 20.dp
    val IconLg = 24.dp
    val IconXl = 32.dp
    val Icon2xl = 48.dp
    val IconHero = 64.dp

    // ── Stroke / divider widths ──
    val StrokeHairline = 1.dp
    val StrokeMedium = 2.dp
    val StrokeThick = 3.dp
    val StrokeXThick = 4.dp

    // ── Card paddings ──
    val CardPadCompact = 12.dp
    val CardPadDefault = 16.dp
    val CardPadRoomy = 20.dp
}

/**
 * Helpers for computing common UI state colors.
 */
object SkyCastColors {
    /** Returns an AQI color by US-AQI value. */
    fun forUsAqi(aqi: Int): Color = when {
        aqi <= 50 -> AqiGood
        aqi <= 100 -> AqiFair
        aqi <= 150 -> AqiModerate
        aqi <= 200 -> AqiPoor
        aqi <= 300 -> AqiVeryPoor
        else -> AqiSevere
    }

    /** Returns an AQI color by European AQI value (0-100 scale). */
    fun forEuAqi(aqi: Int): Color = when {
        aqi <= 20 -> AqiGood
        aqi <= 40 -> AqiFair
        aqi <= 60 -> AqiModerate
        aqi <= 80 -> AqiPoor
        else -> AqiVeryPoor
    }

    /** Returns a UV-index color. */
    fun forUvIndex(uv: Double): Color = when {
        uv < 3.0 -> UvLow
        uv < 6.0 -> UvModerate
        uv < 8.0 -> UvHigh
        uv < 11.0 -> UvVeryHigh
        else -> UvExtreme
    }

    /** Returns a Beaufort-style severity color for wind in km/h. */
    fun forWindKph(kph: Double): Color = when {
        kph < 12 -> FreshGreen
        kph < 30 -> AqiFair
        kph < 50 -> UvModerate
        kph < 75 -> UvHigh
        kph < 100 -> UvVeryHigh
        else -> UvExtreme
    }
}

/**
 * Map weather codes (Open-Meteo) to a canonical condition category used by
 * SkyCast components. This keeps the rest of the app provider-agnostic.
 */
enum class SkyCondition {
    Clear, PartlyCloudy, Cloudy, Fog,
    Drizzle, Rain, HeavyRain, Snow, Thunder, Unknown;

    companion object {
        fun fromWmo(code: Int, isDay: Boolean): SkyCondition = when (code) {
            0 -> if (isDay) Clear else Clear
            1 -> PartlyCloudy
            2 -> PartlyCloudy
            3 -> Cloudy
            45, 48 -> Fog
            51, 53, 55 -> Drizzle
            56, 57 -> Drizzle
            61, 63, 65 -> Rain
            66, 67 -> Rain
            71, 73, 75 -> Snow
            77 -> Snow
            80, 81, 82 -> HeavyRain
            85, 86 -> Snow
            95, 96, 99 -> Thunder
            else -> Unknown
        }
    }
}

/**
 * Severity levels used by the alert system.
 */
enum class SkyAlertSeverity(val label: String) {
    Info("Information"),
    Advisory("Advisory"),
    Watch("Watch"),
    Warning("Warning"),
    Emergency("Emergency");

    fun color(): Color = when (this) {
        Info -> SeverityInfo
        Advisory -> SeverityAdvisory
        Watch -> SeverityWatch
        Warning -> SeverityWarning
        Emergency -> SeverityEmergency
    }
}
