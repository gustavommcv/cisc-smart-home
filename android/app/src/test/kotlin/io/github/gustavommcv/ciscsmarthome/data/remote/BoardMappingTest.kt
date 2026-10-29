package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.SensorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the board says of a sensor, besides its value: what it is, and the lines to draw over its chart. */
class BoardMappingTest {
  private fun sensor(json: String): Entity.Sensor = BoardJson.decodeFromString<EntityDto>(json).toDomain() as Entity.Sensor

  private fun sensorWith(extra: String) = sensor("""{"id":"gas-level","kind":"sensor","name":"Gas level","value":142,"unit":"adc"$extra}""")

  @Test
  fun `a sensor that says nothing of itself has no class and no limits`() {
    val sensor = sensorWith("")
    assertNull(sensor.sensorClass)
    assertEquals(emptyList<LimitSpec>(), sensor.limits)
  }

  @Test
  fun `the class of a sensor is the one the board says`() {
    assertEquals(SensorKind.Gas, sensorWith(""","class":"gas"""").sensorClass)
    assertEquals(SensorKind.Temperature, sensorWith(""","class":"temperature"""").sensorClass)
    assertEquals(SensorKind.Humidity, sensorWith(""","class":"humidity"""").sensorClass)
  }

  @Test
  fun `a class the app does not know is no class, and the sensor is still read`() {
    val sensor = sensorWith(""","class":"radiation"""")
    assertNull(sensor.sensorClass)
    assertEquals(EntityId("gas-level"), sensor.id)
  }

  @Test
  fun `a limit with a value is a fixed line`() {
    assertEquals(listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 160.0)), sensorWith(""","limits":[{"kind":"alarm","value":160}]""").limits)
  }

  @Test
  fun `a limit with an entity is the line of the value of that entity`() {
    val limits = sensorWith(""","limits":[{"kind":"turns-on","entity":"dehumidifier-on-at"},{"kind":"turns-off","entity":"dehumidifier-off-at"}]""").limits
    assertEquals(listOf<LimitSpec>(LimitSpec.OfEntity(LimitKind.DehumidifierOn, EntityId("dehumidifier-on-at")), LimitSpec.OfEntity(LimitKind.DehumidifierOff, EntityId("dehumidifier-off-at"))), limits)
  }

  @Test
  fun `a limit of a kind the app does not know, or with neither a value nor an entity, is left out`() {
    val limits = sensorWith(""","limits":[{"kind":"nuclear","value":5},{"kind":"alarm"},{"kind":"alarm","value":160}]""").limits
    assertEquals(listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 160.0)), limits)
  }

  @Test
  fun `a limit with a value and an entity is the value`() {
    assertEquals(listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 7.0)), sensorWith(""","limits":[{"kind":"alarm","value":7,"entity":"x"}]""").limits)
  }

  @Test
  fun `the class and the limits do not change what a sensor reads`() {
    val sensor = sensorWith(""","class":"gas","limits":[{"kind":"alarm","value":160}]""")
    assertEquals(Entity.Sensor(EntityId("gas-level"), "Gas level", io.github.gustavommcv.ciscsmarthome.domain.SensorValue.Number(142.0), "adc", SensorKind.Gas, listOf(LimitSpec.Fixed(LimitKind.Alarm, 160.0))), sensor)
  }
}
