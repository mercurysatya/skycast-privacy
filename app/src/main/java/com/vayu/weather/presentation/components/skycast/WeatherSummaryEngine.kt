package com.vayu.weather.presentation.components.skycast

import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.HourlyWeather
import com.vayu.weather.domain.model.WeatherInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Deterministic natural-language weather summary generator.
 *
 * Produces human-friendly sentences from structured weather data without
 * requiring an external AI service. Designed for an English locale;
 * translations can be added later by mapping the [Sentence]s to a string
 * resource set.
 */
object WeatherSummaryEngine {

    /** A discrete summary chunk the UI can render. */
    data class Sentence(val text: String, val emphasis: Emphasis = Emphasis.NORMAL) {
        enum class Emphasis { NORMAL, EMPHASIS }
    }

    fun summarize(
        info: WeatherInfo,
        isCelsius: Boolean,
        locale: Locale = Locale.EN
    ): List<Sentence> {
        val sentences = mutableListOf<Sentence>()
        val current = info.current
        val today = info.daily.firstOrNull()
        // Use the API's current time (in the weather location's timezone)
        // instead of LocalDateTime.now() (device timezone) so time-of-day
        // descriptions match the weather location.
        val locationTime = try {
            LocalDateTime.parse(current.time, DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.now()
        }
        val now = locationTime
        // Use the data's isDay flag when available so the summary matches the
        // conditions, not the device wall clock (which is important for
        // backfill / cache scenarios and for predictability in tests).
        val isDay = current.isDay
        val timeOfDay = if (!isDay) "tonight" else describeTimeOfDay(now.hour)

        val temp = convertTemp(current.temperature, isCelsius)
        val feels = current.apparentTemperature?.let { convertTemp(it, isCelsius) }
        val isRainy = current.weatherCode in 51..82 || current.weatherCode in 95..99
        val isThunder = current.weatherCode in 95..99
        val isFog = current.weatherCode in 45..48
        val isSnow = current.weatherCode in 71..86
        val isClear = current.weatherCode == 0
        val isCloudy = current.weatherCode in 2..3
        val isWindy = (current.windSpeed ?: 0.0) >= 30
        val isHumid = (current.humidity ?: 50.0) >= 70
        val isDry = (current.humidity ?: 50.0) < 30

        // Opening line — current state. Distinguish day vs night so a clear
        // night reads "Clear skies tonight" instead of "Clear skies this morning".
        val opening = when {
            isThunder -> "Thunderstorms $timeOfDay — stay indoors if possible."
            isRainy && isWindy -> "Rain and gusty winds $timeOfDay."
            isRainy -> "Wet weather $timeOfDay. Keep an umbrella handy."
            isFog -> "Foggy conditions $timeOfDay — visibility may be reduced."
            isSnow -> "Snow $timeOfDay — bundle up and allow extra travel time."
            isClear && isDay -> "Clear skies ${describeTimeOfDay(now.hour)}. Pleasant to be outside."
            isClear -> "Clear skies $timeOfDay."
            isCloudy && isDay -> "Cloudy skies ${describeTimeOfDay(now.hour)}."
            isCloudy -> "Cloudy skies $timeOfDay."
            current.weatherCode == 1 && isDay -> "Mostly clear with a few clouds ${describeTimeOfDay(now.hour)}."
            current.weatherCode == 1 -> "Mostly clear with a few clouds $timeOfDay."
            isDay -> "Mild conditions ${describeTimeOfDay(now.hour)}."
            else -> "Mild conditions $timeOfDay."
        }
        sentences += Sentence(opening, Sentence.Emphasis.EMPHASIS)

        // Feels-like callout.
        if (feels != null && kotlin.math.abs(feels - temp) >= 3) {
            val direction = if (feels > temp) "warmer" else "cooler"
            sentences += Sentence(
                "It feels like ${feels}° — $direction than the actual air temperature."
            )
        }

        // Current temperature in user-selected unit.
        sentences += Sentence("It's $temp° right now.")

        // High/low.
        if (today != null) {
            val high = convertTemp(today.maxTemp, isCelsius)
            val low = convertTemp(today.minTemp, isCelsius)
            sentences += Sentence("Today: high ${high}°, low ${low}°.")
        }

        // Next 6-hour rain outlook.
        val nextRainy = findNextRainyHour(info.hourly, now)
        if (nextRainy != null && !isRainy) {
            val label = relativeMinutesLabel(nextRainy.minutesFromNow)
            sentences += Sentence(
                "Rain is expected $label (${nextRainy.probability}% chance)."
            )
        }

        // Wind callout.
        if (isWindy) {
            val speed = (current.windSpeed ?: 0.0).roundToInt()
            sentences += Sentence("Winds around $speed km/h — secure loose outdoor items.")
        }

        // Humidity callout — only when it does not contradict the current state.
        // In rain/snow/storm scenarios, "dry air" or "humid air" both feel wrong.
        if (!isRainy && !isSnow && !isThunder) {
            when {
                isHumid -> sentences += Sentence("Humid air — it may feel stickier than the thermometer shows.")
                isDry -> sentences += Sentence("Dry air — stay hydrated and consider a moisturizer.")
            }
        }

        return sentences
    }

    /**
     * Information-rich summary for the dashboard hero.
     *
     * Returns a primary sentence (the headline) and a secondary detail
     * (typically today's H/L plus an outlook for later in the day). The
     * primary string is constructed from actual data — no fabricated
     * predictions, no contradictions.
     */
    data class DetailedSummary(
        val primary: String,
        val secondary: String?
    )

    fun summarizeDetailed(info: WeatherInfo, isCelsius: Boolean): DetailedSummary {
        val current = info.current
        val today = info.daily.firstOrNull()
        val high = today?.maxTemp?.let { convertTemp(it, isCelsius) }
        val low = today?.minTemp?.let { convertTemp(it, isCelsius) }

        val code = current.weatherCode
        val isDay = current.isDay
        val isRainy = code in 51..82 || code in 95..99
        val isThunder = code in 95..99
        val isSnow = code in 71..86

        // Use the API's current time (in the weather location's timezone)
        val locationNow = try {
            LocalDateTime.parse(current.time, DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.now()
        }

        // Find next significant rain window (>= 40% probability)
        val nextRain = findNextRainyHour(info.hourly, locationNow)
        val rainIn = nextRain?.let { relativeMinutesLabel(it.minutesFromNow) }

        val condition = localizedCondition(code, isDay)

        // Build the primary sentence. The structure is:
        //   <Condition now> [with a <rain chance> after <time>] [temperatures <range>]
        // We compose the sentence from non-contradictory clauses only.
        val parts = mutableListOf<String>()
        parts += "$condition."

        val rainMention = when {
            isThunder -> "Thunderstorms ongoing — stay indoors if you can."
            isRainy -> "Wet weather expected to continue."
            isSnow -> "Snow expected to continue."
            rainIn != null && nextRain != null -> "A $rainIn, expect rain (${nextRain.probability}% chance)."
            else -> null
        }
        rainMention?.let { parts += it }

        if (high != null && low != null) {
            parts += "Temperatures will reach $high° today, falling to $low° overnight."
        } else if (high != null) {
            parts += "Temperatures will reach $high° today."
        } else if (low != null) {
            parts += "Low $low° expected."
        }

        val primary = parts.joinToString(" ")

        // Secondary detail: feels-like + UV hint
        val feels = current.apparentTemperature?.let { convertTemp(it, isCelsius) }
        val uv = today?.uvIndex?.roundToInt() ?: 0
        val secondaryParts = mutableListOf<String>()
        if (feels != null && kotlin.math.abs(feels - (high ?: low ?: feels)) >= 3) {
            secondaryParts += "Feels like $feels°."
        }
        if (uv >= 6) {
            secondaryParts += "UV is high — sunscreen recommended."
        }
        val secondary = secondaryParts.takeIf { it.isNotEmpty() }?.joinToString(" ")

        return DetailedSummary(primary = primary, secondary = secondary)
    }

    private fun localizedCondition(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Clear skies" else "Clear skies tonight"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Heavy drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        80 -> "Rain showers"
        81 -> "Heavy showers"
        82 -> "Violent showers"
        95 -> "Thunderstorms"
        96, 99 -> "Thunderstorms with hail"
        else -> "Cloudy"
    }

    data class Locale(val name: String) {
        companion object { val EN = Locale("en") }
    }

    private data class NextRain(
        val minutesFromNow: Int,
        val probability: Int
    )

    private fun findNextRainyHour(hourly: List<HourlyWeather>, now: LocalDateTime = LocalDateTime.now()): NextRain? {
        hourly.sortedBy { it.time }.forEach { h ->
            val time = parseHourly(h.time) ?: return@forEach
            if (time.isBefore(now)) return@forEach
            val minutes = java.time.Duration.between(now, time).toMinutes().toInt()
            if (minutes < 0) return@forEach
            val code = h.weatherCode
            val prob = h.precipitationProbability ?: 0
            val isRainy = code in 51..82 || code in 95..99
            if (isRainy && prob >= 30) {
                return NextRain(minutes, prob)
            }
        }
        return null
    }

    private fun parseHourly(time: String): LocalDateTime? = try {
        LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        null
    }

    private fun relativeMinutesLabel(minutes: Int): String = when {
        minutes < 30 -> "within the next half hour"
        minutes < 60 -> "within the next hour"
        minutes < 120 -> "in the next 1–2 hours"
        minutes < 360 -> "later this afternoon"
        minutes < 720 -> "this evening"
        else -> "tomorrow"
    }

    private fun describeTimeOfDay(hour: Int): String = when (hour) {
        in 5..11 -> "this morning"
        in 12..16 -> "this afternoon"
        in 17..20 -> "this evening"
        else -> "tonight"
    }

    private fun convertTemp(c: Double, isCelsius: Boolean): Int =
        if (isCelsius) c.roundToInt() else (c * 9.0 / 5.0 + 32).roundToInt()

    /** Convenience helper for "best outdoor time" suggestion used by morning briefing. */
    fun suggestOutdoorWindow(hourly: List<HourlyWeather>, isCelsius: Boolean): String? {
        val now = LocalDateTime.now()
        val good = hourly.sortedBy { it.time }
            .mapNotNull { h ->
                val time = parseHourly(h.time) ?: return@mapNotNull null
                if (time.isBefore(now)) return@mapNotNull null
                val prob = h.precipitationProbability ?: 0
                val tooWindy = (h.windSpeed ?: 0.0) > 40
                val tooHot = h.temperature >= (if (isCelsius) 35 else 95)
                if (prob <= 20 && !tooWindy && !tooHot) time to prob else null
            }
            .take(6)
        if (good.isEmpty()) return null
        val start = good.first().first
        val end = good.last().first
        val fmt = DateTimeFormatter.ofPattern("h a")
        return "${start.format(fmt).lowercase().replace("am", "AM").replace("pm", "PM")} – ${end.format(fmt).lowercase().replace("am", "AM").replace("pm", "PM")}"
    }
}
