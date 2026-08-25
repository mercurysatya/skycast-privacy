package com.vayu.weather.data.converter

import com.vayu.weather.presentation.weather.TemperatureUnit
import com.vayu.weather.presentation.weather.WindUnit

/**
 * Centralized unit conversion layer.
 * Normalizes SI values internally and converts only for presentation.
 * Never modifies raw provider values.
 */

/** Temperature conversions */
object UnitsConverter {

    /** Convert Celsius to Fahrenheit */
    @JvmStatic
    fun celsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9.0 / 5.0) + 32.0
    }

    /** Convert Fahrenheit to Celsius */
    @JvmStatic
    fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return (fahrenheit - 32.0) * 5.0 / 9.0
    }

    /** Get display string for temperature based on unit */
    @JvmStatic
    fun getTemperatureDisplay(
        temperature: Double,
        unit: TemperatureUnit
    ): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "${temperature.toInt()}\u00B0C"
            TemperatureUnit.FAHRENHEIT -> "${celsiusToFahrenheit(temperature).toInt()}\u00B0F"
        }
    }

    /** Get temperature unit label */
    @JvmStatic
    fun getTemperatureUnitLabel(unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "\u00B0C"
            TemperatureUnit.FAHRENHEIT -> "\u00B0F"
        }
    }
}

/** Wind speed conversions */
object WindConverter {

    /** Convert km/h to mph */
    @JvmStatic
    fun kmhToMph(kmh: Double): Double {
        return kmh * 0.621371
    }

    /** Convert mph to km/h */
    @JvmStatic
    fun mphToKmh(mph: Double): Double {
        return mph / 0.621371
    }

    /** Convert km/h to m/s */
    @JvmStatic
    fun kmhToMs(kmh: Double): Double {
        return kmh / 3.6
    }

    /** Convert m/s to km/h */
    @JvmStatic
    fun msToKmh(ms: Double): Double {
        return ms * 3.6
    }

    /** Convert km/h to knots */
    @JvmStatic
    fun kmhToKnots(kmh: Double): Double {
        return kmh * 0.539957
    }

    /** Convert knots to km/h */
    @JvmStatic
    fun knotsToKmh(knots: Double): Double {
        return knots / 0.539957
    }

    /** Get wind speed display string based on unit */
    @JvmStatic
    fun getWindDisplay(
        speedKmh: Double?,
        unit: WindUnit
    ): String {
        return when (unit) {
            WindUnit.KPH -> speedKmh?.let { "${it.toInt()} km/h" } ?: "--"
            WindUnit.MPH -> speedKmh?.let { "${kmhToMph(it).toInt()} mph" } ?: "--"
            WindUnit.MS -> speedKmh?.let { "${kmhToMs(it).toInt()} m/s" } ?: "--"
            WindUnit.KNOTS -> speedKmh?.let { "${kmhToKnots(it).toInt()} knots" } ?: "--"
        }
    }

    /** Get wind unit label */
    @JvmStatic
    fun getWindUnitLabel(unit: WindUnit): String {
        return when (unit) {
            WindUnit.KPH -> "km/h"
            WindUnit.MPH -> "mph"
            WindUnit.MS -> "m/s"
            WindUnit.KNOTS -> "knots"
        }
    }
}

/** Pressure conversions */
object PressureConverter {

    /** Convert hPa to inHg */
    @JvmStatic
    fun hpaToInHg(hpa: Double): Double {
        return hpa / 33.8639
    }

    /** Convert inHg to hPa */
    @JvmStatic
    fun inHgToHpa(inHg: Double): Double {
        return inHg * 33.8639
    }

    /** Get pressure display string based on unit */
    @JvmStatic
    fun getPressureDisplay(
        pressureHpa: Double?,
        unit: PressureUnit
    ): String {
        return when (unit) {
            PressureUnit.HPA -> pressureHpa?.let { "${it.toInt()} hPa" } ?: "--"
            PressureUnit.INHG -> pressureHpa?.let { "${hpaToInHg(it).toInt()} inHg" } ?: "--"
        }
    }

    /** Get pressure unit label */
    @JvmStatic
    fun getPressureUnitLabel(unit: PressureUnit): String {
        return when (unit) {
            PressureUnit.HPA -> "hPa"
            PressureUnit.INHG -> "inHg"
        }
    }
}

/** Precipitation conversions */
object PrecipitationConverter {

    /** Convert mm to inches */
    @JvmStatic
    fun mmToInches(mm: Double): Double {
        return mm / 25.4
    }

    /** Convert inches to mm */
    @JvmStatic
    fun inchesToMm(inches: Double): Double {
        return inches * 25.4
    }

    /** Get precipitation display string */
    @JvmStatic
    fun getPrecipitationDisplay(
        amountMm: Double?,
        unit: PrecipitationUnit
    ): String {
        return when (unit) {
            PrecipitationUnit.MM -> amountMm?.let { "${it.toInt()} mm" } ?: "--"
            PrecipitationUnit.INCH -> amountMm?.let { "${mmToInches(it).toInt()} in" } ?: "--"
        }
    }

    /** Get precipitation unit label */
    @JvmStatic
    fun getPrecipitationUnitLabel(unit: PrecipitationUnit): String {
        return when (unit) {
            PrecipitationUnit.MM -> "mm"
            PrecipitationUnit.INCH -> "inches"
        }
    }
}

/** Enums for presentation units */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

enum class WindUnit {
    KPH,
    MPH,
    MS,
    KNOTS
}

enum class PressureUnit {
    HPA,
    INHG
}

enum class PrecipitationUnit {
    MM,
    INCH
}