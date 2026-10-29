package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the app knows about the devices of the maquette, in one place: where each is, its icon, what it says, and how each of its sensors is charted. */
class DeviceCatalogTest {
  @Test
  fun `the maquette has the gas monitor in the kitchen and the closet monitor in the bedroom, in that order`() {
    assertEquals(listOf(DeviceIds.GasMonitor to AreaIds.Kitchen, DeviceIds.ClosetMonitor to AreaIds.Bedroom), DeviceCatalog.devices.map { it.id to it.areaId })
  }

  @Test
  fun `a device is found by its id, and one nobody knows is not found`() {
    assertEquals(DeviceIds.GasMonitor, DeviceCatalog.of(DeviceIds.GasMonitor)?.id)
    assertNull(DeviceCatalog.of(DeviceId("doorbell")))
  }

  @Test
  fun `the gas monitor is a flame and says how the gas is, the closet monitor is a drop and says what the dehumidifier does`() {
    val gas = DeviceCatalog.of(DeviceIds.GasMonitor)!!
    val closet = DeviceCatalog.of(DeviceIds.ClosetMonitor)!!
    assertEquals(DeviceIcon.Flame to HeadlineSpec(EntityIds.GasState, withLabel = false), gas.icon to gas.headline)
    assertEquals(DeviceIcon.Drop to HeadlineSpec(EntityIds.DehumidifierPower, withLabel = true), closet.icon to closet.headline)
  }

  @Test
  fun `each device lists the sensors it is charted by, with their unit and kind`() {
    assertEquals(listOf(Triple(EntityIds.GasLevel, SensorKind.Gas, "adc")), DeviceCatalog.of(DeviceIds.GasMonitor)!!.sensors.map { Triple(it.id, it.kind, it.unit) })
    assertEquals(
      listOf(Triple(EntityIds.Temperature, SensorKind.Temperature, "°C"), Triple(EntityIds.Humidity, SensorKind.Humidity, "%")),
      DeviceCatalog.of(DeviceIds.ClosetMonitor)!!.sensors.map { Triple(it.id, it.kind, it.unit) },
    )
  }

  // ---- how each sensor is charted ----------------------------------------------------------

