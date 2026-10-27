package com.vayu.weather.domain.model

/**
 * Supplies the user-visible title and message text for an advisory.
 *
 * Keeping the text out of the evaluation logic means the advisories stay
 * localized (the texts come from the app's string resources) while the
 * evaluation itself remains unit-testable with a simple fake.
 */
interface WeatherAdvisoryTextProvider {
    fun titleFor(type: WeatherAdvisoryType): String
    fun messageFor(type: WeatherAdvisoryType, value: Int): String
}
