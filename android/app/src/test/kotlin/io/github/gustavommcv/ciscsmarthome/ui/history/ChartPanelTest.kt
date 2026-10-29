package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.ChartRange
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.SensorKind
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A chart of the history made of what was read: one band for a chart of a sensor, two for the chart that compares, each with its own scale and the same window. */
class ChartPanelTest {
  private val until = Instant.parse("2026-10-27T12:00:00Z")
  private val since = until.minus(Duration.ofHours(6))
  private val temperature = ChartSensor(EntityId("temperature"), "Temperature", "°C")
  private val humidity = ChartSensor(EntityId("humidity"), "Humidity", "%")

  private fun reading(minutesBefore: Long, value: Double?) = Reading(until.minus(Duration.ofMinutes(minutesBefore)), value)

  @Test
  fun `a chart of one sensor has one band`() {
    val panel = panelOf(ChartSpec.Single(temperature), mapOf(temperature.id to listOf(reading(30, 20.0), reading(10, 22.0))), since, until)
    assertEquals(1, panel.lanes.size)
    assertEquals(temperature, panel.lanes.single().sensor)
    assertEquals(20.0, panel.lanes.single().chart!!.min, 0.0)
    assertEquals(22.0, panel.lanes.single().chart!!.max, 0.0)
  }

  @Test
  fun `the chart that compares has a band for each sensor, the first one above`() {
    val readings = mapOf(temperature.id to listOf(reading(30, 20.0), reading(10, 22.0)), humidity.id to listOf(reading(30, 60.0), reading(10, 70.0)))
    val panel = panelOf(ChartSpec.Compare(temperature, humidity), readings, since, until)
    assertEquals(listOf(temperature, humidity), panel.lanes.map { it.sensor })
  }

  @Test
  fun `each band has the scale of its own sensor, so a temperature and a humidity do not squash each other`() {
    val readings = mapOf(temperature.id to listOf(reading(30, 20.0), reading(10, 22.0)), humidity.id to listOf(reading(30, 60.0), reading(10, 70.0)))
    val lanes = panelOf(ChartSpec.Compare(temperature, humidity), readings, since, until).lanes
    assertEquals(20.0 to 22.0, lanes[0].chart!!.min to lanes[0].chart!!.max)
    assertEquals(60.0 to 70.0, lanes[1].chart!!.min to lanes[1].chart!!.max)
  }

  @Test
  fun `the bands share the window, so a reading of the same minute is at the same place in both`() {
    val readings = mapOf(temperature.id to listOf(reading(181, 20.0), reading(180, 22.0)), humidity.id to listOf(reading(181, 60.0), reading(180, 70.0)))
    val lanes = panelOf(ChartSpec.Compare(temperature, humidity), readings, since, until).lanes
    val temperatureX = lanes[0].chart!!.lines.single().map { it.x }
    assertEquals(listOf(0.4972f, 0.5f), temperatureX.map { Math.round(it * 10_000) / 10_000f })
    assertEquals(temperatureX, lanes[1].chart!!.lines.single().map { it.x })
  }

  @Test
  fun `a sensor with nothing read in the window is a band with no chart, and the other one is drawn`() {
    val readings = mapOf(temperature.id to listOf(reading(10, 21.0)), humidity.id to emptyList())
    val lanes = panelOf(ChartSpec.Compare(temperature, humidity), readings, since, until).lanes
    assertNotNull(lanes[0].chart)
    assertNull(lanes[1].chart)
  }

  @Test
  fun `readings with no value, or outside the window, are no reading`() {
    val readings = mapOf(temperature.id to listOf(reading(10, null), reading(60 * 10, 30.0)))
    assertNull(panelOf(ChartSpec.Single(temperature), readings, since, until).lanes.single().chart)
  }

  @Test
  fun `a chart is empty when none of its bands has anything to draw`() {
    assertTrue(panelOf(ChartSpec.Compare(temperature, humidity), emptyMap(), since, until).isEmpty)
    assertTrue(panelOf(ChartSpec.Single(temperature), mapOf(temperature.id to emptyList()), since, until).isEmpty)
  }

  @Test
  fun `a chart with one band that has something is not empty`() {
    val readings = mapOf(humidity.id to listOf(reading(10, 65.0)))
    assertFalse(panelOf(ChartSpec.Compare(temperature, humidity), readings, since, until).isEmpty)
  }

  @Test
  fun `the bands break their lines where the silence given is longer`() {
    val readings = mapOf(temperature.id to listOf(reading(90, 20.0), reading(60, 21.0)))
    val spec = ChartSpec.Single(temperature)
    assertEquals(2, panelOf(spec, readings, since, until).lanes.single().chart!!.lines.size)
    assertEquals(1, panelOf(spec, readings, since, until, maxGap = Duration.ofHours(1)).lanes.single().chart!!.lines.size)
  }

  @Test
  fun `the panel keeps the spec it was made from`() {
    val spec = ChartSpec.Single(temperature)
    assertEquals(spec, panelOf(spec, emptyMap(), since, until).spec)
  }

  // ---- what the catalog says of each sensor ------------------------------------------------------

  private val gas = ChartSensor(EntityId("gas-level"), "Gas", "adc")

