package com.vayu.weather.data.local

import android.content.Context
import com.vayu.weather.R
import com.vayu.weather.domain.model.WeatherAdvisoryTextProvider
import com.vayu.weather.domain.model.WeatherAdvisoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Advisory texts taken from the app's string resources, so the advisory cards
 * and notifications stay localized.
 */
class ResWeatherAdvisoryTextProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : WeatherAdvisoryTextProvider {

    override fun titleFor(type: WeatherAdvisoryType): String = context.getString(
        when (type) {
            WeatherAdvisoryType.RAIN -> R.string.rain_alert_title
            WeatherAdvisoryType.WIND -> R.string.wind_alert_title
            WeatherAdvisoryType.UV -> R.string.uv_alert_title
            WeatherAdvisoryType.HEAT -> R.string.heat_alert_title
            WeatherAdvisoryType.COLD -> R.string.cold_alert_title
        }
    )

    override fun messageFor(type: WeatherAdvisoryType, value: Int): String = context.getString(
        when (type) {
            WeatherAdvisoryType.RAIN -> R.string.rain_alert_message
            WeatherAdvisoryType.WIND -> R.string.wind_alert_message
            WeatherAdvisoryType.UV -> R.string.uv_alert_message
            WeatherAdvisoryType.HEAT -> R.string.heat_alert_message
            WeatherAdvisoryType.COLD -> R.string.cold_alert_message
        },
        value
    )
}
