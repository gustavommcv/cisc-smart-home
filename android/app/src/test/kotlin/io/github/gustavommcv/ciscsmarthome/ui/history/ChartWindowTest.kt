package io.github.gustavommcv.ciscsmarthome.ui.history

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** The stretch of time a chart shows: the last hours up to now, or a month or a year of the calendar, whole, in the time zone of the phone. */
class ChartWindowTest {
  private val saoPaulo = ZoneId.of("America/Sao_Paulo")
  private val lisbon = ZoneId.of("Europe/Lisbon")

  private fun at(text: String) = Instant.parse(text)

  private fun window(period: ChartPeriod, offset: Int, now: String, zone: ZoneId = saoPaulo) = windowOf(period, offset, at(now), zone)

  // ---- the last hours ---------------------------------------------------------------------

  @Test
  fun `the last hours end now and start that long before`() {
    val now = "2026-10-27T12:00:00Z"
    ChartPeriod.entries.filter { !it.isCalendar }.forEach { period ->
      val window = window(period, 0, now)
      assertEquals(at(now), window.until)
      assertEquals(at(now).minus(period.duration), window.since)
    }
  }

  @Test
  fun `the last hours have no pages, so a page is not looked at`() {
    assertEquals(window(ChartPeriod.SixHours, 0, "2026-10-27T12:00:00Z"), window(ChartPeriod.SixHours, 3, "2026-10-27T12:00:00Z").copy(offset = 0))
    assertEquals(0, window(ChartPeriod.SixHours, 3, "2026-10-27T12:00:00Z").offset)
  }

  // ---- a month ---------------------------------------------------------------------------

  @Test
  fun `the month is the whole calendar month, from midnight of the first day in the zone of the phone`() {
    val window = window(ChartPeriod.Month, 0, "2026-10-27T12:00:00Z")
    assertEquals(at("2026-10-01T03:00:00Z"), window.since)
    assertEquals(at("2026-11-01T03:00:00Z"), window.until)
  }

  @Test
  fun `the page before is the month before`() {
    val window = window(ChartPeriod.Month, 1, "2026-10-27T12:00:00Z")
    assertEquals(at("2026-09-01T03:00:00Z"), window.since)
    assertEquals(at("2026-10-01T03:00:00Z"), window.until)
    assertEquals(1, window.offset)
  }

  @Test
  fun `going back from january goes to december of the year before`() {
    val window = window(ChartPeriod.Month, 1, "2026-01-15T12:00:00Z")
    assertEquals(at("2025-12-01T03:00:00Z"), window.since)
    assertEquals(at("2026-01-01T03:00:00Z"), window.until)
  }

  @Test
  fun `a page many months back is that month`() {
    val window = window(ChartPeriod.Month, 14, "2026-10-27T12:00:00Z")
    assertEquals(at("2025-08-01T03:00:00Z"), window.since)
    assertEquals(at("2025-09-01T03:00:00Z"), window.until)
  }

  @Test
  fun `february of a leap year has twenty nine days`() {
    val window = window(ChartPeriod.Month, 0, "2028-02-10T12:00:00Z")
    assertEquals(Duration.ofDays(29), Duration.between(window.since, window.until))
  }

  @Test
  fun `february of a year that is not a leap year has twenty eight`() {
    val window = window(ChartPeriod.Month, 0, "2027-02-10T12:00:00Z")
    assertEquals(Duration.ofDays(28), Duration.between(window.since, window.until))
  }

  @Test
  fun `which month it is depends on the zone, and the last evening of october in the phone is not november`() {
    val window = window(ChartPeriod.Month, 0, "2026-11-01T01:00:00Z")
    assertEquals(at("2026-10-01T03:00:00Z"), window.since)
  }

  @Test
  fun `the month follows the clock of the calendar, so the month of the change of the clock has a hour less`() {
    val window = window(ChartPeriod.Month, 0, "2027-03-15T12:00:00Z", lisbon)
    assertEquals(at("2027-03-01T00:00:00Z"), window.since)
    assertEquals(at("2027-03-31T23:00:00Z"), window.until)
  }

  // ---- a year ----------------------------------------------------------------------------

  @Test
  fun `the year is the whole calendar year`() {
    val window = window(ChartPeriod.Year, 0, "2026-10-27T12:00:00Z")
    assertEquals(at("2026-01-01T03:00:00Z"), window.since)
    assertEquals(at("2027-01-01T03:00:00Z"), window.until)
  }

  @Test
  fun `the page before is the year before, and a leap year has three hundred and sixty six days`() {
    val window = window(ChartPeriod.Year, 2, "2026-10-27T12:00:00Z")
    assertEquals(at("2024-01-01T03:00:00Z"), window.since)
    assertEquals(at("2025-01-01T03:00:00Z"), window.until)
    assertEquals(Duration.ofDays(366), Duration.between(window.since, window.until))
  }

  @Test
  fun `the first day of the year in the phone is the year, though it is still the year before in utc`() {
    val window = window(ChartPeriod.Year, 0, "2027-01-01T04:00:00Z")
    assertEquals(at("2027-01-01T03:00:00Z"), window.since)
  }

  @Test
  fun `a page before the present is not a page after it`() {
    assertEquals(window(ChartPeriod.Month, 0, "2026-10-27T12:00:00Z"), window(ChartPeriod.Month, -4, "2026-10-27T12:00:00Z"))
  }

  @Test
  fun `a window says which period it is of`() {
    assertEquals(ChartPeriod.Year, window(ChartPeriod.Year, 0, "2026-10-27T12:00:00Z").period)
  }
}
