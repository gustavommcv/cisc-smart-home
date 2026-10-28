package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two limits of the dehumidifier as one thing: how far each can be moved, and the way back to the default, in an order the board accepts. */
class HumidityLimitsTest {
  private val onId = EntityId("dehumidifier-on-at")
  private val offId = EntityId("dehumidifier-off-at")

  private fun on(value: Double?, default: Double? = 85.0, step: Double = 1.0) =
    Entity.Number(onId, "Turn on at", value, "%", min = 40.0, max = 95.0, step = step, default = default, commandable = true)

  private fun off(value: Double?, default: Double? = 83.0, step: Double = 1.0) =
    Entity.Number(offId, "Turn off at", value, "%", min = 40.0, max = 95.0, step = step, default = default, commandable = true)

  private fun limits(onValue: Double, offValue: Double) = HumidityLimits.of(listOf(on(onValue), off(offValue)))!!

  // ---- finding them in a device ---------------------------------------------------------

  @Test
  fun `the limits are found among the entities of a device`() {
    val sensor = Entity.Sensor(EntityIds.Humidity, "Humidity", SensorValue.Number(70.0), "%")
    assertNotNull(HumidityLimits.of(listOf(sensor, on(85.0), off(83.0))))
  }

  @Test
  fun `without one of them there are no limits`() {
    assertNull(HumidityLimits.of(listOf(on(85.0))))
    assertNull(HumidityLimits.of(listOf(off(83.0))))
    assertNull(HumidityLimits.of(emptyList()))
  }

  @Test
  fun `a limit with no value is no limit`() {
    assertNull(HumidityLimits.of(listOf(on(null), off(83.0))))
    assertNull(HumidityLimits.of(listOf(on(85.0), off(null))))
  }

  @Test
  fun `an entity with the right id that is not a number is no limit`() {
    val impostor = Entity.Sensor(onId, "Turn on at", SensorValue.Number(85.0), "%")
    assertNull(HumidityLimits.of(listOf(impostor, off(83.0))))
  }

  @Test
  fun `the limits the app knows are the ids of the contract`() {
    assertEquals(EntityIds.DehumidifierOnAt, onId)
    assertEquals(EntityIds.DehumidifierOffAt, offId)
  }

  // ---- how far each can be moved ----------------------------------------------------------

  @Test
  fun `each limit moves by a step, within what the board accepts`() {
    val limits = limits(90.0, 80.0)
    assertEquals(91.0, limits.raiseOn()!!, 0.0)
    assertEquals(89.0, limits.lowerOn()!!, 0.0)
    assertEquals(81.0, limits.raiseOff()!!, 0.0)
    assertEquals(79.0, limits.lowerOff()!!, 0.0)
  }

  @Test
  fun `the upper limit cannot come closer than two points to the lower one`() {
    val limits = limits(85.0, 83.0)
    assertNull(limits.lowerOn())
    assertEquals(86.0, limits.raiseOn()!!, 0.0)
    assertEquals(85.0, limits(86.0, 83.0).lowerOn()!!, 0.0)
  }

  @Test
  fun `the lower limit cannot come closer than two points to the upper one`() {
    val limits = limits(85.0, 83.0)
    assertNull(limits.raiseOff())
    assertEquals(82.0, limits.lowerOff()!!, 0.0)
    assertEquals(84.0, limits(86.0, 83.0).raiseOff()!!, 0.0)
  }

  @Test
  fun `the limits do not leave the range of the board`() {
    assertNull(limits(95.0, 80.0).raiseOn())
    assertNull(limits(60.0, 40.0).lowerOff())
    assertEquals(94.0, limits(95.0, 80.0).lowerOn()!!, 0.0)
    assertEquals(41.0, limits(60.0, 40.0).raiseOff()!!, 0.0)
  }

