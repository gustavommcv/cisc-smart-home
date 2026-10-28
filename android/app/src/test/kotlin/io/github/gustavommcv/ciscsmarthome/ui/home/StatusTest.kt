package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How a room, and the house, is: told from what the board read, and never better than what is known. */
class StatusTest {
  private fun gas(state: String) = Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text(state))

  private fun reading(id: String, value: Double? = 1.0) = Entity.Sensor(EntityId(id), id, if (value == null) SensorValue.Unavailable else SensorValue.Number(value))

  private fun area(id: String, vararg entities: Entity) = Area(AreaId(id), id, listOf(Device(DeviceId("device-of-$id"), "Device", entities.toList())))

  private fun house(vararg areas: Area) = Home(BootId("aaaa1111"), 1_000, areas.toList())

  // ---- a room ----------------------------------------------------------------------------

  @Test
  fun `a room whose readings all arrived, with normal gas, is fine`() {
    assertEquals(Status.Fine, area("kitchen", gas("normal"), reading("gas-level")).status())
  }

  @Test
  fun `gas in emergency is an emergency, whatever else is missing`() {
    assertEquals(Status.Emergency, area("kitchen", gas("emergency"), reading("gas-level", null)).status())
  }

  @Test
  fun `a gas sensor that is warming up is neither a leak nor fine`() {
    assertEquals(Status.Warming, area("kitchen", gas("starting"), reading("gas-level")).status())
  }

  @Test
  fun `warming up outranks readings that are missing`() {
    assertEquals(Status.Warming, area("kitchen", gas("starting"), reading("gas-level", null)).status())
  }

  @Test
  fun `each reading that did not arrive counts, of every kind of entity`() {
    val room =
      area(
        "lab",
        reading("temperature", null),
        Entity.BinarySensor(EntityId("valve-open"), "Valve", null),
        Entity.Switch(EntityId("dehumidifier-power"), "Power", on = null, commandable = true),
        Entity.Select(EntityId("dehumidifier-mode"), "Mode", selected = null, options = listOf("automatic"), commandable = true),
        Entity.Number(EntityId("dehumidifier-on-at"), "Turn on at", null, "%", min = 40.0, max = 95.0, step = 1.0, default = 85.0, commandable = true),
        reading("humidity"),
      )
    assertEquals(Status.Unavailable(5), room.status())
  }

  @Test
  fun `a state of the gas the app does not know is a reading that did not arrive`() {
    assertEquals(Status.Unavailable(1), area("kitchen", gas("smoldering"), reading("gas-level")).status())
  }

  @Test
  fun `a room without a gas sensor is judged by what it has`() {
    assertEquals(Status.Fine, area("bedroom", reading("temperature"), reading("humidity")).status())
    assertEquals(Status.Unavailable(1), area("bedroom", reading("temperature"), reading("humidity", null)).status())
  }

  @Test
  fun `a room with nothing in it has nothing wrong`() {
    assertEquals(Status.Fine, Area(AreaId("attic"), "Attic", emptyList()).status())
  }

  // ---- the house -------------------------------------------------------------------------

  private val fine = area("bedroom", reading("temperature"))
  private val missing = area("hall", reading("temperature", null))
  private val warming = area("pantry", gas("starting"))
  private val leaking = area("kitchen", gas("emergency"))

  @Test
  fun `the house is as bad as its worst room`() {
    assertEquals(Status.Emergency, house(fine, missing, warming, leaking).status())
    assertEquals(Status.Warming, house(fine, missing, warming).status())
    assertEquals(Status.Unavailable(1), house(fine, missing).status())
    assertEquals(Status.Fine, house(fine).status())
  }

  @Test
  fun `the readings missing in the rooms add up in the house`() {
    assertEquals(Status.Unavailable(2), house(missing, area("hall-2", reading("humidity", null)), fine).status())
  }

  @Test
  fun `a house without rooms has nothing wrong`() {
    assertEquals(Status.Fine, house().status())
  }

  @Test
  fun `the room in emergency is found, and none when there is none`() {
    assertEquals(leaking, house(fine, leaking).emergencyArea())
    assertNull(house(fine, warming, missing).emergencyArea())
  }

  // ---- how it looks ----------------------------------------------------------------------

  @Test
  fun `each state has its tone`() {
    assertEquals(Tone.Primary, Status.Fine.tone())
    assertEquals(Tone.Alert, Status.Emergency.tone())
    assertEquals(Tone.Neutral, Status.Warming.tone())
    assertEquals(Tone.Neutral, Status.Unavailable(3).tone())
  }

  @Test
  fun `only what is fine gets the reassuring icon`() {
    assertEquals(R.drawable.ic_verified_user, Status.Fine.iconRes())
    assertEquals(R.drawable.ic_warning, Status.Emergency.iconRes())
    assertEquals(R.drawable.ic_warning, Status.Warming.iconRes())
    assertEquals(R.drawable.ic_warning, Status.Unavailable(1).iconRes())
  }
}
