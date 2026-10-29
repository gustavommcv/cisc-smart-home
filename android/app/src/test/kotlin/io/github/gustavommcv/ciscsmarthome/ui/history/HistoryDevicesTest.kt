package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.ChartProfile
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.SensorKind
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which devices the history lists, and which charts each one has: a chart for each sensor that reads in a unit, and one that compares temperature and humidity. */
class HistoryDevicesTest {
  private fun sensor(id: String, unit: String?, value: SensorValue = SensorValue.Number(1.0)) = Entity.Sensor(EntityId(id), "Name of $id", value, unit)

  private fun device(id: String, vararg entities: Entity) = Device(DeviceId(id), "Device $id", entities.toList())

  private fun house(vararg areas: Pair<String, List<Device>>) = Home(BootId("aaaa1111"), 1_000, areas.map { (id, devices) -> Area(AreaId(id), "Area $id", devices) })

  private val gasMonitor = device("gas-monitor", sensor("gas-level", "adc"), sensor("gas-state", null, SensorValue.Text("normal")), sensor("gas-alerts", null))
  private val closetMonitor = device("closet-monitor", sensor("temperature", "°C"), sensor("humidity", "%"))

  private fun chartIds(device: HistoryDevice) = device.charts.map { it.id }

  // ---- which devices ------------------------------------------------------------------

  @Test
  fun `each device with something to chart is listed, in the order of the house`() {
    val devices = historyDevices(house("kitchen" to listOf(gasMonitor), "bedroom" to listOf(closetMonitor)))
    assertEquals(listOf("gas-monitor", "closet-monitor"), devices.map { it.id.value })
  }

  @Test
  fun `a device says which room it is in`() {
    val device = historyDevices(house("kitchen" to listOf(gasMonitor))).single()
    assertEquals(AreaId("kitchen"), device.areaId)
    assertEquals("Area kitchen", device.areaName)
    assertEquals("Device gas-monitor", device.name)
  }

  @Test
  fun `each device says the room it is in, and not the first one of the house`() {
    val devices = historyDevices(house("kitchen" to listOf(gasMonitor), "bedroom" to listOf(closetMonitor)))
    assertEquals(listOf(AreaId("kitchen"), AreaId("bedroom")), devices.map { it.areaId })
    assertEquals(listOf("Area kitchen", "Area bedroom"), devices.map { it.areaName })
  }

