package io.github.gustavommcv.ciscsmarthome.ui.history

import android.content.res.Resources
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.home.formatNumber

// A number the way the app writes numbers (a comma, no grouping), with the unit it reads in when there is one.
private fun valueText(value: Double, unit: String?): String = formatNumber(value) + (unit?.let { " $it" } ?: "")

internal fun Resources.minText(value: Double, unit: String?): String = getString(R.string.history_min, valueText(value, unit))

internal fun Resources.maxText(value: Double, unit: String?): String = getString(R.string.history_max, valueText(value, unit))

/** The label of the left end of the chart: how far back it goes. */
internal fun Resources.sinceText(period: ChartPeriod): String = getString(R.string.history_since, getString(period.labelRes))

/** What a screen reader says of a chart it cannot see: what it is, how far back it goes and the range it covers. */
internal fun Resources.chartDescription(label: String, period: ChartPeriod, min: Double, max: Double, unit: String?): String =
  getString(R.string.history_chart_description, label, getString(period.labelRes), valueText(min, unit), valueText(max, unit))
