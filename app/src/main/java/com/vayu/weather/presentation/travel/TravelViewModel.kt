package com.vayu.weather.presentation.travel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.City
import com.vayu.weather.domain.model.DailyWeather
import com.vayu.weather.domain.model.WeatherInfo
import com.vayu.weather.domain.use_case.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * SkyCast Travel ViewModel.
 *
 * The user picks a destination city and a travel date. The VM caches the
 * fetched weather and exposes the relevant [DailyWeather] so the screen can
 * build a travel-readiness summary.
 */
@HiltViewModel
class TravelViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase
) : ViewModel() {

    data class TravelState(
        val destination: City? = null,
        val travelDate: LocalDate? = null,
        val dailyForDate: DailyWeather? = null,
        val aqi: Int? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    var state by mutableStateOf(TravelState())
        private set

    fun setDestination(city: City) {
        state = state.copy(destination = city, error = null)
        refresh()
    }

    fun setDate(date: LocalDate) {
        state = state.copy(travelDate = date, dailyForDate = null, error = null)
        if (state.destination != null) refresh()
    }

    fun clear() {
        state = TravelState()
    }

    fun refresh() {
        val city = state.destination ?: return
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getWeatherUseCase(city.latitude, city.longitude)
                .onSuccess { info ->
                    val day = state.travelDate?.let { pickDay(info, it) }
                    state = state.copy(
                        dailyForDate = day,
                        aqi = null, // AQI loaded separately elsewhere
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    state = state.copy(isLoading = false, error = e.message ?: "Couldn't load forecast.")
                }
        }
    }

    private fun pickDay(info: WeatherInfo, target: LocalDate): DailyWeather? {
        return info.daily.firstOrNull { it.date == target.toString() }
    }
}
