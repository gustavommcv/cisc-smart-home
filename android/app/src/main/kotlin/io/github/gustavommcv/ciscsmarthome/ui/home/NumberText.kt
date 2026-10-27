package io.github.gustavommcv.ciscsmarthome.ui.home

import java.text.NumberFormat
import java.util.Locale

// The app is in Portuguese, so its numbers are too: the phone may be set to another language, and a screen that says
// "Temperatura" next to "18.1" reads as a mistake.
private val PORTUGUESE = Locale.forLanguageTag("pt-BR")

/** A reading as the user reads it: no decimals for a whole number, one otherwise, the separator of Portuguese, no grouping. */
fun formatNumber(value: Double, locale: Locale = PORTUGUESE): String =
  NumberFormat.getNumberInstance(locale)
    .apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 1
      isGroupingUsed = false
    }
    .format(value)
