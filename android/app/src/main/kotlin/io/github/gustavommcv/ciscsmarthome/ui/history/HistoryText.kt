package io.github.gustavommcv.ciscsmarthome.ui.history

import android.content.res.Resources
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.home.formatNumber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// A number the way the app writes numbers (a comma, no grouping), with the unit it reads in when there is one.
private fun valueText(value: Double, unit: String?): String = formatNumber(value) + (unit?.let { " $it" } ?: "")

internal fun Resources.minText(value: Double, unit: String?): String = getString(R.string.history_min, valueText(value, unit))

internal fun Resources.maxText(value: Double, unit: String?): String = getString(R.string.history_max, valueText(value, unit))

/** The label of the left end of the chart: how far back it goes. */
internal fun Resources.sinceText(period: ChartPeriod): String = getString(R.string.history_since, getString(period.labelRes))

/** What a screen reader says of a chart it cannot see: what it is, which stretch of time it shows and the range it covers. */
internal fun Resources.chartDescription(label: String, window: ChartWindow, zone: ZoneId, min: Double, max: Double, unit: String?): String =
  getString(R.string.history_chart_description, label, windowWords(window, zone), valueText(min, unit), valueText(max, unit))

/** The left end of the chart: how far back the last hours go, or the first day of the month, or the first month of the year. */
internal fun Resources.windowStartText(window: ChartWindow, zone: ZoneId): String =
  when (window.period) {
    ChartPeriod.Month -> DayAndMonth.format(window.since.atZone(zone))
    ChartPeriod.Year -> MonthOnly.format(window.since.atZone(zone))
    else -> sinceText(window.period)
  }

/** The right end of the chart: now, or the last day of the month, or the last month of the year. */
internal fun Resources.windowEndText(window: ChartWindow, zone: ZoneId): String {
  // The window ends at midnight of the next one, so its last day is the day before.
  val last = window.until.atZone(zone).minusDays(1)
  return when (window.period) {
    ChartPeriod.Month -> DayAndMonth.format(last)
    ChartPeriod.Year -> MonthOnly.format(last)
    else -> getString(R.string.history_now)
  }
}

/** The name of the month with its year, or the year; for the last hours, the word of the period. */
internal fun Resources.windowTitle(window: ChartWindow, zone: ZoneId): String =
  when (window.period) {
    ChartPeriod.Month -> MonthAndYear.format(window.since.atZone(zone)).replaceFirstChar { it.uppercase() }
    ChartPeriod.Year -> YearOnly.format(window.since.atZone(zone))
    else -> getString(window.period.labelRes)
  }

private fun Resources.windowWords(window: ChartWindow, zone: ZoneId): String =
  if (window.period.isCalendar) getString(R.string.history_window_in, windowTitle(window, zone).replaceFirstChar { it.lowercase() })
  else getString(R.string.history_window_recent, getString(window.period.labelRes))

// The app speaks Portuguese, so the names of the months are Portuguese whatever the language of the phone.
private val Portuguese: Locale = Locale.forLanguageTag("pt-BR")
private val MonthAndYear = DateTimeFormatter.ofPattern("LLLL 'de' yyyy", Portuguese)
private val DayAndMonth = DateTimeFormatter.ofPattern("d MMM", Portuguese)
private val MonthOnly = DateTimeFormatter.ofPattern("MMM", Portuguese)
private val YearOnly = DateTimeFormatter.ofPattern("yyyy", Portuguese)

/** The second line of a device of the list: the room it is in and how many charts it has. */
internal fun Resources.deviceSubtitle(room: String, charts: Int): String = getQuantityString(R.plurals.history_device_subtitle, charts, charts, room)

/** The title of the chart that compares two sensors: both names, the first one the one above. */
internal fun Resources.compareTitle(upper: String, lower: String): String = getString(R.string.history_compare_title, upper, lower.replaceFirstChar { it.lowercase() })
