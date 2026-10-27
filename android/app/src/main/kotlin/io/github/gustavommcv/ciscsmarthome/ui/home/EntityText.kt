package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.nameRes

/** The name of a room: ours in Portuguese for the rooms we know, the board's otherwise. */
@Composable
internal fun roomName(area: Area): String = area.id.nameRes()?.let { stringResource(it) } ?: area.name

/** The name of a device: ours in Portuguese for the ones we know, the board's otherwise. */
@Composable
internal fun deviceName(device: Device): String = device.id.nameRes()?.let { stringResource(it) } ?: device.name

/** The label of an entity: ours in Portuguese for the ones we know, the board's otherwise. */
@Composable
internal fun entityLabel(entity: Entity): String = entity.id.labelRes()?.let { stringResource(it) } ?: entity.name

/** What an entity reads, in words: numbers with their unit, states and modes in Portuguese. */
@Composable
internal fun entityValue(entity: Entity): String {
  val unavailable = stringResource(R.string.value_unavailable)
  return when (entity) {
    is Entity.Sensor ->
      when (val value = entity.value) {
        is SensorValue.Number -> formatNumber(value.value) + (entity.unit?.let { " $it" } ?: "")
        is SensorValue.Text -> if (entity.id == EntityIds.GasState) gasStateRes(value.value)?.let { stringResource(it) } ?: value.value else value.value
        SensorValue.Unavailable -> unavailable
      }
    is Entity.BinarySensor -> entity.value?.let { stringResource(if (it) R.string.value_yes else R.string.value_no) } ?: unavailable
    is Entity.Switch -> entity.on?.let { stringResource(if (it) R.string.value_on else R.string.value_off) } ?: unavailable
    is Entity.Select -> entity.selected?.let { option -> optionRes(option)?.let { stringResource(it) } ?: option } ?: unavailable
  }
}
