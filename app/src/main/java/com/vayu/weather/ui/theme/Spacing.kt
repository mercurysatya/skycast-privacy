package com.vayu.weather.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design system spacing constants.
 * Use these consistently throughout the app instead of hardcoded values.
 */
object WeatherSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val hero = 48.dp
}

/**
 * Design system shape constants.
 */
object WeatherShapes {
    val cardSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val cardMedium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val cardLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    val cardXL = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
    val button = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
}

/**
 * Design system animation durations (in milliseconds).
 */
object WeatherAnimation {
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val STAGGER_DELAY = 80
}

/**
 * Design system opacity constants for glass effects.
 */
object WeatherOpacity {
    const val GLASS_LIGHT = 0.10f
    const val GLASS_MEDIUM = 0.15f
    const val GLASS_HEAVY = 0.25f
    const val TEXT_PRIMARY = 0.90f
    const val TEXT_SECONDARY = 0.70f
    const val TEXT_TERTIARY = 0.50f
    const val TEXT_DISABLED = 0.35f
    const val DIVIDER = 0.08f
    const val ICON = 0.80f
    const val ICON_SECONDARY = 0.60f
}

/**
 * Weather condition color tokens.
 */
object WeatherColors {
    val sunny = androidx.compose.ui.graphics.Color(0xFFFBBF24)
    val rainy = androidx.compose.ui.graphics.Color(0xFF38BDF8)
    val stormy = androidx.compose.ui.graphics.Color(0xFF818CF8)
    val snowy = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
    val foggy = androidx.compose.ui.graphics.Color(0xFF94A3B8)
    val night = androidx.compose.ui.graphics.Color(0xFF1E1B4B)
    val uvLow = androidx.compose.ui.graphics.Color(0xFF22C55E)
    val uvModerate = androidx.compose.ui.graphics.Color(0xFFFBBF24)
    val uvHigh = androidx.compose.ui.graphics.Color(0xFFF97316)
    val uvVeryHigh = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val uvExtreme = androidx.compose.ui.graphics.Color(0xFF9333EA)
    val aqiGood = androidx.compose.ui.graphics.Color(0xFF22C55E)
    val aqiFair = androidx.compose.ui.graphics.Color(0xFF84CC16)
    val aqiModerate = androidx.compose.ui.graphics.Color(0xFFFBBF24)
    val aqiPoor = androidx.compose.ui.graphics.Color(0xFFF97316)
    val aqiVeryPoor = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val aqiSevere = androidx.compose.ui.graphics.Color(0xFFBE185D)
}
