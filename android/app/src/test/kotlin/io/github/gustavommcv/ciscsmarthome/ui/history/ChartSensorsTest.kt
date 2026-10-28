package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which sensors the chart offers: the ones with a unit, the known ones first, and the three of the maquete until the board is heard. */
class ChartSensorsTest {
  private fun sensor(id: String, unit: String?, value: SensorValue = SensorValue.Number(1.0)) = Entity.Sensor(EntityId(id), "Name of $id", value, unit)

  private fun house(vararg entities: Entity) = Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("room"), "Room", listOf(Device(DeviceId("device"), "Device", entities.toList())))))

  private fun ids(sensors: List<ChartSensor>) = sensors.map { it.id.value }

  @Test
  fun `the sensors that read a number with a unit are offered`() {
    val sensors = chartSensors(house(sensor("gas-level", "adc"), sensor("temperature", "°C")))
    assertEquals(listOf("gas-level", "temperature"), ids(sensors))
    assertEquals(listOf("adc", "°C"), sensors.map { it.unit })
  }

  @Test
  fun `a sensor that is only a count, with no unit, is not offered`() {
    assertEquals(listOf("humidity"), ids(chartSensors(house(sensor("gas-alerts", null), sensor("humidity", "%")))))
  }

  @Test
  fun `what is not a sensor, or reads a word, is not offered`() {
    val entities =
      arrayOf(
        Entity.BinarySensor(EntityId("valve-open"), "Valve", true),
        Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true),
        Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic"), commandable = true),
        sensor("gas-state", null, SensorValue.Text("normal")),
        sensor("humidity", "%"),
      )
    assertEquals(listOf("humidity"), ids(chartSensors(house(*entities))))
  }

  @Test
  fun `a sensor with no reading now is still offered, for the history has what it read before`() {
    assertEquals(listOf("temperature"), ids(chartSensors(house(sensor("temperature", "°C", SensorValue.Unavailable)))))
  }

  @Test
  fun `the sensors of the maquete come first, in their order, then the others in the order of the house`() {
    val sensors = chartSensors(house(sensor("wind", "km/h"), sensor("humidity", "%"), sensor("gas-level", "adc"), sensor("rain", "mm"), sensor("temperature", "°C")))
    assertEquals(listOf("gas-level", "temperature", "humidity", "wind", "rain"), ids(sensors))
  }

  @Test
  fun `the name the board gave is kept, for a sensor the app has no words for`() {
    assertEquals("Name of wind", chartSensors(house(sensor("wind", "km/h"))).single().name)
  }

  @Test
  fun `without a house, the three sensors of the maquete are offered, so the history can be read away from the board`() {
    val sensors = chartSensors(null)
    assertEquals(listOf("gas-level", "temperature", "humidity"), ids(sensors))
    assertEquals(listOf("adc", "°C", "%"), sensors.map { it.unit })
  }

  @Test
  fun `a house with no sensors to chart offers none`() {
    assertEquals(emptyList<ChartSensor>(), chartSensors(house(sensor("gas-alerts", null))))
  }
}
