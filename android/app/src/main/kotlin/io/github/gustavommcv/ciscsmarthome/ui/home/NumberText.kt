package io.github.gustavommcv.ciscsmarthome.ui.home

import java.text.NumberFormat
import java.util.Locale

/** A reading as the user reads it: no decimals for a whole number, one otherwise, the separator of the language, no grouping. */
fun formatNumber(value: Double, locale: Locale = Locale.getDefault()): String =
  NumberFormat.getNumberInstance(locale)
    .apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 1
      isGroupingUsed = false
    }
    .format(value)
