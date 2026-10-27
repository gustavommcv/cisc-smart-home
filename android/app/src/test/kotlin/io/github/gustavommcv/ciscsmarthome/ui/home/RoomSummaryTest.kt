package io.github.gustavommcv.ciscsmarthome.ui.home

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomSummaryTest {
  private fun sensor(id: String, value: SensorValue = SensorValue.Number(1.0)) = Entity.Sensor(EntityId(id), id, value)

  private val power = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)

  private fun area(id: String, vararg devices: List<Entity>) = Area(AreaId(id), id, devices.mapIndexed { i, entities -> Device(DeviceId("d$i"), "d$i", entities) })

  private fun ids(entities: List<Entity>) = entities.map { it.id.value }

  // ---- what a card says about a room ------------------------------------------------------

  @Test
  fun `the kitchen is summed up by the gas state and the gas level`() {
    val kitchen = area("kitchen", listOf(sensor("gas-alerts"), sensor("gas-level"), sensor("gas-state")), listOf(Entity.BinarySensor(EntityId("valve-open"), "Valve", true)))
    assertEquals(listOf("gas-state", "gas-level"), ids(kitchen.highlights()))
  }

  @Test
  fun `the bedroom is summed up by the temperature, the humidity and the dehumidifier`() {
    val bedroom = area("bedroom", listOf(sensor("humidity"), sensor("temperature")), listOf(power, Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic"), true)))
    assertEquals(listOf("temperature", "humidity", "dehumidifier-power"), ids(bedroom.highlights()))
  }

  @Test
  fun `a card never says more than three things`() {
    val crowded = area("lab", listOf(sensor("gas-state"), sensor("gas-level"), sensor("temperature"), sensor("humidity"), power))
    assertEquals(3, crowded.highlights().size)
    assertEquals(listOf("gas-state", "gas-level", "temperature"), ids(crowded.highlights()))
  }

  @Test
  fun `a room of things the app does not know is summed up by the first things it has`() {
    val garage = area("garage", listOf(sensor("door"), sensor("light"), sensor("fan"), sensor("lock")))
    assertEquals(listOf("door", "light", "fan"), ids(garage.highlights()))
  }

  @Test
  fun `a room with nothing in it says nothing`() {
    assertEquals(emptyList<Entity>(), area("empty").highlights())
  }

  @Test
  fun `a room that has only some of the known things is summed up by those`() {
    val partial = area("kitchen", listOf(sensor("gas-state"), sensor("door")))
    assertEquals(listOf("gas-state"), ids(partial.highlights()))
  }

  // ---- when a room is in emergency ----------------------------------------------------------

  private fun house(vararg areas: Area) = Home(BootId("aaaa1111"), 1_000, areas.toList())

  private fun gasRoom(id: String, state: String, alerts: Double = 1.0) =
    area(id, listOf(sensor("gas-state", SensorValue.Text(state)), sensor("gas-alerts", SensorValue.Number(alerts))))

  @Test
  fun `the room with the leak is in emergency`() {
    val kitchen = gasRoom("kitchen", "emergency")
    assertTrue(house(kitchen, area("bedroom")).isInEmergency(kitchen))
  }

  @Test
  fun `a room is not in emergency when the gas is normal`() {
    val kitchen = gasRoom("kitchen", "normal")
    assertFalse(house(kitchen).isInEmergency(kitchen))
  }

  @Test
  fun `only the room that has the gas sensor is in emergency`() {
    val kitchen = gasRoom("kitchen", "emergency")
    val bedroom = area("bedroom", listOf(sensor("temperature")))
    assertFalse(house(kitchen, bedroom).isInEmergency(bedroom))
  }

  @Test
  fun `a house that says nothing about gas has no room in emergency`() {
    val bedroom = area("bedroom", listOf(sensor("temperature")))
    assertFalse(house(bedroom).isInEmergency(bedroom))
  }
}
