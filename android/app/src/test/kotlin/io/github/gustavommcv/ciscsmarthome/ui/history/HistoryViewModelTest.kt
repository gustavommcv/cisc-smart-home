package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
import io.github.gustavommcv.ciscsmarthome.testing.FakeNameBook
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The clock of the tests of time: it is the start plus what the test scheduler has let pass. */
private class VirtualClock(private val scheduler: TestCoroutineScheduler, private val start: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
  override fun getZone(): ZoneId = zone

  override fun withZone(zone: ZoneId): Clock = this

  override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
  private val repository = FakeHomeRepository()
  private val nameBook = FakeNameBook()
  private val start = Instant.parse("2026-10-27T12:00:00Z")
  private val gasLevel = EntityId("gas-level")
  private val temperature = EntityId("temperature")
  private val humidity = EntityId("humidity")
  private val gasMonitor = DeviceId("gas-monitor")
  private val closetMonitor = DeviceId("closet-monitor")

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun sensor(id: EntityId, unit: String) = Entity.Sensor(id, "Name of ${id.value}", SensorValue.Number(1.0), unit)

  private fun device(id: DeviceId, vararg sensors: Entity) = Device(id, "Device ${id.value}", sensors.toList())

  private val maquette =
    Home(
      BootId("aaaa1111"),
      1_000,
      listOf(
        Area(AreaId("kitchen"), "Kitchen", listOf(device(gasMonitor, sensor(gasLevel, "adc")))),
        Area(AreaId("bedroom"), "Bedroom", listOf(device(closetMonitor, sensor(temperature, "°C"), sensor(humidity, "%")))),
      ),
    )

  private fun reading(minutesBeforeStart: Long, value: Double?) = Reading(start.minus(Duration.ofMinutes(minutesBeforeStart)), value)

  /** A view model that someone is watching, on the time of the test. */
  private fun TestScope.watched(zone: ZoneId = ZoneOffset.UTC): Pair<HistoryViewModel, StateFlow<HistoryState>> {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = HistoryViewModel(repository, nameBook, VirtualClock(testScheduler, start, zone))
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    return viewModel to viewModel.state
  }

  private fun panels(state: HistoryState) = (state.charts as ChartsState.Ready).panels

  // ---- the list ---------------------------------------------------------------------------

  @Test
  fun `it opens on the list of devices, no device open, and on six hours`() = runTest {
    repository.home.value = maquette
    val (_, state) = watched()
    assertNull(state.value.open)
    assertEquals(ChartPeriod.SixHours, state.value.period)
    assertEquals(listOf("gas-monitor", "closet-monitor"), state.value.devices.map { it.id.value })
  }

  @Test
  fun `before anyone watches, the list is already there and no chart is asked for`() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    repository.home.value = maquette
    val state = HistoryViewModel(repository, nameBook, VirtualClock(testScheduler, start)).state.value
    assertEquals(listOf("gas-monitor", "closet-monitor"), state.devices.map { it.id.value })
    assertNull(state.open)
    assertEquals(ChartsState.Loading, state.charts)
    assertTrue(repository.seriesRequests.isEmpty())
  }

  @Test
  fun `before anyone watches, the names and the icons are already there, so a screen does not flash the default ones`() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    nameBook.renameDevice(closetMonitor, "Armário do quarto")
    nameBook.chooseAreaIcon(AreaId("bedroom"), RoomIcon.Garage)
    val state = HistoryViewModel(repository, nameBook, VirtualClock(testScheduler, start)).state.value
    assertEquals("Armário do quarto", state.names.devices.getValue(closetMonitor))
    assertEquals(RoomIcon.Garage, state.icons.of(AreaId("bedroom")))
  }

  @Test
  fun `without a house, it lists the devices of the maquette, so the history can be read away from the board`() = runTest {
    val (_, state) = watched()
    assertEquals(listOf("gas-monitor", "closet-monitor"), state.value.devices.map { it.id.value })
  }

  @Test
  fun `a list asks the history for nothing`() = runTest {
    repository.home.value = maquette
    watched()
    advanceTimeBy(HistoryViewModel.REFRESH.toMillis() * 3)
    assertTrue(repository.seriesRequests.isEmpty())
  }

  @Test
  fun `the names and the icons the user gave come with the state`() = runTest {
    nameBook.renameDevice(closetMonitor, "Armário do quarto")
    nameBook.chooseAreaIcon(AreaId("bedroom"), RoomIcon.Garage)
    repository.home.value = maquette
    val (_, state) = watched()
    assertEquals("Armário do quarto", state.value.names.devices.getValue(closetMonitor))
    assertEquals(RoomIcon.Garage, state.value.icons.of(AreaId("bedroom")))
    nameBook.renameDevice(closetMonitor, null)
    assertEquals(Names(), state.value.names)
  }

  @Test
  fun `the icons are the ones of the name book, and a room with none has the default one`() = runTest {
    repository.home.value = maquette
    val (_, state) = watched()
    assertEquals(AreaIcons(), state.value.icons)
  }

  // ---- a device ---------------------------------------------------------------------------

  @Test
  fun `opening a device shows a chart for each of its charts`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    assertEquals(closetMonitor, state.value.open?.id)
    assertEquals(listOf("compare:temperature+humidity", "single:temperature", "single:humidity"), panels(state.value).map { it.spec.id })
  }

  @Test
  fun `a device is read once for each sensor it has, whatever the number of charts that draw it`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    viewModel.openDevice(closetMonitor)
    assertEquals(setOf(temperature, humidity), repository.seriesRequests.map { it.first }.toSet())
    assertEquals(2, repository.seriesRequests.size)
  }

  @Test
  fun `the charts are made of what the history has for the sensors`() = runTest {
    repository.home.value = maquette
    repository.setReadings(temperature, listOf(reading(30, 20.0), reading(10, 22.0)))
    repository.setReadings(humidity, listOf(reading(30, 60.0), reading(10, 70.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    val compare = panels(state.value).first()
    assertEquals(listOf(20.0 to 22.0, 60.0 to 70.0), compare.lanes.map { it.chart!!.min to it.chart!!.max })
    val humidityOnly = panels(state.value).last()
    assertEquals(60.0, humidityOnly.lanes.single().chart!!.min, 0.0)
  }

  @Test
  fun `a reading that arrives moves the charts`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(30, 40.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    repository.setReadings(gasLevel, listOf(reading(30, 40.0), reading(1, 90.0)))
    assertEquals(90.0, panels(state.value).single().lanes.single().chart!!.max, 0.0)
  }

  @Test
  fun `a chart with nothing in the window is empty`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(60 * 10, 40.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    assertTrue(panels(state.value).single().isEmpty)
  }

  @Test
  fun `a device that has the history of only one of its sensors draws that one`() = runTest {
    repository.home.value = maquette
    repository.setReadings(temperature, listOf(reading(10, 21.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    val (compare, onlyTemperature, onlyHumidity) = panels(state.value)
    assertFalse(compare.isEmpty)
    assertNull(compare.lanes[1].chart)
    assertFalse(onlyTemperature.isEmpty)
    assertTrue(onlyHumidity.isEmpty)
  }

  @Test
  fun `opening a device the house does not have opens nothing`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(DeviceId("elsewhere"))
    assertNull(state.value.open)
  }

  @Test
  fun `a device the house does not have is not opened later, when the house gets it`() = runTest {
    repository.home.value = maquette.copy(areas = maquette.areas.take(1))
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    repository.home.value = maquette
    assertNull(state.value.open)
  }

  @Test
  fun `back closes the device and says it did`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    assertTrue(viewModel.back())
    assertNull(state.value.open)
    assertEquals(ChartsState.Loading, state.value.charts)
  }

  @Test
  fun `back from the list is not for the history to answer`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    assertFalse(viewModel.back())
  }

  @Test
  fun `a device that left the house is no longer the one that is open`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    repository.home.value = maquette.copy(areas = maquette.areas.take(1))
    assertNull(state.value.open)
  }

  @Test
  fun `a device stays open when the house changes and still has it`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(closetMonitor)
    repository.home.value = maquette.copy(uptimeMs = 2_000)
    assertEquals(closetMonitor, state.value.open?.id)
  }

  @Test
  fun `a house heard again with the same devices does not ask the history again`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    viewModel.openDevice(closetMonitor)
    val asked = repository.seriesRequests.size
    repository.home.value = maquette.copy(uptimeMs = 2_000)
    repository.home.value = maquette.copy(uptimeMs = 3_000)
    assertEquals(asked, repository.seriesRequests.size)
  }

  // ---- the period ---------------------------------------------------------------------------

  @Test
  fun `choosing a period asks the history for that window`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.TwentyFourHours)
    assertEquals(ChartPeriod.TwentyFourHours, state.value.period)
    assertEquals(gasLevel to start.minus(Duration.ofHours(24)), repository.seriesRequests.last())
  }

  @Test
  fun `a longer period brings readings a shorter one left out`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(60 * 10, 10.0), reading(10, 50.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    assertEquals(50.0, panels(state.value).single().lanes.single().chart!!.min, 0.0)
    viewModel.choosePeriod(ChartPeriod.TwentyFourHours)
    assertEquals(10.0, panels(state.value).single().lanes.single().chart!!.min, 0.0)
  }

  @Test
  fun `the period stays when another device is opened`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.OneHour)
    viewModel.back()
    viewModel.openDevice(closetMonitor)
    assertEquals(ChartPeriod.OneHour, state.value.period)
  }

  @Test
  fun `the window moves on with time, so the charts do not stay where they were drawn`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    viewModel.openDevice(gasMonitor)
    val first = repository.seriesRequests.last().second
    advanceTimeBy(HistoryViewModel.REFRESH.toMillis() + 1)
    assertTrue("the window did not move: $first", repository.seriesRequests.last().second.isAfter(first))
  }

  // ---- the month and the year ------------------------------------------------------------------

  private val october = Instant.parse("2026-10-01T00:00:00Z")
  private val november = Instant.parse("2026-11-01T00:00:00Z")
  private val september = Instant.parse("2026-09-01T00:00:00Z")

  private fun hour(day: Int, hourOfDay: Int, lowest: Double, highest: Double) =
    HourlyReading(Instant.parse("2026-10-%02dT%02d:00:00Z".format(day, hourOfDay)), lowest, highest)

  @Test
  fun `a month asks the history for the hours of that calendar month, and not for the readings`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    viewModel.openDevice(gasMonitor)
    val readingsAsked = repository.seriesRequests.size
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(Triple(gasLevel, october, november), repository.hourlyRequests.last())
    assertEquals(readingsAsked, repository.seriesRequests.size)
  }

  @Test
  fun `a month is read once for each sensor`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched()
    viewModel.openDevice(closetMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(setOf(temperature, humidity), repository.hourlyRequests.map { it.first }.toSet())
    assertEquals(2, repository.hourlyRequests.size)
  }

  @Test
  fun `the chart of a month is made of the least and the greatest of its hours`() = runTest {
    repository.home.value = maquette
    repository.setHours(gasLevel, listOf(hour(5, 10, 40.0, 90.0), hour(6, 10, 44.0, 60.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    val chart = panels(state.value).single().lanes.single().chart!!
    assertEquals(40.0, chart.min, 0.0)
    assertEquals(90.0, chart.max, 0.0)
    assertEquals(ChartPeriod.Month, state.value.window.period)
  }

  @Test
  fun `hours that follow each other are one line, and a missing hour breaks it`() = runTest {
    repository.home.value = maquette
    repository.setHours(gasLevel, listOf(hour(5, 10, 40.0, 50.0), hour(5, 11, 41.0, 51.0), hour(5, 13, 42.0, 52.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(listOf(4, 2), panels(state.value).single().lanes.single().chart!!.lines.map { it.size })
  }

  @Test
  fun `the hours of another month are not in the chart`() = runTest {
    repository.home.value = maquette
    repository.setHours(gasLevel, listOf(HourlyReading(Instant.parse("2026-09-30T23:00:00Z"), 1.0, 2.0), HourlyReading(Instant.parse("2026-11-01T00:00:00Z"), 8.0, 9.0), hour(5, 10, 40.0, 90.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(40.0 to 90.0, panels(state.value).single().lanes.single().chart!!.let { it.min to it.max })
  }

  @Test
  fun `a year is the whole calendar year`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Year)
    assertEquals(Triple(gasLevel, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z")), repository.hourlyRequests.last())
    assertEquals(ChartPeriod.Year, state.value.period)
  }

  @Test
  fun `a month begins at midnight in the zone of the phone`() = runTest {
    repository.home.value = maquette
    val (viewModel, _) = watched(zone = ZoneId.of("America/Sao_Paulo"))
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(Triple(gasLevel, Instant.parse("2026-10-01T03:00:00Z"), Instant.parse("2026-11-01T03:00:00Z")), repository.hourlyRequests.last())
  }

  // ---- the pages --------------------------------------------------------------------------------

  @Test
  fun `going back shows the month before, and says it can go forward`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2026-07-10T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertFalse(state.value.canGoLater)
    viewModel.earlier()
    assertEquals(1, state.value.window.offset)
    assertEquals(Triple(gasLevel, september, october), repository.hourlyRequests.last())
    assertTrue(state.value.canGoLater)
  }

  @Test
  fun `going forward again comes back to the month before it`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2026-07-10T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    viewModel.earlier()
    viewModel.earlier()
    viewModel.later()
    assertEquals(1, state.value.window.offset)
    viewModel.later()
    assertEquals(0, state.value.window.offset)
    assertEquals(Triple(gasLevel, october, november), repository.hourlyRequests.last())
  }

  @Test
  fun `it cannot go forward from the present`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    viewModel.later()
    assertEquals(0, state.value.window.offset)
    assertFalse(state.value.canGoLater)
  }

  @Test
  fun `it goes back only while the history has hours before the window`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2026-09-20T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertTrue(state.value.canGoEarlier)
    viewModel.earlier()
    assertFalse("nothing is before September 20, which is in September", state.value.canGoEarlier)
    viewModel.earlier()
    assertEquals(1, state.value.window.offset)
  }

  @Test
  fun `the first hour of the history being the first moment of the window is nothing before it`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(october)
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    assertFalse(state.value.canGoEarlier)
  }

  @Test
  fun `with no history there is nowhere to go back to`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Year)
    assertFalse(state.value.canGoEarlier)
    viewModel.earlier()
    assertEquals(0, state.value.window.offset)
  }

  @Test
  fun `the last hours have no pages`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2020-01-01T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    assertFalse(state.value.canGoEarlier)
    viewModel.earlier()
    assertEquals(0, state.value.window.offset)
    assertEquals(gasLevel to start.minus(Duration.ofHours(6)), repository.seriesRequests.last())
  }

  @Test
  fun `choosing a period goes back to the present`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2020-01-01T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    viewModel.earlier()
    viewModel.earlier()
    viewModel.choosePeriod(ChartPeriod.Year)
    assertEquals(0, state.value.window.offset)
    viewModel.earlier()
    viewModel.choosePeriod(ChartPeriod.Month)
    assertEquals(0, state.value.window.offset)
  }

  @Test
  fun `the page stays when another device is opened`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2020-01-01T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    viewModel.earlier()
    viewModel.back()
    viewModel.openDevice(closetMonitor)
    assertEquals(ChartPeriod.Month, state.value.window.period)
    assertEquals(1, state.value.window.offset)
  }

  @Test
  fun `before anyone watches, the window is the one of the period that opens and there are no pages`() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val state = HistoryViewModel(repository, nameBook, VirtualClock(testScheduler, start)).state.value
    assertEquals(ChartPeriod.SixHours, state.window.period)
    assertEquals(start.minus(Duration.ofHours(6)), state.window.since)
    assertEquals(start, state.window.until)
    assertFalse(state.canGoEarlier)
    assertFalse(state.canGoLater)
  }

  @Test
  fun `going forward from the present does not make the next step back lose a page`() = runTest {
    repository.home.value = maquette
    repository.setFirstHour(Instant.parse("2020-01-01T00:00:00Z"))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    viewModel.later()
    viewModel.later()
    viewModel.earlier()
    assertEquals(1, state.value.window.offset)
  }

  @Test
  fun `the readings of the last hours break their line after two minutes of silence`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(60, 40.0), reading(30, 50.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    assertEquals(2, panels(state.value).single().lanes.single().chart!!.lines.size)
  }

  @Test
  fun `the state carries the zone the windows are made in, for the screen to write the dates in`() = runTest {
    repository.home.value = maquette
    val (_, state) = watched(zone = ZoneId.of("America/Sao_Paulo"))
    assertEquals(ZoneId.of("America/Sao_Paulo"), state.value.zone)
  }

  @Test
  fun `a month that is being read is read again when an hour changes`() = runTest {
    repository.home.value = maquette
    repository.setHours(gasLevel, listOf(hour(5, 10, 40.0, 50.0)))
    val (viewModel, state) = watched()
    viewModel.openDevice(gasMonitor)
    viewModel.choosePeriod(ChartPeriod.Month)
    repository.setHours(gasLevel, listOf(hour(5, 10, 40.0, 99.0)))
    assertEquals(99.0, panels(state.value).single().lanes.single().chart!!.max, 0.0)
  }
}