  @Test
  fun `a device with no sensor that reads in a unit is not listed`() {
    val switches = device("switches", Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true), sensor("gas-alerts", null))
    assertEquals(emptyList<String>(), historyDevices(house("room" to listOf(switches))).map { it.id.value })
  }

  @Test
  fun `two devices of one room are two entries`() {
    val devices = historyDevices(house("room" to listOf(gasMonitor, closetMonitor)))
    assertEquals(listOf("gas-monitor", "closet-monitor"), devices.map { it.id.value })
    assertEquals(setOf(AreaId("room")), devices.map { it.areaId }.toSet())
  }

  // ---- which charts ---------------------------------------------------------------------

  @Test
  fun `the gas monitor has one chart, the level of gas, and a state or a count is not a chart`() {
    val device = historyDevices(house("kitchen" to listOf(gasMonitor))).single()
    assertEquals(listOf("single:gas-level"), chartIds(device))
  }

  @Test
  fun `a device that reads temperature and humidity has a chart that compares them, and one for each`() {
    val device = historyDevices(house("bedroom" to listOf(closetMonitor))).single()
    assertEquals(listOf("compare:temperature+humidity", "single:temperature", "single:humidity"), chartIds(device))
  }

  @Test
  fun `the comparison puts temperature above humidity, whatever the order the board lists them in`() {
    val backwards = device("closet-monitor", sensor("humidity", "%"), sensor("temperature", "°C"))
    val compare = historyDevices(house("bedroom" to listOf(backwards))).single().charts.first() as ChartSpec.Compare
    assertEquals(listOf("temperature", "humidity"), compare.sensors.map { it.id.value })
  }

  @Test
  fun `a device that reads only one of the two has no comparison`() {
    val onlyTemperature = device("probe", sensor("temperature", "°C"), sensor("wind", "km/h"))
    assertEquals(listOf("single:temperature", "single:wind"), chartIds(historyDevices(house("room" to listOf(onlyTemperature))).single()))
  }

  @Test
  fun `humidity and temperature of different devices are not compared`() {
    val devices = historyDevices(house("room" to listOf(device("a", sensor("temperature", "°C")), device("b", sensor("humidity", "%")))))
    assertEquals(listOf(listOf("single:temperature"), listOf("single:humidity")), devices.map { chartIds(it) })
  }

  @Test
  fun `a device the app does not know has a chart for each sensor, in the order of the device`() {
    val odd = device("weather", sensor("wind", "km/h"), sensor("rain", "mm"))
    assertEquals(listOf("single:wind", "single:rain"), chartIds(historyDevices(house("roof" to listOf(odd))).single()))
  }

  @Test
  fun `a sensor with no reading now still has its chart, for the history has what it read before`() {
    val unavailable = device("closet-monitor", sensor("temperature", "°C", SensorValue.Unavailable))
    assertEquals(listOf("single:temperature"), chartIds(historyDevices(house("bedroom" to listOf(unavailable))).single()))
  }

  @Test
  fun `a sensor keeps the name the board gave it, and the unit it reads in`() {
    val chart = historyDevices(house("room" to listOf(device("weather", sensor("wind", "km/h"))))).single().charts.single() as ChartSpec.Single
    assertEquals(ChartSensor(EntityId("wind"), "Name of wind", "km/h"), chart.sensor)
  }

  @Test
  fun `a chart says which sensors it draws`() {
    val charts = historyDevices(house("bedroom" to listOf(closetMonitor))).single().charts
    assertEquals(listOf("temperature", "humidity"), charts[0].sensors.map { it.id.value })
    assertEquals(listOf("temperature"), charts[1].sensors.map { it.id.value })
    assertEquals(listOf("humidity"), charts[2].sensors.map { it.id.value })
  }

  // ---- before the board is heard -----------------------------------------------------------

  @Test
  fun `before the board is heard, the two devices of the maquete are listed, so the history can be read away from it`() {
    val devices = historyDevices(null)
    assertEquals(listOf("gas-monitor", "closet-monitor"), devices.map { it.id.value })
    assertEquals(listOf(AreaId("kitchen"), AreaId("bedroom")), devices.map { it.areaId })
  }

  @Test
  fun `their charts are the ones the heard board gives`() {
    val devices = historyDevices(null)
    assertEquals(listOf("single:gas-level"), chartIds(devices[0]))
    assertEquals(listOf("compare:temperature+humidity", "single:temperature", "single:humidity"), chartIds(devices[1]))
    assertEquals(listOf("adc"), devices[0].charts.flatMap { it.sensors }.map { it.unit })
    assertEquals(listOf("°C", "%", "°C", "%"), devices[1].charts.flatMap { it.sensors }.map { it.unit })
  }

  // ---- the key of a chart -----------------------------------------------------------------

  @Test
  fun `a chart has a key that tells it from the others of the device`() {
    val keys = historyDevices(house("bedroom" to listOf(closetMonitor))).single().charts.map { it.id }
    assertEquals(keys.size, keys.toSet().size)
    assertNull(keys.firstOrNull { it.isBlank() })
  }

  // ---- what the board says of the sensors -----------------------------------------------------------

  private fun declared(id: String, unit: String, kind: SensorKind?, limits: List<LimitSpec> = emptyList()) = Entity.Sensor(EntityId(id), "Name of $id", SensorValue.Number(1.0), unit, kind, limits)

  @Test
  fun `a device nobody knows with a temperature and a humidity that say so has the chart that compares them`() {
    val garage = device("garage-monitor", declared("garage-temp", "°C", SensorKind.Temperature), declared("garage-humidity", "%", SensorKind.Humidity))
    val charts = historyDevices(house("garage" to listOf(garage))).single().charts
    assertEquals(listOf("compare:garage-temp+garage-humidity", "single:garage-temp", "single:garage-humidity"), charts.map { it.id })
  }

  @Test
  fun `a device nobody knows with sensors that say nothing is a line for each`() {
    val garage = device("garage-monitor", declared("garage-temp", "°C", null), declared("garage-humidity", "%", null))
    assertEquals(listOf("single:garage-temp", "single:garage-humidity"), historyDevices(house("garage" to listOf(garage))).single().charts.map { it.id })
  }

  @Test
  fun `a sensor carries the profile the board gave it`() {
    val limits = listOf<LimitSpec>(LimitSpec.Fixed(LimitKind.Alarm, 90.0))
    val garage = device("garage-monitor", declared("co-level", "ppm", SensorKind.Gas, limits))
    val sensor = (historyDevices(house("garage" to listOf(garage))).single().charts.single() as ChartSpec.Single).sensor
    assertEquals(SensorKind.Gas, sensor.kind)
    assertEquals(limits, sensor.profile.limits)
    assertEquals(0.0..400.0, sensor.profile.scale.atLeast)
  }

  @Test
  fun `a sensor of the maquette that the board does not describe has the profile of the catalog`() {
    val sensor = (historyDevices(house("kitchen" to listOf(gasMonitor))).single().charts.single() as ChartSpec.Single).sensor
    assertEquals(DeviceCatalog.chartOf(EntityId("gas-level")), sensor.profile)
    assertEquals(SensorKind.Gas, sensor.kind)
  }

  @Test
  fun `a sensor with no class and no catalog is a plain line`() {
    val sensor = (historyDevices(house("room" to listOf(device("weather", declared("wind", "km/h", null))))).single().charts.single() as ChartSpec.Single).sensor
    assertEquals(SensorKind.Generic, sensor.kind)
    assertEquals(ChartProfile(), sensor.profile)
  }
}
