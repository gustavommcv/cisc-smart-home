package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Turning what the board read into the line the screen draws: placed in the window, broken where nothing was heard, reduced without losing the peaks. */
class ChartTest {
  private val since = Instant.parse("2026-10-27T12:00:00Z")
  private val until = since.plus(Duration.ofHours(1))

  private fun reading(secondsAfterStart: Long, value: Double?) = Reading(since.plusSeconds(secondsAfterStart), value)

  private fun chart(vararg readings: Reading) = chartOf(readings.toList(), since, until)

  // ---- what is in the window ----------------------------------------------------------------

  @Test
  fun `a reading outside the window is left out`() {
    val chart = chart(reading(-10, 1.0), reading(10, 2.0), Reading(until.plusSeconds(10), 3.0))!!
    assertEquals(1, chart.lines.single().size)
    assertEquals(2.0, chart.min, 0.0)
    assertEquals(2.0, chart.max, 0.0)
  }

  @Test
  fun `a reading at the very start and at the very end is inside`() {
    val points = chart(Reading(since, 1.0), Reading(until, 2.0))!!.lines.flatten()
    assertEquals(listOf(0f, 1f), points.map { it.x })
  }

  @Test
  fun `a reading with no value is left out`() {
    val chart = chart(reading(0, 1.0), reading(10, null), reading(20, 3.0))!!
    assertEquals(1, chart.lines.size)
    assertEquals(2, chart.lines.single().size)
  }

  @Test
  fun `nothing to draw is no chart`() {
    assertNull(chart())
    assertNull(chart(reading(0, null), reading(10, null)))
    assertNull(chart(reading(-10, 1.0), Reading(until.plusSeconds(10), 1.0)))
  }

  // ---- where a point goes -----------------------------------------------------------------------

  @Test
  fun `across, a point is where its time is in the window`() {
    // Half an hour apart, so each is a line of its own: where they are is what is asked.
    val points = chart(Reading(since, 1.0), Reading(since.plus(Duration.ofMinutes(30)), 1.0), Reading(until, 1.0))!!.lines.flatten()
    assertEquals(listOf(0f, 0.5f, 1f), points.map { it.x })
  }

  @Test
  fun `up, a point is where its value is between the least and the greatest`() {
    val points = chart(reading(0, 10.0), reading(10, 20.0), reading(20, 30.0))!!.lines.single()
    assertEquals(listOf(0f, 0.5f, 1f), points.map { it.y })
  }

  @Test
  fun `a series that never changes is drawn in the middle`() {
    val points = chart(reading(0, 5.0), reading(10, 5.0))!!.lines.single()
    assertEquals(listOf(0.5f, 0.5f), points.map { it.y })
  }

  @Test
  fun `the least and the greatest are the ones that were read`() {
    val chart = chart(reading(0, 20.0), reading(10, 10.0), reading(20, 30.0), reading(30, 25.0))!!
    assertEquals(10.0, chart.min, 0.0)
    assertEquals(30.0, chart.max, 0.0)
  }

  @Test
  fun `a reading alone is a line of one point`() {
    assertEquals(1, chart(reading(0, 7.0))!!.lines.single().size)
  }

  // ---- where nothing was heard --------------------------------------------------------------------

  @Test
  fun `a gap longer than the limit breaks the line in two`() {
    val gap = ChartRules.MaxGap.seconds
    val lines = chart(reading(0, 1.0), reading(10, 1.0), reading(10 + gap + 1, 1.0), reading(20 + gap + 1, 1.0))!!.lines
    assertEquals(listOf(2, 2), lines.map { it.size })
  }

  @Test
  fun `a gap as long as the limit does not break the line`() {
    val gap = ChartRules.MaxGap.seconds
    assertEquals(1, chart(reading(0, 1.0), reading(gap, 1.0))!!.lines.size)
  }

  @Test
  fun `readings with no value count as time without readings`() {
    val gap = ChartRules.MaxGap.seconds
    // The two readings that have a value are further apart than the limit, whatever is between them.
    assertEquals(2, chart(reading(0, 1.0), reading(gap / 2, null), reading(gap + 1, 1.0))!!.lines.size)
  }

