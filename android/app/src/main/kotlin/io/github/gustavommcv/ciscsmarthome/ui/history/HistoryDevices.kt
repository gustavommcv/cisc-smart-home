package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorKind

/** A sensor the chart can show: the [name] is the board's, and the [unit] is what it reads in. */
internal data class ChartSensor(val id: EntityId, val name: String, val unit: String)

/** A chart of a device: the [id] tells it from the others of the same device. */
internal sealed interface ChartSpec {
  val id: String

  /** The sensors it draws, the first one above. */
  val sensors: List<ChartSensor>

  /** The history of one sensor. */
  data class Single(val sensor: ChartSensor) : ChartSpec {
    override val id = "single:${sensor.id.value}"
    override val sensors = listOf(sensor)
  }

  /** Two sensors one above the other, in two bands of the same chart, to see how one follows the other. */
  data class Compare(val upper: ChartSensor, val lower: ChartSensor) : ChartSpec {
    override val id = "compare:${upper.id.value}+${lower.id.value}"
    override val sensors = listOf(upper, lower)
  }
}

/** A device the history lists, the room it is in and the charts it has. The names are the board's: the screen puts the user's and ours before them. */
internal data class HistoryDevice(val id: DeviceId, val name: String, val areaId: AreaId, val areaName: String, val charts: List<ChartSpec>)

/**
 * The devices the history lists: those of the [home] with a sensor that reads in a unit (a state or a count, such as the leaks, has none and is not
 * drawn). Before the board is heard there is no [home], and the history is on the phone all the same, so the two devices of the maquette are listed:
 * it can be read away from the board.
 */
internal fun historyDevices(home: Home?): List<HistoryDevice> {
  // The maquette is what the catalog knows: its devices are listed by what the catalog says, so the history can be read away from the board.
  if (home == null) {
    return DeviceCatalog.devices.map { device ->
      HistoryDevice(device.id, device.id.value, device.areaId, device.areaId.value, chartsOf(device.sensors.map { ChartSensor(it.id, it.id.value, it.unit) }))
    }
  }
  return home.areas.flatMap { area ->
    area.devices.mapNotNull { device ->
      val sensors = device.entities.filterIsInstance<Entity.Sensor>().mapNotNull { sensor -> sensor.unit?.let { ChartSensor(sensor.id, sensor.name, it) } }
      if (sensors.isEmpty()) null else HistoryDevice(device.id, device.name, area.id, area.name, chartsOf(sensors))
    }
  }
}

// Temperature and humidity of one device are drawn also together, the temperature above, and that chart comes first: it is what they are looked at for.
private fun chartsOf(sensors: List<ChartSensor>): List<ChartSpec> {
  val temperature = sensors.firstOrNull { DeviceCatalog.kindOf(it.id) == SensorKind.Temperature }
  val humidity = sensors.firstOrNull { DeviceCatalog.kindOf(it.id) == SensorKind.Humidity }
  val together = if (temperature != null && humidity != null) listOf(ChartSpec.Compare(temperature, humidity)) else emptyList()
  return together + sensors.map { ChartSpec.Single(it) }
}
