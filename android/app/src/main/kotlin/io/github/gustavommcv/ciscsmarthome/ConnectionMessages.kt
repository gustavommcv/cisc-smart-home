package io.github.gustavommcv.ciscsmarthome

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState

/** What to tell the user about the connection, in the screen and in the notice of the service. */
@StringRes
fun ConnectionState.messageRes(): Int =
  when (this) {
    ConnectionState.Connecting -> R.string.connection_connecting
    ConnectionState.Connected -> R.string.connection_connected
    is ConnectionState.Disconnected ->
      when (problem) {
        ConnectionProblem.Unreachable -> R.string.connection_unreachable
        ConnectionProblem.TooManyClients -> R.string.connection_too_many_clients
        ConnectionProblem.Incompatible -> R.string.connection_incompatible
        ConnectionProblem.NetworkNotSet -> R.string.connection_network_not_set
        ConnectionProblem.NetworkNotJoined -> R.string.connection_network_not_joined
      }
  }
