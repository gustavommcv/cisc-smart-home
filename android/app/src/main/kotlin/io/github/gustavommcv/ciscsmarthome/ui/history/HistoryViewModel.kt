package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
 * What the history tab shows: the devices it lists, the one that is [open] (none while it shows the list), how far back the charts go, the stretch
 * of time of the [window] (and whether the page before or the page after it exists), the charts of the open device, and the names and icons the user
 * gave, for the list and for the title.
 */
internal data class HistoryState(
  val devices: List<HistoryDevice>,
  val open: HistoryDevice?,
  val period: ChartPeriod,
  val window: ChartWindow,
  val zone: ZoneId,
  val canGoEarlier: Boolean,
  val canGoLater: Boolean,
  val charts: ChartsState,
  val names: Names,
  val icons: AreaIcons,
)

internal class HistoryViewModel(private val repository: HomeRepository, private val nameBook: NameBook, private val clock: Clock) : ViewModel() {
  private val openId = MutableStateFlow<DeviceId?>(null)
  private val chosenPeriod = MutableStateFlow(ChartPeriod.Default)

  // How many months or years back the chart is. It is the same for every device, and goes back to the present when another period is chosen.
  private val page = MutableStateFlow(0)

  private val zone: ZoneId = clock.zone

  private data class Choice(
    val devices: List<HistoryDevice>,
    val open: DeviceId?,
    val period: ChartPeriod,
    val page: Int,
    val firstHour: Instant?,
    val limitValues: Map<EntityId, Double>,
    val names: Names,
    val icons: AreaIcons,
  )

  private data class Customs(val devices: List<HistoryDevice>, val names: Names, val icons: AreaIcons, val limitValues: Map<EntityId, Double>)

  @OptIn(ExperimentalCoroutinesApi::class)
  val state: StateFlow<HistoryState> =
    combine(
        combine(repository.home, nameBook.names, nameBook.icons) { home, names, icons -> Customs(historyDevices(home), names, icons, limitValuesOf(home)) },
        openId,
        chosenPeriod,
        page,
        repository.firstHour(),
      ) { customs, open, period, page, firstHour ->
        Choice(customs.devices, open, period, page, firstHour, customs.limitValues, customs.names, customs.icons)
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
    page.value = 0
  }

  /** Goes to the month or the year before, when the history has hours before the ones of this window. */
  fun earlier() {
    if (state.value.canGoEarlier) page.value += 1
  }

  /** Goes to the month or the year after, up to the present. */
  fun later() {
    if (page.value > 0) page.value -= 1
  }

  private fun initial() =
    HistoryState(
      devices = historyDevices(repository.home.value),
      open = null,
      period = ChartPeriod.Default,
      window = windowOf(ChartPeriod.Default, 0, clock.instant(), zone),
      zone = zone,
      canGoEarlier = false,
      canGoLater = false,
      charts = ChartsState.Loading,
      names = nameBook.names.value,
      icons = nameBook.icons.value,
    )

  // A device that is not in the house any more cannot stay open: the list takes its place.
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun stateOf(choice: Choice): Flow<HistoryState> {
    val open = choice.devices.firstOrNull { it.id == choice.open }
    fun stateWith(window: ChartWindow, charts: ChartsState) =
      HistoryState(
        devices = choice.devices,
        open = open,
        period = choice.period,
        window = window,
        zone = zone,
        // There is a page before when the history has hours before the start of this one: a month with nothing before it is the first.
        canGoEarlier = window.period.isCalendar && choice.firstHour != null && choice.firstHour < window.since,
        canGoLater = window.offset > 0,
        charts = charts,
        names = choice.names,
        icons = choice.icons,
      )
    if (open == null) return flowOf(stateWith(windowOf(choice.period, choice.page, clock.instant(), zone), ChartsState.Loading))
    // A sensor drawn by more than one chart (the temperature is in its own and in the comparison) is read once.
    val sensors = open.charts.flatMap { it.sensors }.distinctBy { it.id }
    // The window starts where it did a moment ago and is asked for again, so it moves on with time even when nothing new is read.
    return ticks()
      .flatMapLatest { now ->
        val asked = windowOf(choice.period, choice.page, now, zone)
        combine(sensors.map { sensor -> sourceOf(sensor.id, asked).map { sensor.id to it } }) { it.toMap() }
      }
      .map { sources ->
        // The window ends when the charts are made, so a reading that came after the last tick is in them.
        val window = windowOf(choice.period, choice.page, clock.instant(), zone)
        val gap = if (window.period.isCalendar) ChartRules.HourlyMaxGap else ChartRules.MaxGap
        val readings = sources.mapValues { it.value.readings }
        // The hours are given only for a month or a year, which is when a sensor is drawn by the day.
        val hours = if (window.period.isCalendar) sources.mapValues { it.value.hours.orEmpty() } else emptyMap()
        stateWith(window, ChartsState.Ready(open.charts.map { panelOf(it, readings, window.since, window.until, gap, choice.limitValues, hours, zone) }))
      }
  }

  // What a sensor has for the window: the readings of the last hours, or the hours of a month or a year (also as the points of a line).
  private class Source(val readings: List<Reading>, val hours: List<HourlyReading>?)

  private fun sourceOf(sensor: EntityId, window: ChartWindow): Flow<Source> =
    if (window.period.isCalendar) repository.hourly(sensor, window.since, window.until).map { Source(it.points(), it) }
    else repository.series(sensor, window.since).map { Source(it, null) }

  // The values the house has now of the entities the limits over the charts are read from (the ones the board points to, and the ones the catalog knows):
  // a limit the user changes is drawn at its new value.
  private fun limitValuesOf(home: Home?): Map<EntityId, Double> {
    val entities = home?.areas?.flatMap { it.devices }?.flatMap { it.entities }.orEmpty()
    val pointedTo = entities.filterIsInstance<Entity.Sensor>().flatMap { it.limits }.filterIsInstance<LimitSpec.OfEntity>().map { it.entity }.toSet()
    val followed = pointedTo + DeviceCatalog.limitEntities
    return entities.filterIsInstance<Entity.Number>().filter { it.id in followed }.mapNotNull { number -> number.value?.let { number.id to it } }.toMap()
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
