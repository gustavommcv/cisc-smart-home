package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How a device is shown: its icon, the line under its name, what goes in its card and when it is the one with the leak. */
class DevicesTest {
  private fun sensor(id: String, value: SensorValue = SensorValue.Number(1.0)) = Entity.Sensor(EntityId(id), id, value)

  private val gasState = sensor("gas-state", SensorValue.Text("normal"))
  private val gasLevel = sensor("gas-level")
  private val gasAlerts = sensor("gas-alerts")
  private val valve = Entity.BinarySensor(EntityId("valve-open"), "Valve", true)
  private val temperature = sensor("temperature")
  private val power = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)
  private val mode = Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic", "manual"), commandable = true)

  private val gasMonitor = Device(DeviceId("gas-monitor"), "Gas monitor", listOf(gasLevel, gasState, gasAlerts, valve))
  private val closetMonitor = Device(DeviceId("closet-monitor"), "Closet monitor", listOf(temperature, power, mode))
  private val stranger = Device(DeviceId("doorbell"), "Doorbell", listOf(sensor("button"), sensor("battery")))

  @Test
  fun `the gas monitor is a flame, the closet monitor a drop, and a device nobody knows a sensor`() {
    assertEquals(R.drawable.ic_local_fire_department, gasMonitor.id.iconRes())
    assertEquals(R.drawable.ic_water_drop, closetMonitor.id.iconRes())
    assertEquals(R.drawable.ic_sensors, stranger.id.iconRes())
  }

  @Test
  fun `the gas monitor says under its name how the gas is, and nothing else`() {
    assertEquals(Headline(gasState, withLabel = false), gasMonitor.headline())
  }

  @Test
  fun `the closet monitor says under its name what the dehumidifier is doing, with the word for it`() {
    assertEquals(Headline(power, withLabel = true), closetMonitor.headline())
  }

  @Test
  fun `a device nobody knows says nothing under its name`() {
    assertNull(stranger.headline())
  }

  @Test
  fun `a known device without the entity of its line says nothing under its name`() {
    assertNull(Device(DeviceId("gas-monitor"), "Gas monitor", listOf(gasLevel)).headline())
  }

  @Test
  fun `what is said under the name is not said again in the rows, when it is only read`() {
    assertEquals(listOf(gasLevel, gasAlerts, valve), gasMonitor.rows())
  }

  @Test
  fun `what is said under the name stays in the rows when it is also a control`() {
    assertEquals(listOf(temperature, power, mode), closetMonitor.rows())
  }

  @Test
  fun `a device nobody knows shows every entity it has, in order`() {
    assertEquals(stranger.entities, stranger.rows())
  }

  @Test
  fun `the device that reads gas in emergency is the one with the leak`() {
    assertTrue(Device(DeviceId("gas-monitor"), "Gas monitor", listOf(sensor("gas-state", SensorValue.Text("emergency")))).isInEmergency())
  }

  @Test
  fun `a device that reads normal gas, warming gas or none is not the one with the leak`() {
    assertFalse(gasMonitor.isInEmergency())
    assertFalse(Device(DeviceId("gas-monitor"), "Gas monitor", listOf(sensor("gas-state", SensorValue.Text("starting")))).isInEmergency())
    assertFalse(closetMonitor.isInEmergency())
  }
}
