package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.DehumidifierModes
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceIds
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.GasStates

// The words of the screen for what the board names in English. What the app does not know is shown as the board says it.

/** The label of an entity of the maquete, or null for one the app does not know. */
@StringRes
internal fun EntityId.labelRes(): Int? =
  when (this) {
    EntityIds.GasLevel -> R.string.label_gas_level
    EntityIds.GasState -> R.string.label_gas_state
    EntityIds.GasAlerts -> R.string.label_gas_alerts
    EntityIds.ValveOpen -> R.string.label_valve_open
    EntityIds.Temperature -> R.string.label_temperature
    EntityIds.Humidity -> R.string.label_humidity
    EntityIds.DehumidifierPower -> R.string.label_dehumidifier_power
    EntityIds.DehumidifierMode -> R.string.label_dehumidifier_mode
    EntityIds.DehumidifierOnAt -> R.string.label_dehumidifier_on_at
    EntityIds.DehumidifierOffAt -> R.string.label_dehumidifier_off_at
    else -> null
  }

/** The name of a device of the maquete, or null for one the app does not know. */
@StringRes
internal fun DeviceId.nameRes(): Int? =
  when (this) {
    DeviceIds.GasMonitor -> R.string.device_gas_monitor
    DeviceIds.ClosetMonitor -> R.string.device_closet_monitor
    else -> null
  }

/** The word for a state of the gas sensor, or null for one the app does not know. */
@StringRes
internal fun gasStateRes(state: String): Int? =
  when (state) {
    GasStates.Starting -> R.string.gas_starting
    GasStates.Normal -> R.string.gas_normal
    GasStates.Emergency -> R.string.gas_emergency
    else -> null
  }

/** The word for an option of a selector (the mode of the dehumidifier), or null for one the app does not know. */
@StringRes
internal fun optionRes(option: String): Int? =
  when (option) {
    DehumidifierModes.Automatic -> R.string.option_automatic
    DehumidifierModes.Manual -> R.string.option_manual
    else -> null
  }
