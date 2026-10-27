package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GasLeakStatusTest {
  private fun kitchen(vararg entities: Entity, area: String = "kitchen") =
    Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId(area), "Area $area", listOf(Device(DeviceId("gas-sensor"), "Gas sensor", entities.toList())))))

  private fun state(value: SensorValue) = Entity.Sensor(EntityId("gas-state"), "Gas state", value)

  private fun alerts(value: SensorValue) = Entity.Sensor(EntityId("gas-alerts"), "Gas alerts", value)

  private fun text(value: String) = SensorValue.Text(value)

  private fun number(value: Double) = SensorValue.Number(value)

  @Test
  fun `an emergency is an active leak, with the number of the leak and the area it is in`() {
    val status = kitchen(state(text("emergency")), alerts(number(3.0))).gasLeakStatus()
    assertEquals(GasLeakStatus(AreaId("kitchen"), alertCount = 3, active = true), status)
  }

  @Test
  fun `a normal house has no active leak, but still says how many there were`() {
    val status = kitchen(state(text("normal")), alerts(number(2.0))).gasLeakStatus()
    assertEquals(GasLeakStatus(AreaId("kitchen"), alertCount = 2, active = false), status)
  }

  @Test
  fun `the sensor warming up is not a leak`() {
    assertEquals(false, kitchen(state(text("starting")), alerts(number(0.0))).gasLeakStatus()?.active)
  }

  @Test
  fun `a state without a valid reading is not a leak`() {
    assertEquals(false, kitchen(state(SensorValue.Unavailable), alerts(number(1.0))).gasLeakStatus()?.active)
  }

  @Test
  fun `the area is the one the gas state is in`() {
    assertEquals(AreaId("garage"), kitchen(state(text("normal")), alerts(number(0.0)), area = "garage").gasLeakStatus()?.areaId)
  }

  @Test
  fun `a house without the gas state says nothing about leaks`() {
    assertNull(kitchen(alerts(number(1.0))).gasLeakStatus())
  }

  @Test
  fun `a house without the count of alerts says nothing about leaks`() {
    assertNull(kitchen(state(text("emergency"))).gasLeakStatus())
  }

  @Test
  fun `a count of alerts that cannot be read says nothing about leaks`() {
    assertNull(kitchen(state(text("emergency")), alerts(SensorValue.Unavailable)).gasLeakStatus())
    assertNull(kitchen(state(text("emergency")), alerts(text("three"))).gasLeakStatus())
  }

  @Test
  fun `an entity with the id of the gas state that is not a sensor is not a gas state`() {
    val wrong = Entity.BinarySensor(EntityId("gas-state"), "Gas state", true)
    assertNull(kitchen(wrong, alerts(number(1.0))).gasLeakStatus())
  }
}
