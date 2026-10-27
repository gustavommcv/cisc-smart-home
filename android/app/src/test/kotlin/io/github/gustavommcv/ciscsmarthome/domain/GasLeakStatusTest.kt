package io.github.gustavommcv.ciscsmarthome.domain

import io.github.gustavommcv.ciscsmarthome.testing.houseOfTheMaquete
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

  // ---- when a warned leak is over -------------------------------------------------------

  private fun home(state: String, alerts: Double, boot: String = "aaaa1111") = houseOfTheMaquete(BootId(boot), 1_000, state, alerts.toLong())

  private fun warned(count: Long, boot: String = "aaaa1111") = GasAlert(BootId(boot), AreaId("kitchen"), count)

  @Test
  fun `a leak is over when its boot says there is no emergency and the count has reached it`() {
    assertTrue(warned(count = 3).isOverIn(home("normal", alerts = 3.0)))
  }

  @Test
  fun `a leak is not over while the house is still in emergency`() {
    assertFalse(warned(count = 3).isOverIn(home("emergency", alerts = 3.0)))
  }

  @Test
  fun `a leak is not over because of a home from before it began`() {
    // The home counted two leaks and says normal; the leak warned of is the third.
    assertFalse(warned(count = 3).isOverIn(home("normal", alerts = 2.0)))
  }

  @Test
  fun `a leak is over when a later leak has also ended`() {
    assertTrue(warned(count = 3).isOverIn(home("normal", alerts = 4.0)))
  }

  @Test
  fun `a leak of another boot is not ended by this boot`() {
    assertFalse(warned(count = 1, boot = "aaaa1111").isOverIn(home("normal", alerts = 5.0, boot = "bbbb2222")))
  }

  @Test
  fun `a house that says nothing about gas does not end a leak`() {
    assertFalse(warned(count = 1).isOverIn(Home(BootId("aaaa1111"), 1_000, emptyList())))
  }

  @Test
  fun `the sensor warming up counts as no emergency, so it ends the leak of its boot`() {
    assertTrue(warned(count = 1).isOverIn(home("starting", alerts = 1.0)))
  }
}
