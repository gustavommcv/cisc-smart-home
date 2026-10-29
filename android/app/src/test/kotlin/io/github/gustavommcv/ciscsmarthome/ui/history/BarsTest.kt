package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.ChartRange
import io.github.gustavommcv.ciscsmarthome.domain.ChartScale
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The chart of a month or a year of the gas: a bar for each day, as tall as the greatest reading of that day. */
class BarsTest {
  private val saoPaulo = ZoneId.of("America/Sao_Paulo")
  private val since = Instant.parse("2026-10-01T03:00:00Z")
  private val until = Instant.parse("2026-11-01T03:00:00Z")
  private val scale = ChartScale(atLeast = 0.0..400.0)

  private fun hour(text: String, lowest: Double, highest: Double) = HourlyReading(Instant.parse(text), lowest, highest)

  private fun bars(vararg hours: HourlyReading, limits: List<Double> = emptyList()) = barsOf(hours.toList(), since, until, saoPaulo, scale, limits)

  @Test
  fun `a bar for each day that has an hour, as tall as the greatest of the day`() {
    val bars = bars(hour("2026-10-05T13:00:00Z", 44.0, 60.0), hour("2026-10-05T14:00:00Z", 45.0, 218.0), hour("2026-10-06T13:00:00Z", 44.0, 70.0))!!
    assertEquals(listOf(218.0, 70.0), bars.bars.map { it.value })
  }

  @Test
  fun `a day with no hour has no bar`() {
    val bars = bars(hour("2026-10-05T13:00:00Z", 44.0, 60.0), hour("2026-10-09T13:00:00Z", 44.0, 70.0))!!
    assertEquals(2, bars.bars.size)
  }

  @Test
  fun `a bar takes the place of its day in the window, a day being one thirty first of october`() {
    val bar = bars(hour("2026-10-01T13:00:00Z", 44.0, 60.0))!!.bars.single()
    assertEquals(0f, bar.from, 0f)
    assertEquals(1f / 31, bar.to, 0.0001f)
    val last = bars(hour("2026-10-31T13:00:00Z", 44.0, 60.0))!!.bars.single()
    assertEquals(30f / 31, last.from, 0.0001f)
    assertEquals(1f, last.to, 0.0001f)
  }

  @Test
  fun `the day is the one of the zone, so one in the morning of november in utc is of the last evening of october`() {
    val bar = bars(hour("2026-11-01T01:00:00Z", 44.0, 60.0))!!.bars.single()
    assertEquals(30f / 31, bar.from, 0.0001f)
    assertEquals(1f, bar.to, 0.0001f)
  }

  @Test
  fun `the hours outside the window are not in any bar`() {
    assertNull(bars(hour("2026-09-30T20:00:00Z", 44.0, 60.0), hour("2026-11-01T03:00:00Z", 44.0, 60.0)))
  }

  @Test
  fun `no hours are no chart`() {
    assertNull(bars())
  }

  @Test
  fun `the bars are on the scale of the gas, from zero to 400, whatever they are`() {
    val bars = bars(hour("2026-10-05T13:00:00Z", 44.0, 60.0))!!
    assertEquals(ChartRange(0.0, 400.0), bars.range)
    assertEquals(0.15f, bars.range.fractionOf(bars.bars.single().value), 0.0001f)
  }

  @Test
  fun `a day above the scale widens it for all, and the limits are in it`() {
    val bars = bars(hour("2026-10-05T13:00:00Z", 44.0, 520.0), limits = listOf(160.0))!!
    assertEquals(ChartRange(0.0, 520.0), bars.range)
  }

  @Test
  fun `the least and the greatest are of what was read in the window`() {
    val bars = bars(hour("2026-10-05T13:00:00Z", 40.0, 60.0), hour("2026-10-06T13:00:00Z", 50.0, 218.0))!!
    assertEquals(40.0, bars.min, 0.0)
    assertEquals(218.0, bars.max, 0.0)
  }

  @Test
  fun `a day that is only partly in the window is a bar from the start of the window or to the end of it`() {
    val partly = barsOf(listOf(hour("2026-10-05T13:00:00Z", 44.0, 60.0)), Instant.parse("2026-10-05T12:00:00Z"), Instant.parse("2026-10-05T18:00:00Z"), saoPaulo, scale, emptyList())!!.bars.single()
    assertEquals(0f, partly.from, 0f)
    assertEquals(1f, partly.to, 0f)
  }
}
