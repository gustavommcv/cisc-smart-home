package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** The range a chart shows: what the sensor read, widened by what the profile says must always be in it. */
class ChartScaleTest {
  private fun range(scale: ChartScale, dataMin: Double, dataMax: Double, vararg limits: Double) = scale.rangeOf(dataMin, dataMax, limits.toList())

  @Test
  fun `a scale with nothing to say shows what was read, from the least to the greatest`() {
    assertEquals(ChartRange(20.0, 25.0), range(ChartScale(), 20.0, 25.0))
  }

  @Test
  fun `a range that must always be shown is shown whole, though the readings are in a corner of it`() {
    assertEquals(ChartRange(0.0, 400.0), range(ChartScale(atLeast = 0.0..400.0), 44.0, 60.0))
  }

  @Test
  fun `a reading outside the range that must be shown widens it, above`() {
    assertEquals(ChartRange(0.0, 520.0), range(ChartScale(atLeast = 0.0..400.0), 44.0, 520.0))
  }

  @Test
  fun `a reading outside the range that must be shown widens it, below`() {
    assertEquals(ChartRange(-5.0, 100.0), range(ChartScale(atLeast = 40.0..100.0), -5.0, 70.0))
  }

  @Test
  fun `a span that is too narrow is widened around the middle of what was read`() {
    assertEquals(ChartRange(15.0, 25.0), range(ChartScale(minSpan = 10.0), 19.0, 21.0))
  }

  @Test
  fun `the numbers of a widened scale are whole, so the readings are in 15 to 25 and not in 15,2 to 25,2`() {
    assertEquals(ChartRange(15.0, 25.0), range(ChartScale(minSpan = 10.0), 17.5, 22.9))
  }

  @Test
  fun `the numbers are not made whole when the readings would not fit in them`() {
    val range = range(ChartScale(minSpan = 10.0), 14.9, 24.8)
    assertEquals(14.85, range.low, 0.0001)
    assertEquals(24.85, range.high, 0.0001)
  }

  @Test
  fun `a span that is wide enough is left as it is`() {
    assertEquals(ChartRange(12.0, 30.0), range(ChartScale(minSpan = 10.0), 12.0, 30.0))
  }

  @Test
  fun `a span of exactly the minimum is left as it is`() {
    assertEquals(ChartRange(15.0, 25.0), range(ChartScale(minSpan = 10.0), 15.0, 25.0))
  }

  @Test
  fun `a limit outside the range is brought into it, so the line of the limit is always drawn`() {
    assertEquals(ChartRange(20.0, 90.0), range(ChartScale(), 20.0, 30.0, 90.0))
    assertEquals(ChartRange(5.0, 30.0), range(ChartScale(), 20.0, 30.0, 5.0))
  }

  @Test
  fun `a limit inside the range changes nothing`() {
    assertEquals(ChartRange(0.0, 400.0), range(ChartScale(atLeast = 0.0..400.0), 44.0, 60.0, 160.0))
  }

  @Test
  fun `the place of a value in the range is from 0 at the bottom to 1 at the top`() {
    val range = ChartRange(0.0, 400.0)
    assertEquals(0f, range.fractionOf(0.0), 0f)
    assertEquals(0.4f, range.fractionOf(160.0), 0.0001f)
    assertEquals(1f, range.fractionOf(400.0), 0f)
  }

  @Test
  fun `a range of one value has the value in the middle`() {
    assertEquals(0.5f, ChartRange(7.0, 7.0).fractionOf(7.0), 0f)
  }
}
