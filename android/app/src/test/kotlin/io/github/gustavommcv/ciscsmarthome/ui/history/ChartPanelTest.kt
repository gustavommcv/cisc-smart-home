package io.github.gustavommcv.ciscsmarthome.ui.history

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
}
