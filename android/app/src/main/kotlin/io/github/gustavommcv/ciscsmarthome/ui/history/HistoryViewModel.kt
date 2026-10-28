package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.Names
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the charts of the open device show: still waiting for the history (or no device is open), or the charts. */
internal sealed interface ChartsState {
  data object Loading : ChartsState

  data class Ready(val panels: List<ChartPanel>) : ChartsState
}

/**
 * What the history tab shows: the devices it lists, the one that is [open] (none while it shows the list), how far back the charts go, the charts
 * of the open device, and the names and icons the user gave, for the list and for the title.
 */
internal data class HistoryState(
  val devices: List<HistoryDevice>,
  val open: HistoryDevice?,
  val period: ChartPeriod,
  val charts: ChartsState,
  val names: Names,
  val icons: AreaIcons,
)

internal class HistoryViewModel(private val repository: HomeRepository, private val nameBook: NameBook, private val clock: Clock) : ViewModel() {
  private val openId = MutableStateFlow<DeviceId?>(null)
  private val chosenPeriod = MutableStateFlow(ChartPeriod.Default)

  private data class Choice(val devices: List<HistoryDevice>, val open: DeviceId?, val period: ChartPeriod, val names: Names, val icons: AreaIcons)

  @OptIn(ExperimentalCoroutinesApi::class)
  val state: StateFlow<HistoryState> =
    combine(repository.home, openId, chosenPeriod, nameBook.names, nameBook.icons) { home, open, period, names, icons ->
        Choice(historyDevices(home), open, period, names, icons)
      }
      // The house is heard again at each reading, and the charts are asked for again only when what they are made of changed.
      .distinctUntilChanged()
      .flatMapLatest { choice -> stateOf(choice) }
      // Stops following the history a few seconds after the screen is gone, so a rotation does not start it over.
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial())

  /** Opens the charts of a device of the list; one the house does not have opens nothing. */
  fun openDevice(id: DeviceId) {
    if (historyDevices(repository.home.value).any { it.id == id }) openId.value = id
  }

  /** Closes the device that is open and goes back to the list. False when none was open: back is not for the history to answer. */
  fun back(): Boolean {
    if (openId.value == null) return false
    openId.value = null
    return true
  }

  fun choosePeriod(period: ChartPeriod) {
    chosenPeriod.value = period
  }

  private fun initial() = HistoryState(historyDevices(repository.home.value), null, ChartPeriod.Default, ChartsState.Loading, nameBook.names.value, nameBook.icons.value)

  // A device that is not in the house any more cannot stay open: the list takes its place.
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun stateOf(choice: Choice): Flow<HistoryState> {
    val open = choice.devices.firstOrNull { it.id == choice.open }
    fun stateWith(charts: ChartsState) = HistoryState(choice.devices, open, choice.period, charts, choice.names, choice.icons)
    if (open == null) return flowOf(stateWith(ChartsState.Loading))
    // A sensor drawn by more than one chart (the temperature is in its own and in the comparison) is read once.
    val sensors = open.charts.flatMap { it.sensors }.distinctBy { it.id }
    // The window starts where it did a moment ago and is asked for again, so it moves on with time even when nothing new is read.
    return ticks()
      .flatMapLatest { now ->
        val since = now.minus(choice.period.duration)
        combine(sensors.map { sensor -> repository.series(sensor.id, since).map { sensor.id to it } }) { it.toMap() }
      }
      .map { readings -> stateWith(ChartsState.Ready(panelsOf(open, readings, choice.period))) }
  }

  // The window ends when the charts are made, so a reading that came after the last tick is in them.
  private fun panelsOf(device: HistoryDevice, readings: Map<EntityId, List<Reading>>, period: ChartPeriod): List<ChartPanel> {
    val until = clock.instant()
    return device.charts.map { panelOf(it, readings, until.minus(period.duration), until) }
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