  @Test
  fun `a limit can come down to the minimum itself, and up to the maximum itself`() {
    assertEquals(40.0, limits(60.0, 41.0).lowerOff()!!, 0.0)
    assertEquals(95.0, limits(94.0, 80.0).raiseOn()!!, 0.0)
  }

  @Test
  fun `a step of the board is the step of the app`() {
    val limits = HumidityLimits.of(listOf(on(90.0, step = 5.0), off(80.0, step = 5.0)))!!
    assertEquals(95.0, limits.raiseOn()!!, 0.0)
    assertEquals(85.0, limits.lowerOn()!!, 0.0)
    assertEquals(85.0, limits.raiseOff()!!, 0.0)
    assertEquals(75.0, limits.lowerOff()!!, 0.0)
  }

  @Test
  fun `a step that would break the gap is not offered, even when it fits the range`() {
    val limits = HumidityLimits.of(listOf(on(90.0, step = 5.0), off(86.0, step = 5.0)))!!
    assertNull(limits.raiseOff())
    assertNull(limits.lowerOn())
  }

  // ---- the default -------------------------------------------------------------------------

  @Test
  fun `the limits are the default when both are`() {
    assertTrue(limits(85.0, 83.0).isDefault)
    assertFalse(limits(86.0, 83.0).isDefault)
    assertFalse(limits(85.0, 80.0).isDefault)
  }

  @Test
  fun `without the default of the board there is nothing to go back to`() {
    val limits = HumidityLimits.of(listOf(on(90.0, default = null), off(80.0, default = null)))!!
    assertTrue(limits.isDefault)
    assertEquals(emptyList<Pair<EntityId, Double>>(), limits.restore())
  }

  @Test
  fun `going back to the default with the limits already there asks for nothing`() {
    assertEquals(emptyList<Pair<EntityId, Double>>(), limits(85.0, 83.0).restore())
  }

  @Test
  fun `only the limit that changed is asked for`() {
    assertEquals(listOf(onId to 85.0), limits(90.0, 83.0).restore())
    assertEquals(listOf(offId to 83.0), limits(85.0, 70.0).restore())
  }

  @Test
  fun `coming down from above, the lower limit goes first, so the gap is never broken`() {
    assertEquals(listOf(offId to 83.0, onId to 85.0), limits(90.0, 88.0).restore())
  }

  @Test
  fun `going up from below, the upper limit goes first`() {
    assertEquals(listOf(onId to 85.0, offId to 83.0), limits(50.0, 45.0).restore())
  }

  @Test
  fun `the upper limit goes first when its default is exactly two points from the lower limit as it is`() {
    val limits = HumidityLimits.of(listOf(on(90.0, default = 80.0), off(78.0, default = 70.0)))!!
    assertEquals(listOf(onId to 80.0, offId to 70.0), limits.restore())
  }

  @Test
  fun `the lower limit goes first when the default of the upper one is one point short of that`() {
    val limits = HumidityLimits.of(listOf(on(90.0, default = 80.0), off(79.0, default = 70.0)))!!
    assertEquals(listOf(offId to 70.0, onId to 80.0), limits.restore())
  }

  @Test
  fun `whatever the limits are, the first step of the way back is one the board accepts`() {
    var checked = 0
    for (onValue in 42..95) {
      for (offValue in 40..(onValue - 2)) {
        val commands = limits(onValue.toDouble(), offValue.toDouble()).restore()
        var currentOn = onValue.toDouble()
        var currentOff = offValue.toDouble()
        for ((id, value) in commands) {
          if (id == onId) currentOn = value else currentOff = value
          assertTrue("from $onValue/$offValue the gap is broken after $id=$value", currentOn - currentOff >= 2.0)
          assertTrue("from $onValue/$offValue the range is broken after $id=$value", currentOn <= 95.0 && currentOff >= 40.0)
        }
        assertEquals("from $onValue/$offValue it does not end at the default", 85.0 to 83.0, currentOn to currentOff)
        checked++
      }
    }
    assertTrue(checked > 1000)
  }
}
