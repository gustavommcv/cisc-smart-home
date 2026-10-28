package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The names the user gives: what counts as a name, and which name is shown, theirs, ours in Portuguese or the board's. */
class NamesTest {
  private val kitchen = AreaId("kitchen")
  private val garage = AreaId("garage")
  private val gasMonitor = DeviceId("gas-monitor")

  @Test
  fun `a name is what was typed, without the space around it`() {
    assertEquals("Cozinha da vó", Names.clean("  Cozinha da vó  "))
  }

  @Test
  fun `nothing, or only space, is no name`() {
    assertNull(Names.clean(""))
    assertNull(Names.clean("    "))
  }

  @Test
  fun `a name is no longer than the limit`() {
    assertEquals("a".repeat(Names.MAX_LENGTH), Names.clean("a".repeat(Names.MAX_LENGTH + 20)))
  }

  @Test
  fun `the limit is thirty letters, enough for a name and short enough for a card`() {
    assertEquals(30, Names.MAX_LENGTH)
  }

  @Test
  fun `the space the cut leaves at the end goes too`() {
    assertEquals("a".repeat(Names.MAX_LENGTH - 1), Names.clean("a".repeat(Names.MAX_LENGTH - 1) + " bbbb"))
  }

  @Test
  fun `the space at the start does not use up the limit`() {
    assertEquals("a".repeat(Names.MAX_LENGTH), Names.clean("   " + "a".repeat(Names.MAX_LENGTH + 10)))
  }

  @Test
  fun `the space inside a name stays as it was typed`() {
    assertEquals("Quarto  do   Pedro", Names.clean("Quarto  do   Pedro"))
  }

  @Test
  fun `a room shows the name the user gave it, before ours and before the board's`() {
    assertEquals("Cozinha da vó", Names(areas = mapOf(kitchen to "Cozinha da vó")).nameOfArea(kitchen, portuguese = "Cozinha", board = "Kitchen"))
  }

  @Test
  fun `a room the user did not name shows ours in Portuguese`() {
    assertEquals("Cozinha", Names().nameOfArea(kitchen, portuguese = "Cozinha", board = "Kitchen"))
  }

  @Test
  fun `a room the user did not name, and we have no words for, shows the name the board gave it`() {
    assertEquals("Garage", Names().nameOfArea(garage, portuguese = null, board = "Garage"))
  }

  @Test
  fun `the name the user gave to another room is not used`() {
    assertEquals("Garage", Names(areas = mapOf(kitchen to "Cozinha da vó")).nameOfArea(garage, portuguese = null, board = "Garage"))
  }

  @Test
  fun `a device shows the name the user gave it, before ours and before the board's`() {
    assertEquals("Detector da pia", Names(devices = mapOf(gasMonitor to "Detector da pia")).nameOfDevice(gasMonitor, portuguese = "Monitor de gás", board = "Gas monitor"))
  }

  @Test
  fun `a device the user did not name shows ours, and then the board's`() {
    assertEquals("Monitor de gás", Names().nameOfDevice(gasMonitor, portuguese = "Monitor de gás", board = "Gas monitor"))
    assertEquals("Doorbell", Names().nameOfDevice(DeviceId("doorbell"), portuguese = null, board = "Doorbell"))
  }

  @Test
  fun `a room and a device with the same id keep each its own name`() {
    val names = Names(areas = mapOf(AreaId("x") to "A sala"), devices = mapOf(DeviceId("x") to "O aparelho"))
    assertEquals("A sala", names.nameOfArea(AreaId("x"), portuguese = null, board = "Room"))
    assertEquals("O aparelho", names.nameOfDevice(DeviceId("x"), portuguese = null, board = "Device"))
  }
}
