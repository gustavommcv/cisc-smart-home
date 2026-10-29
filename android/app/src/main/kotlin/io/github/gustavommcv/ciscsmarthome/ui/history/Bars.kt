package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.ChartRange
import io.github.gustavommcv.ciscsmarthome.domain.ChartScale
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A day of the chart: where it is in the window, [from] and [to] of 0 (the start) to 1 (the end), and the greatest reading of the day. */
internal data class Bar(val from: Float, val to: Float, val value: Double)

/** What the screen draws for a month or a year of the gas: a bar for each day with an hour, on a [range] that is the same for all of them. */
internal data class Bars(val bars: List<Bar>, val min: Double, val max: Double, val range: ChartRange)

/**
 * The bars of the [hours] that are in the window from [since] to [until]: one for each day of [zone] (a day is the one of the phone) that has an hour,
 * as tall as the greatest value of its hours, or null when no hour is in the window. A peak of seconds is the greatest of its hour, so it is the bar.
 */
internal fun barsOf(hours: List<HourlyReading>, since: Instant, until: Instant, zone: ZoneId, scale: ChartScale, limits: List<Double>): Bars? {
  val inside = hours.filter { !it.hour.isBefore(since) && it.hour.isBefore(until) }
  if (inside.isEmpty()) return null
  val window = Duration.between(since, until).toMillis().coerceAtLeast(1)
  fun fractionOf(at: Instant) = (Duration.between(since, at).toMillis().toFloat() / window).coerceIn(0f, 1f)
  val bars =
    inside
      .groupBy { it.hour.atZone(zone).toLocalDate() }
      .toSortedMap()
      .map { (day, ofTheDay) ->
        Bar(fractionOf(day.atStartOfDay(zone).toInstant()), fractionOf(day.plusDays(1).atStartOfDay(zone).toInstant()), ofTheDay.maxOf { it.highest })
      }
  val min = inside.minOf { it.lowest }
  val max = inside.maxOf { it.highest }
  return Bars(bars, min, max, scale.rangeOf(min, max, limits))
}
