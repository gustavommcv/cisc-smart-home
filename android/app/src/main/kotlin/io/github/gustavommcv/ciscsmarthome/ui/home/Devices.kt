package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.DrawableRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.DeviceIcon
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.GasStates
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue

/** The entity a device says under its name, and whether it says the word for it too ("Desumidificador desligado") or only its value ("Normal"). */
internal data class Headline(val entity: Entity, val withLabel: Boolean)

/** The icon of a device, as the catalog says: a flame for the gas monitor, a drop for the closet monitor, and a sensor for one the app does not know. */
@DrawableRes
internal fun DeviceId.iconRes(): Int =
  when (DeviceCatalog.of(this)?.icon) {
    DeviceIcon.Flame -> R.drawable.ic_local_fire_department
    DeviceIcon.Drop -> R.drawable.ic_water_drop
    DeviceIcon.Sensor, null -> R.drawable.ic_sensors
  }

/** What a device says under its name, as the catalog says, or null when it has nothing to say (a device the app does not know, or one without that entity). */
internal fun Device.headline(): Headline? {
  val spec = DeviceCatalog.of(id)?.headline ?: return null
  return entities.firstOrNull { it.id == spec.entity }?.let { Headline(it, spec.withLabel) }
}

/** The entities that go in the rows of the card. What is said under the name is not said twice, unless it is also a control. */
internal fun Device.rows(): List<Entity> {
  val said = headline()?.entity?.takeIf { !it.commandable }
  return entities.filter { it.id != said?.id }
}

/** Whether this device reads gas in emergency: it is the one with the leak. */
internal fun Device.isInEmergency(): Boolean =
  entities.any { it.id == EntityIds.GasState && ((it as? Entity.Sensor)?.value as? SensorValue.Text)?.value == GasStates.Emergency }
