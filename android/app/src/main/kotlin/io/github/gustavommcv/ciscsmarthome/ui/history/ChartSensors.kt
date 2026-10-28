package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.Home

/** A sensor the chart can show: the [name] is the board's, and the [unit] is what it reads in. */
internal data class ChartSensor(val id: EntityId, val name: String, val unit: String)

// The three the maquette keeps a history of (docs/api.md), in the order they are offered, with the unit each one reads in.
private val MAQUETTE = listOf(EntityIds.GasLevel to "adc", EntityIds.Temperature to "°C", EntityIds.Humidity to "%")

/**
 * The sensors the chart offers: those of the [home] that read in a unit (a count, such as the leaks, has none), the ones of the maquette first.
 * Before the board is heard there is no [home], and the history is on the phone all the same, so the three of the maquette are offered: it
 * can be read away from the board.
 */
internal fun chartSensors(home: Home?): List<ChartSensor> {
  if (home == null) return MAQUETTE.map { (id, unit) -> ChartSensor(id, id.value, unit) }
  val order = MAQUETTE.map { it.first }
  return home.areas
    .flatMap { it.devices }
    .flatMap { it.entities }
    .filterIsInstance<Entity.Sensor>()
    .mapNotNull { sensor -> sensor.unit?.let { ChartSensor(sensor.id, sensor.name, it) } }
    // The sort is stable: what the app does not know keeps the order of the house.
    .sortedBy { sensor -> order.indexOf(sensor.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
}
