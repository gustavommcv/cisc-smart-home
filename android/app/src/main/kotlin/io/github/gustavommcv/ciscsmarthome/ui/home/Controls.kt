package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds

/**
 * Whether the control of this entity works now: the board must take commands for it, must be heard (a command
 * sent into the void would only say it failed), and the last command sent to it must have been answered, so a
 * double tap is one command and not two.
 */
internal fun Entity.controlEnabled(connection: ConnectionState, pending: Set<EntityId>): Boolean =
  commandable && connection == ConnectionState.Connected && id !in pending

/**
 * Whether touching this switch asks first. Only silencing the buzzer does: the board still closes the valve and lights the LED, and the phone still warns, but whoever
 * is near the board without the phone would hear nothing in a leak. Enabling it again, or any other switch, does not ask.
 */
internal fun Entity.Switch.asksBeforeToggling(): Boolean = id == EntityIds.BuzzerEnabled && on == true

/** What touching a switch asks for: the opposite of what it shows. A switch with no reading is asked to turn on. */
internal fun Entity.Switch.toggleCommand(): CommandValue.Flag = CommandValue.Flag(on != true)

/** What a command that was not done tells the user. */
sealed interface CommandNotice {
  /** The board understood and said no. */
  data object Refused : CommandNotice

  /** The command could not be sent, or its answer could not be used. */
  data class Failed(val problem: ConnectionProblem) : CommandNotice
}

@StringRes
internal fun CommandNotice.messageRes(): Int =
  when (this) {
    CommandNotice.Refused -> R.string.command_refused
    is CommandNotice.Failed ->
      when (problem) {
        ConnectionProblem.Unreachable, ConnectionProblem.TooManyClients, ConnectionProblem.NetworkNotSet, ConnectionProblem.NetworkNotJoined -> R.string.command_unreachable
        ConnectionProblem.Incompatible -> R.string.command_incompatible
      }
  }
