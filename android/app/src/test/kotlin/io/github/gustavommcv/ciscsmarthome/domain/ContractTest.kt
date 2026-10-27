package io.github.gustavommcv.ciscsmarthome.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ids and words the app knows are the ones of docs/api.md. The constants are written once in the code, so
 * this keeps them from drifting from the document, or from being mistyped, without anyone having to notice.
 */
class ContractTest {
  // The unit tests of Gradle run in android/app.
  private val contract = File("../../docs/api.md").readText()

  private fun assertInContract(kind: String, words: List<String>) {
    words.forEach { assertTrue("$kind `$it` is not in docs/api.md", "`$it`" in contract || "\"$it\"" in contract) }
  }

  @Test
  fun `the entities the app knows are the entities of the document`() {
    assertInContract(
      "entity",
      listOf(
        EntityIds.GasLevel,
        EntityIds.GasState,
        EntityIds.GasAlerts,
        EntityIds.ValveOpen,
        EntityIds.Temperature,
        EntityIds.Humidity,
        EntityIds.DehumidifierPower,
        EntityIds.DehumidifierMode,
      ).map { it.value },
    )
  }

  @Test
  fun `the rooms the app knows are the rooms of the document`() {
    assertInContract("room", listOf(AreaIds.Kitchen, AreaIds.Bedroom).map { it.value })
  }

  @Test
  fun `the devices the app knows are the devices of the document`() {
    assertInContract("device", listOf(DeviceIds.GasSensor, DeviceIds.GasValve, DeviceIds.ClosetSensor, DeviceIds.Dehumidifier).map { it.value })
  }

  @Test
  fun `the states of the gas sensor are the ones of the document`() {
    assertInContract("gas state", listOf(GasStates.Starting, GasStates.Normal, GasStates.Emergency))
  }

  @Test
  fun `the modes of the dehumidifier are the ones of the document`() {
    assertInContract("mode", listOf(DehumidifierModes.Automatic, DehumidifierModes.Manual))
  }

  @Test
  fun `the kind of alert the app knows is the one of the document`() {
    assertInContract("alert", listOf(AlertTypes.GasLeak))
  }

  @Test
  fun `nothing is written twice`() {
    val entities = listOf(EntityIds.GasLevel, EntityIds.GasState, EntityIds.GasAlerts, EntityIds.ValveOpen, EntityIds.Temperature, EntityIds.Humidity, EntityIds.DehumidifierPower, EntityIds.DehumidifierMode)
    assertEquals(entities.size, entities.toSet().size)
    val devices = listOf(DeviceIds.GasSensor, DeviceIds.GasValve, DeviceIds.ClosetSensor, DeviceIds.Dehumidifier)
    assertEquals(devices.size, devices.toSet().size)
    assertEquals(setOf("starting", "normal", "emergency"), setOf(GasStates.Starting, GasStates.Normal, GasStates.Emergency))
  }
}