  @Test
  fun `a break does not move the points, the empty time stays empty`() {
    val gap = ChartRules.MaxGap.seconds
    val lines = chart(reading(0, 1.0), reading(gap + 1, 2.0))!!.lines
    assertEquals(0f, lines[0].single().x, 0f)
    assertEquals((gap + 1) / 3600f, lines[1].single().x, 0.0001f)
  }

  // ---- many readings -----------------------------------------------------------------------------

  private fun crowd(count: Int, valueAt: (Int) -> Double): List<Reading> = List(count) { Reading(since.plusMillis(it * 600L), valueAt(it)) }

  @Test
  fun `a crowd of readings is reduced to what a screen can draw`() {
    val chart = chartOf(crowd(5_000) { 20.0 + (it % 17) }, since, until)!!
    assertTrue("drew ${chart.lines.sumOf { it.size }} points", chart.lines.sumOf { it.size } <= 2 * ChartRules.Buckets + chart.lines.size)
  }

  @Test
  fun `a few readings are all drawn, not reduced`() {
    assertEquals(20, chartOf(crowd(20) { it.toDouble() }, since, until)!!.lines.single().size)
  }

  @Test
  fun `a short peak survives the reduction, for it is what a leak looks like`() {
    val readings = crowd(5_000) { if (it == 2_500) 250.0 else 45.0 }
    val chart = chartOf(readings, since, until)!!
    assertEquals(250.0, chart.max, 0.0)
    assertEquals(1f, chart.lines.flatten().maxOf { it.y }, 0f)
    assertEquals(0f, chart.lines.flatten().minOf { it.y }, 0f)
  }

  @Test
  fun `a short dip survives the reduction too`() {
    val readings = crowd(5_000) { if (it == 2_500) 20.0 else 250.0 }
    val chart = chartOf(readings, since, until)!!
    assertEquals(20.0, chart.min, 0.0)
    assertEquals(0f, chart.lines.flatten().minOf { it.y }, 0f)
  }

  @Test
  fun `after the reduction the points are still in the order of time`() {
    val readings = crowd(5_000) { (it * 7919 % 101).toDouble() }
    val xs = chartOf(readings, since, until)!!.lines.single().map { it.x }
    assertEquals(xs.sorted(), xs)
  }

  @Test
  fun `the reduction is of each stretch alone, so a break stays a break`() {
    val first = crowd(1_000) { 20.0 + it % 5 }
    val second = first.map { it.copy(at = it.at.plus(Duration.ofMinutes(30))) }
    val lines = chartOf(first + second, since, until)!!.lines
    assertEquals(2, lines.size)
    assertTrue(lines[0].maxOf { it.x } < lines[1].minOf { it.x })
  }

  @Test
  fun `the chart of a window with readings is a chart`() {
    assertNotNull(chart(reading(0, 1.0)))
  }

  // ---- the silence that breaks a line ---------------------------------------------------------

  @Test
  fun `a silence longer than the one given breaks the line, and a shorter one does not`() {
    val readings = listOf(reading(0, 1.0), reading(30 * 60, 2.0), reading(30 * 60 + 90 * 60, 3.0))
    val chart = chartOf(readings, since, since.plus(Duration.ofHours(3)), maxGap = Duration.ofHours(1))!!
    assertEquals(listOf(2, 1), chart.lines.map { it.size })
  }

  @Test
  fun `a silence of exactly the one given does not break the line`() {
    val chart = chartOf(listOf(reading(0, 1.0), reading(3_600, 2.0)), since, until, maxGap = Duration.ofHours(1))!!
    assertEquals(1, chart.lines.size)
  }

  @Test
  fun `without a gap given, it is the two minutes of a board that is heard every ten seconds`() {
    val chart = chartOf(listOf(reading(0, 1.0), reading(30 * 60, 2.0)), since, until)!!
    assertEquals(2, chart.lines.size)
  }
}
