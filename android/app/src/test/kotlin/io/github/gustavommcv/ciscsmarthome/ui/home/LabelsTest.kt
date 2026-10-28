package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelsTest {
  private val known =
    listOf("gas-level", "gas-state", "gas-alerts", "valve-open", "temperature", "humidity", "dehumidifier-power", "dehumidifier-mode", "dehumidifier-on-at", "dehumidifier-off-at", "buzzer-enabled")

  @Test
  fun `every entity of the maquete has a label in Portuguese`() {
    known.forEach { assertNotEquals("no label for $it", null, EntityId(it).labelRes()) }
  }

  @Test
  fun `the labels are all different`() {
    val labels = known.map { EntityId(it).labelRes() }
    assertEquals(labels.size, labels.toSet().size)
  }

  @Test
  fun `an entity the app does not know has no label of its own`() {
    assertNull(EntityId("smoke").labelRes())
  }

  private val devices = listOf("gas-monitor", "closet-monitor")

  @Test
  fun `every device of the maquete has a name in Portuguese`() {
    devices.forEach { assertNotEquals("no name for $it", null, DeviceId(it).nameRes()) }
  }

  @Test
  fun `the names of the devices are all different`() {
    val names = devices.map { DeviceId(it).nameRes() }
    assertEquals(names.size, names.toSet().size)
  }

  @Test
  fun `a device the app does not know has no name of its own`() {
    assertNull(DeviceId("camera").nameRes())
  }

  @Test
  fun `the states of the gas sensor have their words`() {
    assertEquals(R.string.gas_starting, gasStateRes("starting"))
    assertEquals(R.string.gas_normal, gasStateRes("normal"))
    assertEquals(R.string.gas_emergency, gasStateRes("emergency"))
  }

  @Test
  fun `a state the app does not know is shown as the board says it`() {
    assertNull(gasStateRes("fainted"))
  }

  @Test
  fun `the modes of the dehumidifier have their words`() {
    assertEquals(R.string.option_automatic, optionRes("automatic"))
    assertEquals(R.string.option_manual, optionRes("manual"))
    assertNull(optionRes("turbo"))
  }
}
