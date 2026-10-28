package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.R
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartPeriodTest {
  @Test
  fun `the periods are one hour, six hours, a day, a month and a year, in that order`() {
    assertEquals(listOf(Duration.ofHours(1), Duration.ofHours(6), Duration.ofHours(24), null, null), ChartPeriod.entries.map { it.duration })
  }

  @Test
  fun `the month and the year are the ones that go by the calendar, and have pages`() {
    assertEquals(listOf(false, false, false, true, true), ChartPeriod.entries.map { it.isCalendar })
  }

  @Test
  fun `the chart opens on six hours`() {
    assertEquals(ChartPeriod.SixHours, ChartPeriod.Default)
  }

  @Test
  fun `each period has its own word`() {
    assertEquals(listOf(R.string.period_1h, R.string.period_6h, R.string.period_24h, R.string.period_month, R.string.period_year), ChartPeriod.entries.map { it.labelRes })
  }
}
