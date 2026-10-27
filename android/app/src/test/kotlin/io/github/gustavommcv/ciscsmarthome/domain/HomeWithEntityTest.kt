package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeWithEntityTest {
  private val power = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)
  private val mode = Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic", "manual"), commandable = true)
  private val humidity = Entity.Sensor(EntityId("humidity"), "Humidity", SensorValue.Number(74.1))
  private val gas = Entity.Sensor(EntityId("gas-level"), "Gas level", SensorValue.Number(142.0))

  private val home =
    Home(
      BootId("aaaa1111"),
      5_000,
      listOf(
        Area(AreaId("kitchen"), "Kitchen", listOf(Device(DeviceId("gas-sensor"), "Gas sensor", listOf(gas)))),
        Area(AreaId("bedroom"), "Bedroom", listOf(Device(DeviceId("closet"), "Closet", listOf(humidity)), Device(DeviceId("dehumidifier"), "Dehumidifier", listOf(power, mode)))),
      ),
    )

  @Test
  fun `the entity is replaced where it is, and nothing else changes`() {
    val on = power.copy(on = true)
    val changed = home.withEntity(on)
    assertEquals(on, changed.findEntity(power.id))
    assertEquals(mode, changed.findEntity(mode.id))
    assertEquals(humidity, changed.findEntity(humidity.id))
    assertEquals(gas, changed.findEntity(gas.id))
    assertEquals(home.bootId, changed.bootId)
    assertEquals(home.uptimeMs, changed.uptimeMs)
  }

  @Test
  fun `the entity keeps its place among the others`() {
    val changed = home.withEntity(power.copy(on = true))
    val bedroom = changed.areas[1]
    assertEquals(listOf(power.id, mode.id), bedroom.devices[1].entities.map { it.id })
    assertEquals(listOf(AreaId("kitchen"), AreaId("bedroom")), changed.areas.map { it.id })
  }

  @Test
  fun `an entity the house does not have changes nothing`() {
    val stranger = Entity.Sensor(EntityId("smoke"), "Smoke", SensorValue.Number(1.0))
    assertSame(home, home.withEntity(stranger))
  }
}
