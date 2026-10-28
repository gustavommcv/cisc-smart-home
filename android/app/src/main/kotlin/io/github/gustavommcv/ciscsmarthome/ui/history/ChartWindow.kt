package io.github.gustavommcv.ciscsmarthome.ui.history

import java.time.Instant
import java.time.ZoneId

/** The stretch of time a chart shows, from [since] up to [until], and which page of the [period] it is ([offset] 0 is the present). */
internal data class ChartWindow(val period: ChartPeriod, val offset: Int, val since: Instant, val until: Instant)

/**
 * The window of [period] seen at [now]. The last hours end now. A month or a year is the whole one of the calendar, from midnight of its first day to
 * midnight of the next, in [zone] (the one of the phone: the last evening of October there is October, though it is November elsewhere), and
 * [offset] goes that many months or years back. The last hours have no pages, and nothing is after the present.
 */
internal fun windowOf(period: ChartPeriod, offset: Int, now: Instant, zone: ZoneId): ChartWindow {
  val duration = period.duration
  if (duration != null) return ChartWindow(period, 0, now.minus(duration), now)
  val back = offset.coerceAtLeast(0).toLong()
  val today = now.atZone(zone).toLocalDate()
  val first =
    when (period) {
      ChartPeriod.Year -> today.withDayOfYear(1).minusYears(back)
      else -> today.withDayOfMonth(1).minusMonths(back)
    }
  val next = if (period == ChartPeriod.Year) first.plusYears(1) else first.plusMonths(1)
  return ChartWindow(period, back.toInt(), first.atStartOfDay(zone).toInstant(), next.atStartOfDay(zone).toInstant())
}
