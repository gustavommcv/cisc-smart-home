package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTest {
  private val gasLevel = Entity.Sensor(EntityId("gas-level"), "Gas level", SensorValue.Number(142.0), unit = "adc")
  private val valve = Entity.BinarySensor(EntityId("valve-open"), "Valve open", value = true)
  private val power = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)
  private val mode =
    Entity.Select(EntityId("dehumidifier-mode"), "Mode", selected = "automatic", options = listOf("automatic", "manual"), commandable = true)

  private val gasSensor = Device(DeviceId("gas-sensor"), "Gas sensor", listOf(gasLevel))
  private val gasValve = Device(DeviceId("gas-valve"), "Gas valve", listOf(valve))
  private val dehumidifier = Device(DeviceId("dehumidifier"), "Dehumidifier", listOf(power, mode))

  private val kitchen = Area(AreaId("kitchen"), "Kitchen", listOf(gasSensor, gasValve))
  private val bedroom = Area(AreaId("bedroom"), "Bedroom", listOf(dehumidifier))
  private val home = Home(BootId("5f3a9c21"), uptimeMs = 183_450, areas = listOf(kitchen, bedroom))

  @Test
  fun `an entity is found in whichever area it is`() {
    assertEquals(gasLevel, home.findEntity(EntityId("gas-level")))
    assertEquals(valve, home.findEntity(EntityId("valve-open")))
    assertEquals(mode, home.findEntity(EntityId("dehumidifier-mode")))
  }

  @Test
  fun `an entity that does not exist is null`() {
    assertNull(home.findEntity(EntityId("no-such-thing")))
  }

  @Test
  fun `an entity is placed in its device and its area`() {
    val placement = home.locate(EntityId("dehumidifier-power"))!!
    assertEquals(bedroom, placement.area)
    assertEquals(dehumidifier, placement.device)
    assertEquals(power, placement.entity)
  }

  @Test
  fun `a house with no areas has no entities`() {
    assertNull(Home(BootId("x"), 0, emptyList()).findEntity(EntityId("gas-level")))
  }

  @Test
  fun `only switches and selects that say so are commandable`() {
    assertEquals(false, gasLevel.commandable)
    assertEquals(false, valve.commandable)
    assertEquals(true, power.commandable)
    assertEquals(true, mode.commandable)
    assertEquals(false, power.copy(commandable = false).commandable)
  }
}
