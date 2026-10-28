package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsTest {
  private val power = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)
  private val mode = Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic", "manual"), commandable = true)

  // ---- when a control works -------------------------------------------------------------

  @Test
  fun `a control works when the board is heard and the entity takes commands`() {
    assertTrue(power.controlEnabled(ConnectionState.Connected, emptySet()))
    assertTrue(mode.controlEnabled(ConnectionState.Connected, emptySet()))
  }

  @Test
  fun `an entity that takes no commands has no working control`() {
    val valve = Entity.BinarySensor(EntityId("valve-open"), "Valve", true)
    val readOnlySwitch = power.copy(commandable = false)
    val gas = Entity.Sensor(EntityId("gas-level"), "Gas", SensorValue.Number(1.0))
    listOf(valve, readOnlySwitch, gas).forEach { assertFalse("${it.id}", it.controlEnabled(ConnectionState.Connected, emptySet())) }
  }

  @Test
  fun `no control works while the board is not heard`() {
    assertFalse(power.controlEnabled(ConnectionState.Connecting, emptySet()))
    ConnectionProblem.entries.forEach { assertFalse(power.controlEnabled(ConnectionState.Disconnected(it), emptySet())) }
  }

  @Test
  fun `a control waits for the answer to the command it sent`() {
    assertFalse(power.controlEnabled(ConnectionState.Connected, setOf(power.id)))
  }

  @Test
  fun `the command of one control does not hold the others`() {
    assertTrue(mode.controlEnabled(ConnectionState.Connected, setOf(power.id)))
  }

  // ---- what a switch asks for -------------------------------------------------------------

  @Test
  fun `a switch that is off asks to be turned on, and one that is on, off`() {
    assertEquals(CommandValue.Flag(true), power.copy(on = false).toggleCommand())
    assertEquals(CommandValue.Flag(false), power.copy(on = true).toggleCommand())
  }

  @Test
  fun `a switch with no reading asks to be turned on`() {
    assertEquals(CommandValue.Flag(true), power.copy(on = null).toggleCommand())
  }

  // ---- what the user is told ----------------------------------------------------------------

  @Test
  fun `each thing that can go wrong with a command has its own words`() {
    assertEquals(R.string.command_refused, CommandNotice.Refused.messageRes())
    assertEquals(R.string.command_unreachable, CommandNotice.Failed(ConnectionProblem.Unreachable).messageRes())
    assertEquals(R.string.command_unreachable, CommandNotice.Failed(ConnectionProblem.TooManyClients).messageRes())
    assertEquals(R.string.command_unreachable, CommandNotice.Failed(ConnectionProblem.NetworkNotSet).messageRes())
    assertEquals(R.string.command_unreachable, CommandNotice.Failed(ConnectionProblem.NetworkNotJoined).messageRes())
    assertEquals(R.string.command_incompatible, CommandNotice.Failed(ConnectionProblem.Incompatible).messageRes())
  }

  @Test
  fun `the words are all different`() {
    val messages = listOf(CommandNotice.Refused, CommandNotice.Failed(ConnectionProblem.Unreachable), CommandNotice.Failed(ConnectionProblem.Incompatible)).map { it.messageRes() }
    assertEquals(messages.size, messages.toSet().size)
  }
}
