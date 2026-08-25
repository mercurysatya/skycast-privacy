package com.vayu.weather.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vayu.weather.domain.model.WeatherHistoryDay
import com.vayu.weather.domain.model.WeatherHistorySnapshot
import com.vayu.weather.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Time range filter for history view. */
enum class HistoryRange(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    THREE_MONTHS("3 Months", 90),
    ALL("All", 3650)
}

data class WeatherHistoryState(
    val snapshots: List<WeatherHistorySnapshot> = emptyList(),
    val dailyData: List<WeatherHistoryDay> = emptyList(),
    val selectedRange: HistoryRange = HistoryRange.WEEK,
    val selectedCity: String? = null,
    val isLoading: Boolean = true,
    val stats: HistoryStats? = null
)

data class HistoryStats(
    val avgTemp: Double,
    val maxTemp: Double,
    val minTemp: Double,
    val avgHumidity: Double?,
    val avgWind: Double?,
    val totalSnapshots: Int,
    val daysTracked: Int
)

@HiltViewModel
class WeatherHistoryViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherHistoryState())
    val state: StateFlow<WeatherHistoryState> = _state.asStateFlow()

    private var historyJob: kotlinx.coroutines.Job? = null

    init {
        loadHistory()
    }

    fun setRange(range: HistoryRange) {
        _state.value = _state.value.copy(selectedRange = range)
        loadHistory()
    }

    fun setSelectedCity(city: String?) {
        _state.value = _state.value.copy(selectedCity = city)
        loadHistory()
    }

    private fun loadHistory() {
        // Cancel the previous collector so range/city switches don't accumulate live collectors
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val range = _state.value.selectedRange
            val since = System.currentTimeMillis() - (range.days.toLong() * 24 * 60 * 60 * 1000)

            try {
                repository.getWeatherHistorySince(since).collect { snapshots ->
                    val filtered = if (_state.value.selectedCity != null) {
                        snapshots.filter { it.cityName == _state.value.selectedCity }
                    } else {
                        snapshots
                    }

                    val dailyData = aggregateByDay(filtered)
                    val stats = computeStats(filtered, dailyData)

                    _state.value = _state.value.copy(
                        snapshots = filtered,
                        dailyData = dailyData,
                        isLoading = false,
                        stats = stats
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WeatherHistoryViewModel", "Failed to load history", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearWeatherHistory()
            _state.value = _state.value.copy(
                snapshots = emptyList(),
                dailyData = emptyList(),
                stats = null,
                isLoading = false
            )
        }
    }

    private fun aggregateByDay(snapshots: List<WeatherHistorySnapshot>): List<WeatherHistoryDay> {
        if (snapshots.isEmpty()) return emptyList()

        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return snapshots
            .groupBy { snapshot ->
                Instant.ofEpochMilli(snapshot.timestamp)
                    .atZone(zoneId)
                    .toLocalDate()
                    .format(formatter)
            }
            .map { (date, daySnapshots) ->
                val temps = daySnapshots.map { it.temperature }
                val humidities = daySnapshots.mapNotNull { it.humidity }
                val winds = daySnapshots.mapNotNull { it.windSpeed }

                WeatherHistoryDay(
                    date = date,
                    minTemp = temps.min(),
                    maxTemp = temps.max(),
                    avgTemp = temps.average(),
                    weatherCode = daySnapshots.maxByOrNull { it.timestamp }?.weatherCode ?: 0,
                    humidity = humidities.ifEmpty { null }?.average(),
                    windSpeed = winds.ifEmpty { null }?.average(),
                    snapshotCount = daySnapshots.size
                )
            }
            .sortedBy { it.date }
    }

    private fun computeStats(
        snapshots: List<WeatherHistorySnapshot>,
        dailyData: List<WeatherHistoryDay>
    ): HistoryStats? {
        if (snapshots.isEmpty()) return null

        val temps = snapshots.map { it.temperature }
        val humidities = snapshots.mapNotNull { it.humidity }
        val winds = snapshots.mapNotNull { it.windSpeed }

        return HistoryStats(
            avgTemp = temps.average(),
            maxTemp = temps.max(),
            minTemp = temps.min(),
            avgHumidity = humidities.ifEmpty { null }?.average(),
            avgWind = winds.ifEmpty { null }?.average(),
            totalSnapshots = snapshots.size,
            daysTracked = dailyData.size
        )
    }
}
