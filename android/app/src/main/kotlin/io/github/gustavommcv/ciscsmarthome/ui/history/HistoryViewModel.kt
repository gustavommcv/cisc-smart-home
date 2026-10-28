package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the chart shows: still waiting for the history, nothing to draw in the window, or the chart with the unit it reads in. */
internal sealed interface ChartState {
  data object Loading : ChartState

  data object Empty : ChartState

  data class Ready(val chart: Chart, val unit: String?) : ChartState
}

/** What the history tab shows: the sensors it offers, the one that is chosen, how far back it goes and the chart. */
internal data class HistoryState(val sensors: List<ChartSensor>, val selected: EntityId?, val period: ChartPeriod, val chart: ChartState)

internal class HistoryViewModel(private val repository: HomeRepository, private val clock: Clock) : ViewModel() {
  private val chosenSensor = MutableStateFlow<EntityId?>(null)
  private val chosenPeriod = MutableStateFlow(ChartPeriod.Default)

  private data class Choice(val sensors: List<ChartSensor>, val sensor: EntityId?, val period: ChartPeriod)

  @OptIn(ExperimentalCoroutinesApi::class)
  val state: StateFlow<HistoryState> =
    combine(repository.home, chosenSensor, chosenPeriod) { home, sensor, period -> Choice(chartSensors(home), sensor, period) }
      .flatMapLatest { choice -> stateOf(choice) }
      // Stops following the history a few seconds after the screen is gone, so a rotation does not start it over.
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial())

  fun selectSensor(id: EntityId) {
    chosenSensor.value = id
  }

  fun choosePeriod(period: ChartPeriod) {
    chosenPeriod.value = period
  }

  private fun initial(): HistoryState {
    val sensors = chartSensors(repository.home.value)
    return HistoryState(sensors, sensors.firstOrNull()?.id, ChartPeriod.Default, ChartState.Loading)
  }

  // A sensor that is not in the house any more cannot stay chosen: the first one takes its place.
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun stateOf(choice: Choice): Flow<HistoryState> {
    val selected = choice.sensors.firstOrNull { it.id == choice.sensor } ?: choice.sensors.firstOrNull() ?: return flowOf(HistoryState(choice.sensors, null, choice.period, ChartState.Empty))
    // The window starts where it did a moment ago and is asked for again, so it moves on with time even when nothing new is read.
    return ticks().flatMapLatest { now ->
      repository.series(selected.id, now.minus(choice.period.duration)).map { readings -> HistoryState(choice.sensors, selected.id, choice.period, chartStateOf(readings, choice.period, selected.unit)) }
    }
  }

  // The window ends when the chart is made, so a reading that came after the last tick is in it.
  private fun chartStateOf(readings: List<Reading>, period: ChartPeriod, unit: String): ChartState {
    val until = clock.instant()
    return chartOf(readings, until.minus(period.duration), until)?.let { ChartState.Ready(it, unit) } ?: ChartState.Empty
  }

  private fun ticks(): Flow<Instant> = flow {
    while (true) {
      emit(clock.instant())
      delay(REFRESH.toMillis())
    }
  }

  internal companion object {
    /** How often the window is moved on, whether or not something was read. */
    val REFRESH: Duration = Duration.ofSeconds(30)

    private const val STOP_TIMEOUT_MS = 5_000L
  }
}
