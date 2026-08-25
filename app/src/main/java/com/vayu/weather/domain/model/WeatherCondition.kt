package com.vayu.weather.domain.model

/**
 * Centralized WMO (World Meteorological Organization) weather code mapping.
 * Every weather code returned by Open-Meteo maps to a structured condition.
 * 
 * See: https://public.opendatahub.eu/en/dataset/open-meteo-weather-code
 */
enum class WeatherCondition(
    val code: Int,
    val title: String,
    val shortDescription: String,
    val iconName: String,
    val dayVisual: String,
    val nightVisual: String,
    val accessibilityDescription: String,
    val animationCategory: AnimationCategory
) {
    // region: WMO Weather Codes (0-99)
    // Source: Open-Metea & WMO standards

    CODE_0(0, "Clear sky", "clear sky",
        "sun", "clear sky", "clear sky",
        "Clear sky, sunny day",
        AnimationCategory.SUNNY),

    CODE_1(1, "Mainly clear", "mainly clear",
        "sun", "mainly clear", "mainly clear",
        "Mainly clear sky",
        AnimationCategory.SUNNY),

    CODE_2(2, "Partly cloudy", "partly cloudy",
        "cloud-sun", "partly cloudy", "partly cloudy",
        "Partly cloudy conditions",
        AnimationCategory.PARTLY_CLOUDY),

    CODE_3(3, "Overcast", "overcast",
        "cloud", "overcast", "overcast",
        "Overcast sky",
        AnimationCategory.CLOUDY),

    CODE_45(45, "Fog", "fog",
        "cloud-fog", "fog", "fog",
        "Foggy conditions, reduced visibility",
        AnimationCategory.FOG),

    CODE_48(48, "Depositing rime fog", "rime fog",
        "cloud-fog", "fog", "fog",
        "Rime fog depositing",
        AnimationCategory.FOG),

    CODE_51(51, "Light drizzle", "light drizzle",
        "cloud-rain", "light rain", "light rain",
        "Light drizzle",
        AnimationCategory.DRIZZLE),

    CODE_53(53, "Moderate drizzle", "moderate drizzle",
        "cloud-rain", "rain", "rain",
        "Moderate drizzle",
        AnimationCategory.DRIZZLE),

    CODE_55(55, "Dense drizzle", "dense drizzle",
        "cloud-rain", "rain", "rain",
        "Dense drizzle",
        AnimationCategory.DRIZZLE),

    CODE_56(56, "Light freezing drizzle", "light freezing drizzle",
        "cloud-rain", "freezing rain", "freezing rain",
        "Light freezing drizzle",
        AnimationCategory.FREEZING_RAIN),

    CODE_57(57, "Dense freezing drizzle", "dense freezing drizzle",
        "cloud-rain", "freezing rain", "freezing rain",
        "Dense freezing drizzle",
        AnimationCategory.FREEZING_RAIN),

    CODE_61(61, "Slight rain", "slight rain",
        "cloud-rain", "rain", "rain",
        "Slight rain",
        AnimationCategory.RAIN),

    CODE_63(63, "Moderate rain", "moderate rain",
        "cloud-rain", "rain", "rain",
        "Moderate rain",
        AnimationCategory.RAIN),

    CODE_65(65, "Heavy rain", "heavy rain",
        "cloud-rain", "rain", "rain",
        "Heavy rain",
        AnimationCategory.RAIN),

    CODE_66(66, "Light freezing rain", "light freezing rain",
        "cloud-rain", "freezing rain", "freezing rain",
        "Light freezing rain",
        AnimationCategory.FREEZING_RAIN),

    CODE_67(67, "Heavy freezing rain", "heavy freezing rain",
        "cloud-rain", "freezing rain", "freezing rain",
        "Heavy freezing rain",
        AnimationCategory.FREEZING_RAIN),

    CODE_71(71, "Slight snow fall", "slight snow",
        "cloud-snow", "snow", "snow",
        "Slight snow fall",
        AnimationCategory.SNOW),

    CODE_73(73, "Moderate snow fall", "moderate snow",
        "cloud-snow", "snow", "snow",
        "Moderate snow fall",
        AnimationCategory.SNOW),

    CODE_75(75, "Heavy snow fall", "heavy snow",
        "cloud-snow", "snow", "snow",
        "Heavy snow fall",
        AnimationCategory.SNOW),

    CODE_77(77, "Snow grains", "snow grains",
        "cloud-snow", "snow", "snow",
        "Snow grains",
        AnimationCategory.SNOW),

    CODE_80(80, "Slight rain showers", "slight rain showers",
        "cloud-rain", "rain showers", "rain showers",
        "Slight rain showers",
        AnimationCategory.RAIN),

    CODE_81(81, "Moderate rain showers", "moderate rain showers",
        "cloud-rain", "rain showers", "rain showers",
        "Moderate rain showers",
        AnimationCategory.RAIN),

    CODE_82(82, "Violent rain showers", "violent rain showers",
        "cloud-rain", "rain showers", "rain showers",
        "Violent rain showers",
        AnimationCategory.RAIN),

    CODE_85(85, "Slight snow showers", "slight snow showers",
        "cloud-snow", "snow showers", "snow showers",
        "Slight snow showers",
        AnimationCategory.SNOW),

    CODE_86(86, "Moderate snow showers", "moderate snow showers",
        "cloud-snow", "snow showers", "snow showers",
        "Moderate snow showers",
        AnimationCategory.SNOW),

    CODE_87(87, "Heavy snow showers", "heavy snow showers",
        "cloud-snow", "snow showers", "snow showers",
        "Heavy snow showers",
        AnimationCategory.SNOW),

    CODE_95(95, "Thunderstorm", "thunderstorm",
        "cloud-lightning", "thunderstorm", "thunderstorm",
        "Thunderstorm",
        AnimationCategory.THUNDERSTORM),

    CODE_96(96, "Thunderstorm with hail", "thunderstorm with hail",
        "cloud-lightning", "thunderstorm", "thunderstorm",
        "Thunderstorm with hail",
        AnimationCategory.THUNDERSTORM),

    CODE_99(99, "Thunderstorm with heavy hail", "thunderstorm with heavy hail",
        "cloud-lightning", "thunderstorm", "thunderstorm",
        "Thunderstorm with heavy hail",
        AnimationCategory.THUNDERSTORM),

    // Fallback
    UNKNOWN(999, "Unknown", "unknown",
        "cloud", "cloud", "cloud",
        "Weather condition unknown",
        AnimationCategory.UNKNOWN);
    // endregion

    companion object {
        @JvmStatic
        fun fromCode(code: Int): WeatherCondition {
            return values().associate { it.code to it }.getOrDefault(code, UNKNOWN)
        }

        @JvmStatic
        fun fromCodeSafe(code: Int): WeatherCondition {
            return when (code) {
                0 -> CODE_0
                1 -> CODE_1
                2 -> CODE_2
                3 -> CODE_3
                45 -> CODE_45
                48 -> CODE_48
                51 -> CODE_51
                53 -> CODE_53
                55 -> CODE_55
                56 -> CODE_56
                57 -> CODE_57
                61 -> CODE_61
                63 -> CODE_63
                65 -> CODE_65
                66 -> CODE_66
                67 -> CODE_67
                71 -> CODE_71
                73 -> CODE_73
                75 -> CODE_75
                77 -> CODE_77
                80 -> CODE_80
                81 -> CODE_81
                82 -> CODE_82
                85 -> CODE_85
                86 -> CODE_86
                87 -> CODE_87
                95 -> CODE_95
                96 -> CODE_96
                99 -> CODE_99
                else -> UNKNOWN
            }
        }
    }
}

enum class AnimationCategory {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    FREEZING_RAIN,
    SNOW,
    THUNDERSTORM,
    UNKNOWN
}