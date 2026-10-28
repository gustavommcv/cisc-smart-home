package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
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
import org.junit.Assert.assertTrue
import org.junit.Test

/** The clock of the tests of time: it is the start plus what the test scheduler has let pass. */
private class VirtualClock(private val scheduler: TestCoroutineScheduler, private val start: Instant) : Clock() {
  override fun getZone(): ZoneId = ZoneOffset.UTC

  override fun withZone(zone: ZoneId): Clock = this

  override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
  private val repository = FakeHomeRepository()
  private val start = Instant.parse("2026-10-27T12:00:00Z")
  private val gasLevel = EntityId("gas-level")
  private val temperature = EntityId("temperature")
  private val humidity = EntityId("humidity")

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun sensor(id: EntityId, unit: String) = Entity.Sensor(id, "Name of ${id.value}", SensorValue.Number(1.0), unit)

  private fun house(vararg sensors: Entity) = Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("room"), "Room", listOf(Device(DeviceId("device"), "Device", sensors.toList())))))

  private val maquette = house(sensor(gasLevel, "adc"), sensor(temperature, "°C"), sensor(humidity, "%"))

  private fun reading(minutesBeforeStart: Long, value: Double?) = Reading(start.minus(Duration.ofMinutes(minutesBeforeStart)), value)

  /** A view model that someone is watching, on the time of the test. */
  private fun TestScope.watched(): Pair<HistoryViewModel, StateFlow<HistoryState>> {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = HistoryViewModel(repository, VirtualClock(testScheduler, start))
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    return viewModel to viewModel.state
  }

  private fun ready(state: HistoryState) = state.chart as ChartState.Ready

  @Test
  fun `before anyone watches, the chart is loading`() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    assertEquals(ChartState.Loading, HistoryViewModel(repository, VirtualClock(testScheduler, start)).state.value.chart)
  }

  @Test
  fun `it opens on the first sensor and on six hours`() = runTest {
    repository.home.value = maquette
    val (_, state) = watched()
    assertEquals(gasLevel, state.value.selected)
    assertEquals(ChartPeriod.SixHours, state.value.period)
  }

  @Test
  fun `it offers the sensors of the house that have a unit`() = runTest {
    repository.home.value = house(sensor(gasLevel, "adc"), sensor(EntityId("wind"), "km/h"))
    val (_, state) = watched()
    assertEquals(listOf("gas-level", "wind"), state.value.sensors.map { it.id.value })
  }

  @Test
  fun `without a house, it offers the sensors of the maquette, so the history can be read away from the board`() = runTest {
    val (_, state) = watched()
    assertEquals(listOf("gas-level", "temperature", "humidity"), state.value.sensors.map { it.id.value })
    assertEquals(gasLevel, state.value.selected)
  }

  @Test
  fun `the chart is made of what the history has for the sensor`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(30, 40.0), reading(10, 60.0)))
    val (_, state) = watched()
    val chart = ready(state.value)
    assertEquals(40.0, chart.chart.min, 0.0)
    assertEquals(60.0, chart.chart.max, 0.0)
    assertEquals("adc", chart.unit)
  }

  @Test
  fun `a reading that arrives moves the chart`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(30, 40.0)))
    val (_, state) = watched()
    repository.setReadings(gasLevel, listOf(reading(30, 40.0), reading(1, 90.0)))
    assertEquals(90.0, ready(state.value).chart.max, 0.0)
  }

  @Test
  fun `choosing a sensor shows what the history has for it`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(10, 45.0)))
    repository.setReadings(temperature, listOf(reading(10, 21.0)))
    val (viewModel, state) = watched()
    viewModel.selectSensor(temperature)
    assertEquals(temperature, state.value.selected)
    assertEquals(21.0, ready(state.value).chart.max, 0.0)
    assertEquals("°C", ready(state.value).unit)
  }

  @Test
  fun `choosing a period asks the history for that window`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.choosePeriod(ChartPeriod.TwentyFourHours)
    assertEquals(ChartPeriod.TwentyFourHours, state.value.period)
    assertEquals(gasLevel to start.minus(Duration.ofHours(24)), repository.seriesRequests.last())
  }

  @Test
  fun `a longer period brings readings a shorter one left out`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(60 * 10, 10.0), reading(10, 50.0)))
    val (viewModel, state) = watched()
    assertEquals(50.0, ready(state.value).chart.min, 0.0)
    viewModel.choosePeriod(ChartPeriod.TwentyFourHours)
    assertEquals(10.0, ready(state.value).chart.min, 0.0)
  }

  @Test
  fun `the window moves on with time, so the chart does not stay where it was drawn`() = runTest {
    repository.home.value = maquette
    watched()
    val first = repository.seriesRequests.last().second
    advanceTimeBy(HistoryViewModel.REFRESH.toMillis() + 1)
    assertTrue("the window did not move: $first", repository.seriesRequests.last().second.isAfter(first))
  }

  @Test
  fun `a history with nothing in the window is an empty chart`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(60 * 10, 40.0)))
    val (_, state) = watched()
    assertEquals(ChartState.Empty, state.value.chart)
  }

  @Test
  fun `readings with no value are an empty chart`() = runTest {
    repository.home.value = maquette
    repository.setReadings(gasLevel, listOf(reading(10, null), reading(5, null)))
    val (_, state) = watched()
    assertEquals(ChartState.Empty, state.value.chart)
  }

  @Test
  fun `a sensor that left the house is no longer the one that is chosen`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.selectSensor(temperature)
    repository.home.value = house(sensor(gasLevel, "adc"), sensor(humidity, "%"))
    assertEquals(gasLevel, state.value.selected)
  }

  @Test
  fun `a sensor stays chosen when the house changes and still has it`() = runTest {
    repository.home.value = maquette
    val (viewModel, state) = watched()
    viewModel.selectSensor(humidity)
    repository.home.value = house(sensor(gasLevel, "adc"), sensor(temperature, "°C"), sensor(humidity, "%"), sensor(EntityId("wind"), "km/h"))
    assertEquals(humidity, state.value.selected)
  }

  @Test
  fun `a house with no sensor to chart has no chart to show`() = runTest {
    repository.home.value = house(Entity.BinarySensor(EntityId("valve-open"), "Valve", true))
    val (_, state) = watched()
    assertEquals(null, state.value.selected)
    assertEquals(ChartState.Empty, state.value.chart)
  }
}