  @Test
  fun `the gas is charted on its own scale and with the limit of the alarm`() {
    val lane = panelOf(ChartSpec.Single(gas), mapOf(gas.id to listOf(reading(30, 44.0), reading(10, 60.0))), since, until).lanes.single()
    assertEquals(ChartRange(0.0, 400.0), lane.chart!!.range)
    assertEquals(listOf(ResolvedLimit(LimitKind.Alarm, 160.0)), lane.limits)
  }

  @Test
  fun `the humidity limits are the values the house has now, and a limit the house does not have is not drawn`() {
    val humidity = ChartSensor(EntityId("humidity"), "Humidity", "%")
    val readings = mapOf(humidity.id to listOf(reading(30, 70.0)))
    val both = panelOf(ChartSpec.Single(humidity), readings, since, until, limitValues = mapOf(EntityId("dehumidifier-on-at") to 85.0, EntityId("dehumidifier-off-at") to 83.0)).lanes.single()
    assertEquals(listOf(ResolvedLimit(LimitKind.DehumidifierOn, 85.0), ResolvedLimit(LimitKind.DehumidifierOff, 83.0)), both.limits)
    val one = panelOf(ChartSpec.Single(humidity), readings, since, until, limitValues = mapOf(EntityId("dehumidifier-on-at") to 85.0)).lanes.single()
    assertEquals(listOf(ResolvedLimit(LimitKind.DehumidifierOn, 85.0)), one.limits)
    assertEquals(emptyList<ResolvedLimit>(), panelOf(ChartSpec.Single(humidity), readings, since, until).lanes.single().limits)
  }

  @Test
  fun `the limits are in the range of the chart`() {
    val humidity = ChartSensor(EntityId("humidity"), "Humidity", "%")
    val lane = panelOf(ChartSpec.Single(humidity), mapOf(humidity.id to listOf(reading(30, 70.0))), since, until, limitValues = mapOf(EntityId("dehumidifier-on-at") to 105.0)).lanes.single()
    assertEquals(ChartRange(40.0, 105.0), lane.chart!!.range)
  }

  @Test
  fun `the temperature is never on a scale narrower than ten degrees`() {
    val lane = panelOf(ChartSpec.Single(temperature), mapOf(temperature.id to listOf(reading(30, 19.0), reading(10, 21.0))), since, until).lanes.single()
    assertEquals(ChartRange(15.0, 25.0), lane.chart!!.range)
  }

  @Test
  fun `a sensor nobody knows has the range of what it read, and no limits`() {
    val wind = ChartSensor(EntityId("wind"), "Wind", "km/h")
    val lane = panelOf(ChartSpec.Single(wind), mapOf(wind.id to listOf(reading(30, 3.0), reading(10, 9.0))), since, until).lanes.single()
    assertEquals(ChartRange(3.0, 9.0), lane.chart!!.range)
    assertEquals(emptyList<ResolvedLimit>(), lane.limits)
  }

  @Test
  fun `a month of the gas is bars when the hours are given, and a month of the temperature is still a line`() {
    val hours = mapOf(gas.id to listOf(HourlyReading(until.minusSeconds(7200), 44.0, 218.0)), temperature.id to listOf(HourlyReading(until.minusSeconds(7200), 19.0, 21.0)))
    val points = mapOf(gas.id to listOf(reading(60, 44.0)), temperature.id to listOf(reading(60, 19.0)))
    val gasLane = panelOf(ChartSpec.Single(gas), points, since, until, hours = hours, zone = java.time.ZoneOffset.UTC).lanes.single()
    assertNull(gasLane.chart)
    assertEquals(listOf(218.0), gasLane.bars!!.bars.map { it.value })
    val temperatureLane = panelOf(ChartSpec.Single(temperature), points, since, until, hours = hours, zone = java.time.ZoneOffset.UTC).lanes.single()
    assertNull(temperatureLane.bars)
    assertNotNull(temperatureLane.chart)
  }

  @Test
  fun `a gas chart of the last hours, with no hours given, is a line`() {
    val lane = panelOf(ChartSpec.Single(gas), mapOf(gas.id to listOf(reading(30, 44.0))), since, until).lanes.single()
    assertNull(lane.bars)
    assertNotNull(lane.chart)
  }

  @Test
  fun `a band with bars and no line is not empty`() {
    val hours = mapOf(gas.id to listOf(HourlyReading(until.minusSeconds(7200), 44.0, 218.0)))
    assertFalse(panelOf(ChartSpec.Single(gas), emptyMap(), since, until, hours = hours, zone = java.time.ZoneOffset.UTC).isEmpty)
  }

  @Test
  fun `a band is drawn by the profile of its sensor, which may be one the board gave`() {
    val co = ChartSensor(EntityId("co-level"), "CO", "ppm", SensorKind.Gas, DeviceCatalog.profileOf(EntityId("co-level"), SensorKind.Gas, listOf(LimitSpec.Fixed(LimitKind.Alarm, 90.0))))
    val lane = panelOf(ChartSpec.Single(co), mapOf(co.id to listOf(reading(30, 12.0))), since, until).lanes.single()
    assertEquals(listOf(ResolvedLimit(LimitKind.Alarm, 90.0)), lane.limits)
    assertEquals(ChartRange(0.0, 400.0), lane.chart!!.range)
  }
}