  @Test
  fun `the gas is charted on a scale that always shows from zero to 400, with the alarm limit, filled below, and a bar for each day in a month`() {
    val chart = DeviceCatalog.chartOf(EntityIds.GasLevel)
    assertEquals(0.0..400.0, chart.scale.atLeast)
    assertNull(chart.scale.minSpan)
    assertEquals(listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 160.0)), chart.limits)
    assertTrue(chart.fillBelow)
    assertEquals(LongPeriods.DailyPeaks, chart.longPeriods)
  }

  @Test
  fun `the alarm limit of the gas is the one of the firmware`() {
    assertEquals(160.0, (DeviceCatalog.chartOf(EntityIds.GasLevel).limits.single() as LimitSpec.Fixed).value, 0.0)
  }

  @Test
  fun `the humidity is charted from the limits the user chose for the dehumidifier, which are entities of the house`() {
    val chart = DeviceCatalog.chartOf(EntityIds.Humidity)
    assertEquals(40.0..100.0, chart.scale.atLeast)
    assertEquals(listOf<LimitSpec>(LimitSpec.OfEntity(LimitKind.DehumidifierOn, EntityIds.DehumidifierOnAt), LimitSpec.OfEntity(LimitKind.DehumidifierOff, EntityIds.DehumidifierOffAt)), chart.limits)
    assertFalse(chart.fillBelow)
    assertEquals(LongPeriods.Envelope, chart.longPeriods)
  }

  @Test
  fun `the temperature is charted on a scale that is never narrower than 10 degrees, so the noise of the sensor is not drama`() {
    val chart = DeviceCatalog.chartOf(EntityIds.Temperature)
    assertNull(chart.scale.atLeast)
    assertEquals(10.0, chart.scale.minSpan!!, 0.0)
    assertEquals(emptyList<LimitSpec>(), chart.limits)
    assertEquals(LongPeriods.Envelope, chart.longPeriods)
  }

  @Test
  fun `a sensor nobody knows is charted as before, a line on the scale of what it read`() {
    assertEquals(ChartProfile(), DeviceCatalog.chartOf(EntityId("wind")))
    assertEquals(SensorKind.Generic, DeviceCatalog.kindOf(EntityId("wind")))
  }

  @Test
  fun `the kind of a sensor is the one of the catalog`() {
    assertEquals(SensorKind.Gas, DeviceCatalog.kindOf(EntityIds.GasLevel))
    assertEquals(SensorKind.Temperature, DeviceCatalog.kindOf(EntityIds.Temperature))
    assertEquals(SensorKind.Humidity, DeviceCatalog.kindOf(EntityIds.Humidity))
  }

  @Test
  fun `the entities the limits are read from are listed, so the screen knows which values to follow`() {
    assertEquals(setOf(EntityIds.DehumidifierOnAt, EntityIds.DehumidifierOffAt), DeviceCatalog.limitEntities)
  }

  // ---- what the board says, and what the catalog says when the board says nothing -------------------

  @Test
  fun `what the board says of a sensor is what it is charted by, though the catalog does not know the sensor`() {
    val profile = DeviceCatalog.profileOf(EntityId("garage-temperature"), SensorKind.Temperature, emptyList())
    assertEquals(ChartScale(minSpan = 10.0), profile.scale)
  }

  @Test
  fun `the class the board gives wins over the catalog of the id`() {
    val profile = DeviceCatalog.profileOf(EntityIds.GasLevel, SensorKind.Humidity, emptyList())
    assertEquals(ChartScale(atLeast = 40.0..100.0), profile.scale)
    assertFalse(profile.fillBelow)
    assertEquals("the limits of the gas are not the ones of a humidity", emptyList<LimitSpec>(), profile.limits)
  }

  @Test
  fun `the limits the board gives win over the ones of the catalog`() {
    val declared = listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 200.0))
    assertEquals(declared, DeviceCatalog.profileOf(EntityIds.GasLevel, SensorKind.Gas, declared).limits)
  }

  @Test
  fun `a board that says nothing is drawn by the catalog of the id, as before`() {
    assertEquals(DeviceCatalog.chartOf(EntityIds.GasLevel), DeviceCatalog.profileOf(EntityIds.GasLevel, null, emptyList()))
    assertEquals(DeviceCatalog.chartOf(EntityIds.Humidity), DeviceCatalog.profileOf(EntityIds.Humidity, null, emptyList()))
  }

  @Test
  fun `a board that gives the class and no limits has the limits of the catalog for a sensor the catalog knows`() {
    assertEquals(listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 160.0)), DeviceCatalog.profileOf(EntityIds.GasLevel, SensorKind.Gas, emptyList()).limits)
  }

  @Test
  fun `a sensor nobody knows that says nothing is a plain line`() {
    assertEquals(ChartProfile(), DeviceCatalog.profileOf(EntityId("wind"), null, emptyList()))
  }

  @Test
  fun `a sensor nobody knows that says it is gas is drawn as the gas, with the limits it gave`() {
    val declared = listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 90.0))
    val profile = DeviceCatalog.profileOf(EntityId("co-level"), SensorKind.Gas, declared)
    assertEquals(0.0..400.0, profile.scale.atLeast)
    assertTrue(profile.fillBelow)
    assertEquals(LongPeriods.DailyPeaks, profile.longPeriods)
    assertEquals(declared, profile.limits)
  }

  @Test
  fun `a generic class is a plain line`() {
    assertEquals(ChartProfile(), DeviceCatalog.profileOf(EntityId("wind"), SensorKind.Generic, emptyList()))
  }

  @Test
  fun `the kind of a sensor is the one the board gave, or the one of the catalog`() {
    assertEquals(SensorKind.Temperature, DeviceCatalog.kindOf(EntityId("garage-temperature"), SensorKind.Temperature))
    assertEquals(SensorKind.Gas, DeviceCatalog.kindOf(EntityIds.GasLevel, null))
    assertEquals(SensorKind.Generic, DeviceCatalog.kindOf(EntityId("wind"), null))
  }
}
