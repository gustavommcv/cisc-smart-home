package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.EntityId

// The words of the screen for what the board names in English. What the app does not know is shown as the board says it.

/** The label of an entity of the maquete, or null for one the app does not know. */
@StringRes
internal fun EntityId.labelRes(): Int? =
  when (value) {
    "gas-level" -> R.string.label_gas_level
    "gas-state" -> R.string.label_gas_state
    "gas-alerts" -> R.string.label_gas_alerts
    "valve-open" -> R.string.label_valve_open
    "temperature" -> R.string.label_temperature
    "humidity" -> R.string.label_humidity
    "dehumidifier-power" -> R.string.label_dehumidifier_power
    "dehumidifier-mode" -> R.string.label_dehumidifier_mode
    else -> null
  }

/** The word for a state of the gas sensor, or null for one the app does not know. */
@StringRes
internal fun gasStateRes(state: String): Int? =
  when (state) {
    "starting" -> R.string.gas_starting
    "normal" -> R.string.gas_normal
    "emergency" -> R.string.gas_emergency
    else -> null
  }

/** The word for an option of a selector (the mode of the dehumidifier), or null for one the app does not know. */
@StringRes
internal fun optionRes(option: String): Int? =
  when (option) {
    "automatic" -> R.string.option_automatic
    "manual" -> R.string.option_manual
    else -> null
  }
